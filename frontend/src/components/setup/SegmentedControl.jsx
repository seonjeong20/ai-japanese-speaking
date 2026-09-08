function SegmentedControl({ label, options, value, onChange }) {
  return (
    <div className="segmented-control">
      <span className="segmented-control__label">{label}</span>
      <div className="segmented-control__row" role="radiogroup" aria-label={label}>
        {options.map((option) => {
          const isSelected = option === value
          return (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={isSelected}
              className={`segmented-control__chip${isSelected ? ' segmented-control__chip--selected' : ''}`}
              onClick={() => onChange(option)}
            >
              {option}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export default SegmentedControl
