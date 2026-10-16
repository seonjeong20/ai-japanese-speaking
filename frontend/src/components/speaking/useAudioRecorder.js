import { useCallback, useEffect, useRef } from 'react'

const RECORDER_MIME_CANDIDATES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']

function pickRecorderMimeType() {
  if (typeof MediaRecorder === 'undefined') return ''
  return RECORDER_MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) || ''
}

// ConversationSpeakingPage/InterviewSpeakingPage에 중복돼 있던 MediaRecorder 생성/시작/종료,
// chunk 수집, MediaStream cleanup, 오류 처리를 공통화한 Hook입니다.
// 녹음이 끝나면 완성된 Blob만 onComplete로 전달하고, 이후 도메인 로직(API 호출 등)은
// 호출 측(SpeakingPage)이 그대로 담당합니다.
// - stop(): 정상 녹음 완료. Blob을 만들어 onComplete로 전달합니다.
// - cancel(): 녹음 취소. 자원만 정리하고 onComplete/onError를 호출하지 않습니다. unmount 시에도 호출됩니다.
export function useAudioRecorder({ onComplete, onError }) {
  const mediaRecorderRef = useRef(null)
  const audioChunksRef = useRef([])
  const streamRef = useRef(null)
  // onerror 이후에도 스펙상 onstop이 뒤따라 호출되므로, 그 때 깨진 오디오를 제출하지 않도록 막는 플래그입니다.
  const recordingFailedRef = useRef(false)
  // 녹음 시도(attempt)마다 증가하는 id입니다. start()는 발급받은 id를 closure로 들고 있다가,
  // 비동기 대기(getUserMedia)나 MediaRecorder event 시점에 현재 id와 다르면 취소된 시도로 보고 무시합니다.
  const attemptIdRef = useRef(0)

  const onCompleteRef = useRef(onComplete)
  const onErrorRef = useRef(onError)

  useEffect(() => {
    onCompleteRef.current = onComplete
    onErrorRef.current = onError
  }, [onComplete, onError])

  // 각 recorder는 자신이 만들어질 때의 stream만 정리합니다. 이미 새 녹음이 시작된 뒤 도착한
  // 과거 recorder의 event가 새 녹음의 stream(streamRef.current)을 끊지 않도록 하기 위함입니다.
  const releaseStream = (stream) => {
    stream.getTracks().forEach((track) => track.stop())
    if (streamRef.current === stream) {
      streamRef.current = null
    }
  }

  const cancel = useCallback(() => {
    // recorder.stop()보다 먼저 attempt를 무효화해야, 뒤이어 발생하는 onstop이 취소로 판단됩니다.
    attemptIdRef.current += 1

    const recorder = mediaRecorderRef.current
    mediaRecorderRef.current = null
    if (recorder && recorder.state !== 'inactive') {
      recorder.stop()
    }

    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    audioChunksRef.current = []
  }, [])

  // unmount는 cancel과 같은 의미입니다. 영구적인 unmounted 플래그를 두지 않으므로
  // StrictMode의 effect cleanup/setup 재실행 이후에도 start()를 다시 호출할 수 있습니다.
  useEffect(() => {
    return () => {
      cancel()
    }
  }, [cancel])

  const start = async () => {
    attemptIdRef.current += 1
    const attemptId = attemptIdRef.current
    const isCurrentAttempt = () => attemptIdRef.current === attemptId

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

      // getUserMedia를 기다리는 동안 cancel/unmount된 경우: 받은 stream을 바로 정리하고 녹음을 시작하지 않습니다.
      if (!isCurrentAttempt()) {
        stream.getTracks().forEach((track) => track.stop())
        return false
      }
      streamRef.current = stream

      const mimeType = pickRecorderMimeType()
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
      audioChunksRef.current = []
      recordingFailedRef.current = false

      recorder.ondataavailable = (event) => {
        if (!isCurrentAttempt()) return
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data)
        }
      }

      // 권한 거부 이후의 오류입니다: 녹음 도중 장치 연결 해제 등으로 MediaRecorder 자체가
      // 실패하는 경우를 처리합니다. 스펙상 error 이후 stop도 뒤따라 호출되므로, 그 때 깨진
      // 오디오가 제출되지 않도록 recordingFailedRef로 막습니다.
      recorder.onerror = (event) => {
        releaseStream(stream)
        if (!isCurrentAttempt()) return
        console.error('MediaRecorder error', event.error ?? event)
        recordingFailedRef.current = true
        onErrorRef.current?.('녹음 중 문제가 발생했습니다. 다시 시도해주세요.')
      }

      recorder.onstop = () => {
        releaseStream(stream)
        if (!isCurrentAttempt()) return
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
      // cancel/unmount 이후 도착한 권한 오류 등은 사용자에게 알리지 않습니다.
      if (!isCurrentAttempt()) return false
      onErrorRef.current?.('마이크 권한이 필요합니다. 브라우저 설정에서 마이크 접근을 허용해주세요.')
      return false
    }
  }

  const stop = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop()
    }
  }

  return { start, stop, cancel }
}
