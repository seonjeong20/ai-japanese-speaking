import { useState } from 'react'

export const SPEAKING_STATUS_TEXT = {
  idle: '마이크를 눌러 말해보세요',
  listening: '듣고 있어요...',
  thinking: '답변을 준비하고 있어요...',
  speaking: 'AI가 말하고 있어요...',
}

// 마이크 버튼을 누를 때마다 idle -> listening -> thinking -> speaking -> listening 순으로 순환합니다.
const NEXT_STATUS = {
  idle: 'listening',
  listening: 'thinking',
  thinking: 'speaking',
  speaking: 'listening',
}

export function useSpeakingStatus(initial = 'idle') {
  const [status, setStatus] = useState(initial)

  const cycleStatus = () => {
    setStatus((current) => NEXT_STATUS[current])
  }

  return { status, statusText: SPEAKING_STATUS_TEXT[status], cycleStatus, setStatus }
}
