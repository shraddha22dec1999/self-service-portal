# Build the static app, then serve it with nginx. nginx also forwards /api to the gateway,
# so the browser talks to one origin and the gateway needs no CORS setup.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Vite bakes VITE_* values in at build time.
ARG VITE_OIDC_AUTHORITY=
ARG VITE_OIDC_CLIENT_ID=
ARG VITE_OIDC_SCOPE="openid profile email"
ARG VITE_ADMIN_USERS=super_admin_1,super_admin_2
RUN npm run build

FROM nginx:1.27-alpine
ENV GATEWAY_URL=http://gateway:8080
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
