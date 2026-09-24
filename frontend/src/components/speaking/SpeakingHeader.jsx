import { BackArrowIcon } from '../icons/DashboardIcons'

// 일반 회화 / 면접 Speaking 화면 상단의 뒤로가기 · 세션 제목 · 경과 시간 영역입니다.
// 뒤로가기 시 실제로 무엇을 할지(세션 중단, 화면 이동, 확인 모달 등)는 각 Page가 onBack으로 결정합니다.
function SpeakingHeader({ title, meta, elapsed, onBack, backLabel }) {
  return (
    <div className="speaking-header">
      <div className="speaking-header__back-row">
        <button type="button" className="setup-page__back" onClick={onBack} aria-label={backLabel}>
          <BackArrowIcon size={18} />
        </button>
        <div className="speaking-header__title-block">
          <p className="speaking-header__title">{title}</p>
          <p className="speaking-header__meta">{meta}</p>
        </div>
      </div>

      <div className="speaking-timer">
        <span className="speaking-timer__dot" />
        <span className="speaking-timer__time">{elapsed}</span>
      </div>
    </div>
  )
}

export default SpeakingHeader
