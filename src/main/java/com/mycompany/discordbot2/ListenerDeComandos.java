package com.mycompany.discordbot2;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback; 
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class ListenerDeComandos extends ListenerAdapter {

    // Variables de Configuración
    private static final String API_KEY_SECRETA = System.getenv("GEMINI_API_KEY");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private final String prefix = "!bot"; // Prefijo del comando

    // -------------------------------------------------------------------------
    // Bloque de Instrucciones (PROMPT) - Base sin formatear
    // -------------------------------------------------------------------------
    private final String PROMPT_CHEF_BASE = 
        "Actúa como un chef italiano apasionado llamado Yumy. Debes responder con un toque de humor y un ligero acento italiano usando frases como: \"Mamma mia\", \"Bella\", \"Perfetto!\".\n\n"
        + "Tu respuesta debe usar este formato estricto: \n"
        + "**Recetta di Yumy: %s**\n" // Marcador 1: Para el nombre de la receta
        + "*Ingredienti (con cantidades aproximadas):*\n"
        + " [Lista de ingredientes]\n"
        + "*Passos (narrados con acento italiano):*\n"
        + " [Passo 1, Passo 2, etc.]\n"
        + "Plato: %s. ¡El plato es una cosa de dos bocas en el prompt!"; // Marcador 2: Para repetir el nombre del plato


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        
        if (event.getAuthor().isBot()) {
            return;
        }

        String mensaje_original = event.getMessage().getContentRaw();

        // 1. Detección del comando "!bot"
        if (mensaje_original.startsWith(prefix)) {
            
            // Extracción del mensaje del usuario
            String mensaje_solicitado = mensaje_original.substring(prefix.length()).trim();

            if (mensaje_solicitado.isEmpty()) {
                event.getChannel().sendMessage("Mamma mia! Per favore, usa el formato: `" + prefix + " [tu pregunta]`").queue();
                return;
            }

            // Enviar mensaje de espera y capturar su ID para poder editarlo después
            event.getChannel().sendMessage("¡Bella! Un momento, sto consultando la mia testa... 🤖").queue(
                // Callback para obtener la referencia del mensaje enviado
                (mensajeEspera) -> {
                    // Llamada al método asíncrono que conecta con Gemini
                    enviarMensajeAIAsincrono(mensaje_solicitado, mensajeEspera);
                }
            );
        }
    }
    
    /**
     * FUNCIÓN FINAL Y ASÍNCRONA para conectarse a la API de Gemini.
     */
    private void enviarMensajeAIAsincrono(String preguntaUsuario, net.dv8tion.jda.api.entities.Message mensajeDiscord) {

        // 1. VERIFICACIÓN DE CLAVE API (Evita fallos si Render no la tiene)
        if (API_KEY_SECRETA == null || API_KEY_SECRETA.isEmpty()) {
            mensajeDiscord.editMessage("❌ Mamma mia! Non posso trovare la chiave segreta (GEMINI_API_KEY). ¡Verifica la configuración di Render!").queue();
            return;
        }

        // 2. Construir el prompt completo
        String platoSolicitado = preguntaUsuario; 
        String fullPrompt = String.format(PROMPT_CHEF_BASE, platoSolicitado, platoSolicitado) 
                            + "\n\nLa pregunta detallada del cliente es: " + preguntaUsuario;

        // 3. Construir el cuerpo JSON
        String jsonPayload = String.format(
            """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ]
            }
            """,
            // Escapar caracteres especiales para que el JSON sea válido
            fullPrompt.replace("\"", "\\\"").replace("\n", "\\n") 
        );

        RequestBody requestBody = RequestBody.create(jsonPayload, JSON_MEDIA_TYPE);

        // 4. Construir la solicitud HTTP
        Request request = new Request.Builder()
            .url(GEMINI_URL + API_KEY_SECRETA)
            .post(requestBody)
            .build();
        
        // 5. Ejecutar la solicitud ASÍNCRONA (para evitar bloqueos de hilo)
        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull okhttp3.Call call, @NotNull IOException e) {
                // Falla de red, timeout, etc.
                mensajeDiscord.editMessage("❌ Lo siento, hubo un fallo técnico en la conexión con la IA: " + e.getMessage()).queue();
            }

            @Override
            public void onResponse(@NotNull okhttp3.Call call, @NotNull Response response) throws IOException {
                String textoRespuesta;
                
                try {
                    // Manejo de error HTTP (clave inválida, rate limit, etc.)
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No body";
                        textoRespuesta = "❌ Fallo API HTTP (" + response.code() + "). Detalle: " + errorBody;
                        mensajeDiscord.editMessage(textoRespuesta).queue();
                        return;
                    }

                    // Leer y analizar la respuesta JSON
                    String responseString = response.body().string();
                    
                    // Análisis del JSON (Busca la clave "text")
                    String textoClave = "\"text\": \"";
                    int startIndex = responseString.indexOf(textoClave);

                    if (startIndex == -1) {
                        textoRespuesta = "❌ Mamma mia! Non posso analizzare la risposta. Forse è stata bloqueada por la IA.";
                    } else {
                        startIndex += textoClave.length();
                        int endIndex = responseString.indexOf("\"", startIndex);
                        if (endIndex == -1) {
                            endIndex = responseString.length();
                        }
                        textoRespuesta = responseString.substring(startIndex, endIndex);
                        
                        // Limpieza: Reemplazar caracteres de escape de JSON
                        textoRespuesta = textoRespuesta
                               .replace("\\n", "\n")
                               .replace("\\\"", "\"")
                               .replace("\\\\", "\\");
                    }
                    
                    // Enviar la respuesta a Discord
                    mensajeDiscord.editMessage(textoRespuesta).queue();

                } catch (Exception e) {
                    System.err.println("Error fatal en Gemini API: " + e.getMessage());
                    e.printStackTrace();
                    mensajeDiscord.editMessage("❌ ¡Perfetto! Fallo interno al procesar el JSON: " + e.getMessage()).queue();
                } finally {
                    // Cerrar el cuerpo de la respuesta
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }
}