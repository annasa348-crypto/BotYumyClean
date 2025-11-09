package com.mycompany.discordbot2;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {
    public static void main(String[] args) {

        String TOKEN_SECRETO = System.getenv("DISCORD_TOKEN");

        if (TOKEN_SECRETO == null || TOKEN_SECRETO.isEmpty()) {
            System.err.println("❌ ¡Error! No se encontró el token de Discord. Configura DISCORD_TOKEN en Render.");
            return;
        }

        try {
            JDABuilder.createDefault(TOKEN_SECRETO)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new ListenerDeComandos())
                    .build()
                    .awaitReady();

            System.out.println("🤖 ¡Bot conectado y listo!");

            // Servidor HTTP para Render
            int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", exchange -> {
                String response = "Bot Yumy está funcionando en el puerto " + PORT;
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            server.start();
            System.out.println("🌐 Servidor web activo en el puerto: " + PORT);

        } catch (Exception e) {
            System.err.println("💥 Error al iniciar el bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

