import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In development the browser calls /api on the Vite server, which forwards to the gateway.
// Same origin, so no CORS setup is needed. Point GATEWAY_URL elsewhere to use a deployed backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.GATEWAY_URL ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
