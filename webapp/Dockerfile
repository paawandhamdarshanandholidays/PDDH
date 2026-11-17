FROM node:22-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci --ignore-scripts
COPY . .
RUN npm run build -- --configuration production

FROM node:22-alpine

ENV NODE_ENV=production
WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force

COPY --from=build --chown=node:node /app/dist/pawandhamdarshanandholidays/browser ./browser
COPY --from=build --chown=node:node /app/dist/pawandhamdarshanandholidays/server ./server
USER node
EXPOSE 4000
CMD ["node", "server/server.mjs"]