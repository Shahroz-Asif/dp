import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client references Node's `global`; polyfill it for the browser
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      // Proxy all /api requests to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      // Proxy WebSocket (SockJS) endpoint
      '/ws': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
