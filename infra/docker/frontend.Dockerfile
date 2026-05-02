FROM node:22-alpine
WORKDIR /app

# Placeholder image for initial scaffold.
COPY frontend /app/frontend

EXPOSE 3000
CMD ["sh", "-c", "echo 'Frontend scaffold ready'; sleep infinity"]
