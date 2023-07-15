# Use the official Java base image
FROM azul/zulu-openjdk-alpine:11

# Set the working directory inside the container
WORKDIR /app

# Copy the .jar file to the container
COPY target/scala-3.2.2/learning-scala-fp-assembly-0.1.0-SNAPSHOT.jar .

# Expose the port your application listens on (if applicable)
EXPOSE 8080

# Define the command to run your application
CMD ["java", "-jar", "-Dport=8080", "-DisProd=true","learning-scala-fp-assembly-0.1.0-SNAPSHOT.jar"]


