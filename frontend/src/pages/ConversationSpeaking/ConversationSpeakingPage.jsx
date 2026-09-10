import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import VoiceOrb from '../../components/speaking/VoiceOrb'
import SessionEndModal from '../../components/speaking/SessionEndModal'
import { useSpeakingStatus } from '../../components/speaking/useSpeakingStatus'
import { useElapsedTimer } from '../../components/speaking/useElapsedTimer'
import { BackArrowIcon, EndCallIcon, MicIcon } from '../../components/icons/DashboardIcons'
import { Difficulty, DifficultyLabel, SubtitleMode } from '../../data/enums'
import { completeConversation, submitAudioTurn } from '../../api/conversations'
import '../../components/setup/SetupForm.css'
import '../../components/speaking/SpeakingSession.css'
import './ConversationSpeakingPage.css'

// URL로 직접 접근하는 등 Setup에서 전달된 state가 없을 때 사용하는 기본값입니다.
// (sessionId는 기본값이 없습니다 — 실제 세션 없이는 대화를 진행할 수 없습니다.)
const DEFAULT_SETTINGS = {
  situation: '카페에서',
  partner: '친구',
  personality: '친절하고 활발한',
  description: '',
  difficulty: Difficulty.INTERMEDIATE,
  subtitleMode: SubtitleMode.JAPANESE,
}

const RECORDER_MIME_CANDIDATES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']

function pickRecorderMimeType() {
  if (typeof MediaRecorder === 'undefined') return ''
  return RECORDER_MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) || ''
}

function pickParticle(word, withBatchim, withoutBatchim) {
  if (!word) return withoutBatchim
  const lastChar = word.trim().slice(-1)
  const code = lastChar.charCodeAt(0) - 0xac00
  if (code < 0 || code > 11171) return withoutBatchim
  return code % 28 === 0 ? withoutBatchim : withBatchim
}

function buildSessionTitle(situation, partner) {
  if (!situation && !partner) return '일반 회화 연습'
  const particle = pickParticle(partner, '과', '와')
  return `${situation}${partner ? ` ${partner}${particle}` : ''} 대화하기`
}

