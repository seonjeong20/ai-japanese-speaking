function SetupCta({ label, onClick, disabled = false }) {
  return (
    <button type="button" className="setup-page__cta" onClick={onClick} disabled={disabled}>
      {label}
    </button>
  )
}

export default SetupCta
