import { useEffect } from 'react'

function SessionEndModal({ open, onContinue, onEnd }) {
  useEffect(() => {
    if (!open) return undefined

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onContinue()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [open, onContinue])

  if (!open) return null

  return (
    <div className="session-end-modal__backdrop" role="presentation" onClick={onContinue}>
      <div
        className="session-end-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-end-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <p id="session-end-modal-title" className="session-end-modal__title">
          학습을 종료하시겠습니까?
        </p>

        <div className="session-end-modal__actions">
          <button
            type="button"
            className="session-end-modal__button session-end-modal__button--ghost"
            onClick={onContinue}
          >
            계속하기
          </button>
          <button
            type="button"
            className="session-end-modal__button session-end-modal__button--primary"
            onClick={onEnd}
          >
            종료하기
          </button>
        </div>
      </div>
    </div>
  )
}

export default SessionEndModal
