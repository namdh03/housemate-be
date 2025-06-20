# Build
FROM maven:3.8.3-openjdk-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies first (cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy rest of the source
COPY src ./src

# Build the application
RUN mvn clean install -DskipTests

#  Runtime
FROM eclipse-temurin:17.0.8.1_1-jre-ubi9-minimal

# Set timezone to Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && echo "Asia/Ho_Chi_Minh" > /etc/timezone

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Expose app port
EXPOSE 8888

# Java options via ENV
ENV JAVA_OPTS="-Xmx2048m -Xms256m"

# Entry point
ENTRYPOINT java -jar $JAVA_OPTIONS /app/app.jar
