function SetupCta({ label, onClick }) {
  return (
    <button type="button" className="setup-page__cta" onClick={onClick}>
      {label}
    </button>
  )
}

export default SetupCta
