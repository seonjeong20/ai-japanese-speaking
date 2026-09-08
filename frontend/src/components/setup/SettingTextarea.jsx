function SettingTextarea({ label, icon: Icon, value, onChange, placeholder, rows = 3 }) {
  return (
    <div className="setting-field">
      <div className="setting-field__label-row">
        <Icon size={15} className="setting-field__label-icon" />
        <span className="setting-field__label">{label}</span>
      </div>

      <textarea
        className="setup-page__textarea"
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={rows}
      />
    </div>
  )
}

export default SettingTextarea
