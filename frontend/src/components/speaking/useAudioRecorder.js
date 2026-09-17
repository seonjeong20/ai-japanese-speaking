import { useEffect, useRef } from 'react'

const RECORDER_MIME_CANDIDATES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']

function pickRecorderMimeType() {
  if (typeof MediaRecorder === 'undefined') return ''
  return RECORDER_MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) || ''
}

// ConversationSpeakingPage/InterviewSpeakingPage에 중복돼 있던 MediaRecorder 생성/시작/종료,
// chunk 수집, MediaStream cleanup, 오류 처리를 공통화한 Hook입니다.
// 녹음이 끝나면 완성된 Blob만 onComplete로 전달하고, 이후 도메인 로직(API 호출 등)은
// 호출 측(SpeakingPage)이 그대로 담당합니다.
export function useAudioRecorder({ onComplete, onError }) {
  const mediaRecorderRef = useRef(null)
  const audioChunksRef = useRef([])
  const streamRef = useRef(null)
  // onerror 이후에도 스펙상 onstop이 뒤따라 호출되므로, 그 때 깨진 오디오를 제출하지 않도록 막는 플래그입니다.
  const recordingFailedRef = useRef(false)

  const onCompleteRef = useRef(onComplete)
  const onErrorRef = useRef(onError)

  useEffect(() => {
    onCompleteRef.current = onComplete
    onErrorRef.current = onError
  }, [onComplete, onError])

  useEffect(() => {
    return () => {
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [])

  const start = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      const mimeType = pickRecorderMimeType()
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
      audioChunksRef.current = []
      recordingFailedRef.current = false

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data)
        }
      }

      // 권한 거부 이후의 오류입니다: 녹음 도중 장치 연결 해제 등으로 MediaRecorder 자체가
      // 실패하는 경우를 처리합니다. 스펙상 error 이후 stop도 뒤따라 호출되므로, 그 때 깨진
      // 오디오가 제출되지 않도록 recordingFailedRef로 막습니다.
      recorder.onerror = (event) => {
        console.error('MediaRecorder error', event.error ?? event)
        recordingFailedRef.current = true
        streamRef.current?.getTracks().forEach((track) => track.stop())
        streamRef.current = null
        onErrorRef.current?.('녹음 중 문제가 발생했습니다. 다시 시도해주세요.')
      }

      recorder.onstop = () => {
        streamRef.current?.getTracks().forEach((track) => track.stop())
        streamRef.current = null
        if (recordingFailedRef.current) {
          recordingFailedRef.current = false
          return
        }
        const blob = new Blob(audioChunksRef.current, { type: recorder.mimeType || 'audio/webm' })
        onCompleteRef.current?.(blob)
      }

      mediaRecorderRef.current = recorder
      recorder.start()
      return true
    } catch {
      onErrorRef.current?.('마이크 권한이 필요합니다. 브라우저 설정에서 마이크 접근을 허용해주세요.')
      return false
    }
  }

  const stop = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop()
    }
  }

  return { start, stop }
}
