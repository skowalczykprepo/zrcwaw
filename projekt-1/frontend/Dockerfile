# Etap 1: budowanie statycznych plikow (Vite)
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etap 2: lekki serwer Node (Express) serwujacy dist/ + konfiguracje srodowiskowa
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
COPY server.js .
ENV PORT=3000
EXPOSE 3000
CMD ["node", "server.js"]
