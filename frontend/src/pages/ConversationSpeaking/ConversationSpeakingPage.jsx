import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import VoiceOrb from '../../components/speaking/VoiceOrb'
import SessionEndModal from '../../components/speaking/SessionEndModal'
import { useSpeakingStatus } from '../../components/speaking/useSpeakingStatus'
import { useElapsedTimer } from '../../components/speaking/useElapsedTimer'
import { BackArrowIcon, EndCallIcon, MicIcon } from '../../components/icons/DashboardIcons'
import { Difficulty, DifficultyLabel, SubtitleMode } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/speaking/SpeakingSession.css'
import './ConversationSpeakingPage.css'

// URL로 직접 접근하는 등 Setup에서 전달된 state가 없을 때 사용하는 기본값입니다.
const DEFAULT_SETTINGS = {
  situation: '카페에서',
  partner: '친구',
  personality: '친절하고 활발한',
  description: '',
  difficulty: Difficulty.INTERMEDIATE,
  subtitleMode: SubtitleMode.JAPANESE,
}

// STT/LLM이 아직 연결되지 않아 화면 확인용으로 고정해 둔 예시 대화입니다.
const MOCK_TURNS = [
  {
    speaker: 'ai',
    jp: 'いらっしゃいませ。ご注文はお決まりですか?',
    kr: '어서오세요. 주문 정하셨나요?',
  },
  {
    speaker: 'user',
    jp: 'あ、まだです。おすすめは何ですか?',
    kr: '아, 아직이요. 추천 메뉴가 뭐예요?',
  },
]

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

  const { status, statusText, cycleStatus } = useSpeakingStatus()
  const elapsed = useElapsedTimer()
  const [isEndModalOpen, setEndModalOpen] = useState(false)

  const sessionTitle = buildSessionTitle(settings.situation, settings.partner)
  const sessionMeta = `일반 회화 · ${settings.partner} · ${DifficultyLabel[settings.difficulty]}`

  const handleBack = () => navigate('/conversation/setup')
  const handleEndClick = () => setEndModalOpen(true)
  const handleContinue = () => setEndModalOpen(false)
  const handleConfirmEnd = () => navigate('/conversation/feedback')

  return (
    <LearnerLayout>
      <div className="speaking-page">
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

        <div className="caption-card">
          {settings.subtitleMode === SubtitleMode.OFF ? (
            <p className="speaking-subtitle-off">자막이 꺼져 있어요.</p>
          ) : (
            MOCK_TURNS.map((turn, index) => (
              <div
                key={index}
                className={`caption-bubble-row${turn.speaker === 'user' ? ' caption-bubble-row--user' : ''}`}
              >
                <div className={`caption-bubble${turn.speaker === 'user' ? ' caption-bubble--user' : ''}`}>
                  <p className="caption-bubble__speaker">
                    {turn.speaker === 'user' ? '나' : `AI · ${settings.partner}`}
                  </p>
                  <p className="caption-bubble__jp">{turn.jp}</p>
                  {settings.subtitleMode === SubtitleMode.JAPANESE_KOREAN && (
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
            onClick={cycleStatus}
            aria-label={`마이크, 현재 상태: ${statusText}`}
          >
            <MicIcon size={22} />
          </button>

          <button type="button" className="speaking-end-button" onClick={handleEndClick}>
            <EndCallIcon size={16} className="speaking-end-button__icon" />
            대화 종료
          </button>
        </div>
      </div>

      <SessionEndModal open={isEndModalOpen} onContinue={handleContinue} onEnd={handleConfirmEnd} />
    </LearnerLayout>
  )
}

export default ConversationSpeakingPage
