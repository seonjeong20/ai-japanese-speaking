import { ClockIcon, EditIcon, MessageIcon } from '../icons/DashboardIcons'

const STAT_ICONS = {
  clock: ClockIcon,
  message: MessageIcon,
  edit: EditIcon,
}

// 일반 회화 Feedback의 "총 대화 시간 / 주고받은 문장 / 표현 교정" 통계 줄입니다.
// ConversationFeedbackPage와 History Detail(과거 일반 회화 기록)에서 함께 사용합니다.
function FeedbackStatsRow({ stats }) {
  return (
    <div className="conversation-feedback-stats">
      {stats.map((stat) => {
        const Icon = STAT_ICONS[stat.icon]
        return (
          <div key={stat.id} className="conversation-feedback-stat">
            <span className="conversation-feedback-stat__icon">
              <Icon size={17} />
            </span>
            <div className="conversation-feedback-stat__text">
              <p className="conversation-feedback-stat__value">{stat.value}</p>
              <p className="conversation-feedback-stat__label">{stat.label}</p>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default FeedbackStatsRow
