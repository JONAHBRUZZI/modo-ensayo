FROM eclipse-temurin:21-jre
WORKDIR /app

# Placeholder image for initial scaffold.
COPY backend /app/backend

EXPOSE 8080
CMD ["sh", "-c", "echo 'Backend scaffold ready'; sleep infinity"]
