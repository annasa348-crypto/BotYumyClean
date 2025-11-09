# Imagen base de Java 17 (la versión más usada para bots con JDA)
FROM openjdk:17-jdk-slim

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar los archivos del proyecto
COPY pom.xml .
COPY src ./src

# Instalar Maven y compilar el proyecto
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Copiar el .jar generado a la carpeta final (ajustá el nombre si cambia)
COPY target/DiscordBot-1.0-SNAPSHOT-jar-with-dependencies.jar /app/bot.jar

# Comando para ejecutar el bot
CMD ["java", "-jar", "bot.jar"]

