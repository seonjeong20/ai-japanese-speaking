// `options`는 [{ value, label }] 형태의 enum 옵션 목록입니다.
// (예: DIFFICULTY_OPTIONS, SUBTITLE_MODE_OPTIONS from '../../data/enums')
function SegmentedControl({ label, options, value, onChange }) {
  return (
    <div className="segmented-control">
      <span className="segmented-control__label">{label}</span>
      <div className="segmented-control__row" role="radiogroup" aria-label={label}>
        {options.map((option) => {
          const isSelected = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={isSelected}
              className={`segmented-control__chip${isSelected ? ' segmented-control__chip--selected' : ''}`}
              onClick={() => onChange(option.value)}
            >
              {option.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export default SegmentedControl
