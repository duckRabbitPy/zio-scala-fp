# Use the official Java base image
FROM azul/zulu-openjdk-alpine:11

# Set the working directory inside the container
WORKDIR /app

# Copy the .jar file to the container
COPY target/scala-3.2.2/learning-scala-fp-assembly-0.1.0-SNAPSHOT.jar .

# Copy the resources to the container
COPY ./src/main/resources/ /app/src/main/resources/

# Expose the port your application listens on
EXPOSE 8080

# Set an environment variable (optional)
ENV RUNNING_IN_DOCKER=true

# Define the command to run your application
CMD ["java", "-jar", "-Dport=8080", "-DisProd=true", "learning-scala-fp-assembly-0.1.0-SNAPSHOT.jar"]
