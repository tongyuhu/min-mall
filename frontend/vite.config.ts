import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: Object.fromEntries(
      ['/api/v1', '/admin/v1'].map((path) => [
        path,
        {
          target: 'http://127.0.0.1:18080',
          changeOrigin: true,
        },
      ]),
    ),
  },
  test: { include: ['tests/**/*.test.ts'] },
});
