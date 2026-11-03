import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // 저장소 루트의 .env를 읽습니다. Vite는 VITE_로 시작하는 값만 화면 코드에 넘깁니다.
  envDir: '..',
})
