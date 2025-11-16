package com.mycompany.discordbot2;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot2 {
    public static void main(String[] args) {

        String TOKEN_SECRETO = System.getenv("DISCORD_TOKEN"); //variable de entorno para render 

        if (TOKEN_SECRETO == null || TOKEN_SECRETO.isEmpty()) {
            System.err.println("❌ ¡Error! No se encontró el token de Discord. Configura DISCORD_TOKEN en Render.");
            return; //esta linea es para cuando el token sea incorrecto o no lo encuentra
        }

        try {
            JDABuilder.createDefault(TOKEN_SECRETO) //constructor principal, donde inicia la conexion con discord
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES) //esto le dice a discord que evento debe recibir 
                    .addEventListeners(new ListenerDeComandos()) //cada que reciba el mensaje lo envia a la clase de listener de comandos
                    .build()
                    .awaitReady(); //esta linea espera la conexion del bot 

            System.out.println("🤖 ¡Bot conectado y listo!");

            // Servidor HTTP para Render
            int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")); //lee el puerto de render 8080 para recibir las peticiones
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0); //configura el servidor web para solo peticiones 
            server.createContext("/", exchange -> { //esto cuando el bot visita la URL en render 
                String response = "Bot Yumy está funcionando en el puerto " + PORT;
                exchange.sendResponseHeaders(200, response.length()); //cuando la solicitud es exitosa codigo 200
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes()); //esto envia un mensaje a render de respuesta que el bot esta funcionando 
                }
            });
            server.start(); //inicia el servidor http para que render sepa que la app esta viva  
            System.out.println("🌐 Servidor web activo en el puerto: " + PORT);

        } catch (Exception e) {
            System.err.println("💥 Error al iniciar el bot: " + e.getMessage());
            e.printStackTrace(); //esta linea muestra una excepcion que el bot no se conecto 
        }
    }
}

