import { useEffect, useState } from 'react'

function formatElapsed(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// 세션 경과 시간을 1초마다 증가시키는 mock 타이머입니다 (실제 서버 동기화 없음).
export function useElapsedTimer() {
  const [seconds, setSeconds] = useState(0)

  useEffect(() => {
    const intervalId = setInterval(() => {
      setSeconds((prev) => prev + 1)
    }, 1000)
    return () => clearInterval(intervalId)
  }, [])

  return formatElapsed(seconds)
}
