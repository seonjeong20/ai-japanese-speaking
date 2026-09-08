import { useEffect, useRef, useState } from 'react'

const PRESS_ANIMATION_MS = 180

function formatDuration(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours === 0) return `${minutes}분`
  if (minutes === 0) return `${hours}시간`
  return `${hours}시간 ${minutes}분`
}

function WeeklyStudyCard({ data }) {
  const [selectedDay, setSelectedDay] = useState(null)
  const [pressedDay, setPressedDay] = useState(null)
  const pressTimeoutRef = useRef(null)

  useEffect(() => {
    return () => clearTimeout(pressTimeoutRef.current)
  }, [])

  const totalMinutes = data.days.reduce((sum, day) => sum + day.minutes, 0)
  const averageMinutes = Math.floor(totalMinutes / data.days.length)
  const maxMinutes = Math.max(...data.days.map((day) => day.minutes), 1)

  const handleBarClick = (label) => {
    setSelectedDay((current) => (current === label ? null : label))

    clearTimeout(pressTimeoutRef.current)
    setPressedDay(label)
    pressTimeoutRef.current = setTimeout(() => {
      setPressedDay((current) => (current === label ? null : current))
    }, PRESS_ANIMATION_MS)
  }

  return (
    <section className="dashboard-card weekly-study-card">
      <header className="dashboard-card__header">
        <h2 className="dashboard-card__title dashboard-card__title--sm">주간 학습 시간</h2>
        <span className="weekly-study-card__week-label">{data.weekLabel}</span>
      </header>

      <div className="weekly-study-card__stat-row">
        <p className="weekly-study-card__total">{formatDuration(totalMinutes)}</p>
        <p className="weekly-study-card__average">일 평균 {averageMinutes}분</p>
      </div>

      <div className="weekly-study-card__chart">
        <div className="weekly-study-card__bars">
          {data.days.map((day) => {
            const heightPercent = Math.max((day.minutes / maxMinutes) * 100, 4)
            const isAboveAverage = day.minutes >= averageMinutes
            const isSelected = selectedDay === day.label
            const isPressed = pressedDay === day.label

            const barClassName = [
              'weekly-study-card__bar',
              isAboveAverage && 'weekly-study-card__bar--lime',
              isSelected && 'weekly-study-card__bar--selected',
              isPressed && 'weekly-study-card__bar--pressed',
            ]
              .filter(Boolean)
              .join(' ')

            return (
              <div className="weekly-study-card__bar-col" key={day.label}>
                <button
                  type="button"
                  className="weekly-study-card__bar-button"
                  aria-label={`${day.label}요일 학습 시간 ${formatDuration(day.minutes)}`}
                  aria-pressed={isSelected}
                  onClick={() => handleBarClick(day.label)}
                >
                  <span className={barClassName} style={{ height: `${heightPercent}%` }}>
                    {isSelected && (
                      <span className="weekly-study-card__tooltip" role="status">
                        {formatDuration(day.minutes)}
                      </span>
                    )}
                  </span>
                </button>
              </div>
            )
          })}
        </div>
        <div className="weekly-study-card__labels">
          {data.days.map((day) => (
            <span key={day.label}>{day.label}</span>
          ))}
        </div>
      </div>
    </section>
  )
}

export default WeeklyStudyCard