function ConversationSpeakingPage() {
  const navigate = useNavigate()
  const location = useLocation()
  // Setup 화면에서 값을 비워둔 채 넘어오거나 URL로 직접 접근한 경우를 대비해
  // 필드별로 빈 값이면 기본값으로 보정합니다.
  const receivedSettings = location.state ?? {}
  const settings = Object.fromEntries(
    Object.entries(DEFAULT_SETTINGS).map(([key, fallback]) => [key, receivedSettings[key] || fallback]),
  )
  const sessionId = receivedSettings.sessionId

  const { status, statusText, setStatus } = useSpeakingStatus()
  const elapsed = useElapsedTimer()
  const [isEndModalOpen, setEndModalOpen] = useState(false)
  const [isEnding, setEnding] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [turns, setTurns] = useState([])

  const mediaRecorderRef = useRef(null)
  const audioChunksRef = useRef([])
  const streamRef = useRef(null)
  const audioPlaybackRef = useRef(null)
  const conversationScrollRef = useRef(null)

  // Setup을 거치지 않고 URL로 직접 들어오는 등 실제 세션 정보가 없으면
  // 대화를 진행할 수 없으므로 설정 화면으로 되돌려보냅니다.
  useEffect(() => {
    if (!sessionId) {
      navigate('/conversation/setup', { replace: true })
    }
  }, [sessionId, navigate])

  useEffect(() => {
    return () => {
      streamRef.current?.getTracks().forEach((track) => track.stop())
      audioPlaybackRef.current?.pause()
    }
  }, [])

  // 새 Message가 추가될 때마다 Conversation 영역 내부에서 최신 Message가 보이도록 scroll합니다.
  useEffect(() => {
    const el = conversationScrollRef.current
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  }, [turns])

  const sessionTitle = buildSessionTitle(settings.situation, settings.partner)
  const sessionMeta = `일반 회화 · ${settings.partner} · ${DifficultyLabel[settings.difficulty]}`

  const handleBack = () => navigate('/conversation/setup')

  const startRecording = async () => {
    setErrorMessage('')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      const mimeType = pickRecorderMimeType()
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
      audioChunksRef.current = []

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data)
        }
      }

      recorder.onstop = () => {
        streamRef.current?.getTracks().forEach((track) => track.stop())
        streamRef.current = null
        const blob = new Blob(audioChunksRef.current, { type: recorder.mimeType || 'audio/webm' })
        submitTurn(blob)
      }

      mediaRecorderRef.current = recorder
      recorder.start()
      setStatus('listening')
    } catch {
      setErrorMessage('마이크 권한이 필요합니다. 브라우저 설정에서 마이크 접근을 허용해주세요.')
    }
  }

  const stopRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop()
    }
  }

  const handleMicClick = () => {
    if (status === 'idle') {
      startRecording()
    } else if (status === 'listening') {
      stopRecording()
    }
    // thinking/speaking 중에는 클릭을 무시해 동일 turn이 중복 요청되지 않도록 합니다.
  }

  const submitTurn = async (audioBlob) => {
    setStatus('thinking')
    try {
      const result = await submitAudioTurn(sessionId, audioBlob, 'turn.webm')

      setTurns((prev) => [
        ...prev,
        { speaker: 'user', jp: result.userMessage.content, kr: null },
        { speaker: 'ai', jp: result.aiMessage.content, kr: result.aiMessageKoreanSubtitle },
      ])

      playAiAudio(result.aiAudioBase64, result.aiAudioMimeType)
    } catch (error) {
      setErrorMessage(error.message || '응답을 받아오지 못했습니다. 다시 시도해주세요.')
      setStatus('idle')
    }
  }

  const playAiAudio = (base64Audio, mimeType) => {
    if (!base64Audio) {
      setStatus('idle')
      return
    }
    setStatus('speaking')
    const audio = new Audio(`data:${mimeType || 'audio/mpeg'};base64,${base64Audio}`)
    audioPlaybackRef.current = audio
    const finish = () => setStatus('idle')
    audio.onended = finish
    audio.onerror = finish
    audio.play().catch(finish)
  }

  const handleEndClick = () => {
    if (status === 'thinking' || isEnding) return
    setEndModalOpen(true)
  }
  const handleContinue = () => setEndModalOpen(false)

  const handleConfirmEnd = async () => {
    if (isEnding) return
    setEnding(true)
    setEndModalOpen(false)
    stopRecording()
    audioPlaybackRef.current?.pause()

    try {
      await completeConversation(sessionId)
      navigate('/conversation/feedback', { state: { sessionId } })
    } catch (error) {
      setErrorMessage(error.message || '학습 종료 처리에 실패했습니다. 다시 시도해주세요.')
      setEnding(false)
    }
  }

  const isMicDisabled = status === 'thinking' || status === 'speaking' || isEnding

  return (
    <LearnerLayout>
      <div className="speaking-page conversation-speaking-page">
        <div className="speaking-header">
          <div className="speaking-header__back-row">
            <button
              type="button"
              className="setup-page__back"
              onClick={handleBack}
              aria-label="설정으로 돌아가기"
            >
              <BackArrowIcon size={18} />
            </button>
            <div className="speaking-header__title-block">
              <p className="speaking-header__title">{sessionTitle}</p>
              <p className="speaking-header__meta">{sessionMeta}</p>
            </div>
          </div>

          <div className="speaking-timer">
            <span className="speaking-timer__dot" />
            <span className="speaking-timer__time">{elapsed}</span>
          </div>
        </div>

        <div className="voice-orb-area">
          <VoiceOrb variant="lg" status={status} />
          <p className="voice-orb-area__status">{statusText}</p>
        </div>

        {errorMessage && (
          <p className="setup-page__error" role="alert">
            {errorMessage}
          </p>
        )}

        <div className="caption-card" ref={conversationScrollRef}>
          {settings.subtitleMode === SubtitleMode.OFF ? (
            <p className="speaking-subtitle-off">자막이 꺼져 있어요.</p>
          ) : turns.length === 0 ? (
            <p className="speaking-subtitle-off">마이크를 눌러 대화를 시작해보세요.</p>
          ) : (
            turns.map((turn, index) => (
              <div
                key={index}
                className={`caption-bubble-row${turn.speaker === 'user' ? ' caption-bubble-row--user' : ''}`}
              >
                <div className={`caption-bubble${turn.speaker === 'user' ? ' caption-bubble--user' : ''}`}>
                  <p className="caption-bubble__speaker">
                    {turn.speaker === 'user' ? '나' : `AI · ${settings.partner}`}
                  </p>
                  <p className="caption-bubble__jp">{turn.jp}</p>
                  {settings.subtitleMode === SubtitleMode.JAPANESE_KOREAN && turn.kr && (
                    <p className="caption-bubble__kr">{turn.kr}</p>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        <div className="speaking-controls">
          <button
            type="button"
            className={`speaking-mic-button${status !== 'idle' ? ' speaking-mic-button--active' : ''}`}
            onClick={handleMicClick}
            disabled={isMicDisabled}
            aria-label={`마이크, 현재 상태: ${statusText}`}
          >
            <MicIcon size={22} />
          </button>

          <button type="button" className="speaking-end-button" onClick={handleEndClick} disabled={isEnding}>
            <EndCallIcon size={16} className="speaking-end-button__icon" />
            {isEnding ? '종료하는 중...' : '대화 종료'}
          </button>
        </div>
      </div>

      <SessionEndModal open={isEndModalOpen} onContinue={handleContinue} onEnd={handleConfirmEnd} />
    </LearnerLayout>
  )
}

export default ConversationSpeakingPage
