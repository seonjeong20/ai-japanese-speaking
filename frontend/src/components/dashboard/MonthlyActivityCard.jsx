import { ChevronLeftSmallIcon, ChevronRightSmallIcon } from '../icons/DashboardIcons'

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

function getIntensityClass(minutes) {
  if (!minutes) return 'monthly-cell--none'
  if (minutes <= 20) return 'monthly-cell--low'
  if (minutes <= 45) return 'monthly-cell--medium'
  return 'monthly-cell--high'
}

function MonthlyActivityCard({ data }) {
  const studiedDays = data.weeks.flat().filter((minutes) => minutes > 0).length

  return (
    <section className="dashboard-card monthly-activity-card">
      <header className="dashboard-card__header">
        <h2 className="dashboard-card__title dashboard-card__title--sm">{data.label}</h2>
        <div className="monthly-activity-card__nav">
          <ChevronLeftSmallIcon size={14} />
          <span>{data.month}</span>
          <ChevronRightSmallIcon size={14} />
        </div>
      </header>

      <div className="monthly-activity-card__weekdays">
        {WEEKDAY_LABELS.map((day) => (
          <span key={day}>{day}</span>
        ))}
      </div>

      <div className="monthly-activity-card__grid">
        {data.weeks.map((week, weekIndex) => (
          <div className="monthly-activity-card__row" key={weekIndex}>
            {week.map((minutes, dayIndex) => (
              <span key={dayIndex} className={`monthly-cell ${getIntensityClass(minutes)}`} />
            ))}
          </div>
        ))}
      </div>

      <p className="monthly-activity-card__summary">이번 달 {studiedDays}일 학습했어요!</p>

      <ul className="monthly-activity-card__legend">
        <li>
          <span className="monthly-cell monthly-cell--high" />
          많이 학습
        </li>
        <li>
          <span className="monthly-cell monthly-cell--medium" />
          보통
        </li>
        <li>
          <span className="monthly-cell monthly-cell--low" />
          조금
        </li>
        <li>
          <span className="monthly-cell monthly-cell--none" />
          없음
        </li>
      </ul>
    </section>
  )
}

export default MonthlyActivityCard
