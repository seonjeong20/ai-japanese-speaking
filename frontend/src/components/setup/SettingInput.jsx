function SettingInput({ id, label, icon: Icon, value, onChange, options, placeholder }) {
  const listId = `${id}-options`

  return (
    <div className="setting-field">
      <div className="setting-field__label-row">
        <Icon size={15} className="setting-field__label-icon" />
        <span className="setting-field__label">{label}</span>
      </div>

      <input
        type="text"
        id={id}
        list={listId}
        className="setting-input"
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-label={label}
        autoComplete="off"
      />
      <datalist id={listId}>
        {options.map((option) => (
          <option key={option} value={option} />
        ))}
      </datalist>
    </div>
  )
}

export default SettingInput
