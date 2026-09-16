import { useEffect, useState } from 'react'
import LearnerLayout from '../../components/layout/LearnerLayout'
import RecentLearningCard from '../../components/dashboard/RecentLearningCard'
import MonthlyActivityCard from '../../components/dashboard/MonthlyActivityCard'
import WeeklyStudyCard from '../../components/dashboard/WeeklyStudyCard'
import { BellIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { getCurrentUser } from '../../api/client'
import { getMyHistory } from '../../api/history'
import { SessionTypeLabel } from '../../data/enums'
import './DashboardPage.css'

const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일']

function formatDateLabel(isoDateTime) {
  if (!isoDateTime) return ''
  const date = new Date(isoDateTime)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

function toMinutes(durationSeconds) {
  if (durationSeconds == null) return 0
  return Math.round(durationSeconds / 60)
}

// 최근 학습 내역: /api/history(최신순)를 그대로 사용해 상위 4건만 보여준다.
function buildRecentLearning(historyItems) {
  return historyItems.slice(0, 4).map((item) => ({
    id: item.sessionId,
    type: SessionTypeLabel[item.sessionType] ?? item.sessionType,
    title: item.title,
    date: formatDateLabel(item.startedAt),
    duration: toMinutes(item.durationSeconds),
  }))
}

// 이번 달 학습 잔디: 세션의 startedAt 날짜별로 durationSeconds 합계를 구해
// 달력 주(일~토) 단위 2차원 배열로 만든다. MonthlyActivityCard는 이 배열의
// 값(분)만 보고 색상 구간을 스스로 정하므로 별도 변환이 필요 없다.
function buildMonthlyActivity(historyItems, now) {
  const year = now.getFullYear()
  const month = now.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  const minutesByDay = new Array(daysInMonth + 1).fill(0)
  historyItems.forEach((item) => {
    const started = new Date(item.startedAt)
    if (Number.isNaN(started.getTime())) return
    if (started.getFullYear() !== year || started.getMonth() !== month) return
    minutesByDay[started.getDate()] += toMinutes(item.durationSeconds)
  })

  const firstWeekday = new Date(year, month, 1).getDay() // 0(일) ~ 6(토)
  const weeks = []
  let currentWeek = new Array(firstWeekday).fill(0)
  for (let day = 1; day <= daysInMonth; day += 1) {
    currentWeek.push(minutesByDay[day])
    if (currentWeek.length === 7) {
      weeks.push(currentWeek)
      currentWeek = []
    }
  }
  if (currentWeek.length > 0) weeks.push(currentWeek)

  return {
    month: `${year}년 ${month + 1}월`,
    label: `${month + 1}월 학습 기록`,
    weeks,
  }
}

// 주간 학습 시간: 이번 주(월~일) 각 요일의 durationSeconds 합계.
function buildWeeklyStudy(historyItems, now) {
  const startOfWeek = new Date(now)
  const weekday = startOfWeek.getDay() // 0(일) ~ 6(토)
  const diffToMonday = weekday === 0 ? 6 : weekday - 1
  startOfWeek.setDate(startOfWeek.getDate() - diffToMonday)
  startOfWeek.setHours(0, 0, 0, 0)

  const endOfWeek = new Date(startOfWeek)
  endOfWeek.setDate(endOfWeek.getDate() + 7)

  const minutesByLabel = new Map(WEEKDAY_LABELS.map((label) => [label, 0]))
  historyItems.forEach((item) => {
    const started = new Date(item.startedAt)
    if (Number.isNaN(started.getTime())) return
    if (started < startOfWeek || started >= endOfWeek) return
    const dayIndex = (started.getDay() + 6) % 7 // 월=0 ... 일=6
    const label = WEEKDAY_LABELS[dayIndex]
    minutesByLabel.set(label, minutesByLabel.get(label) + toMinutes(item.durationSeconds))
  })

  return {
    weekLabel: '이번 주',
    days: WEEKDAY_LABELS.map((label) => ({ label, minutes: minutesByLabel.get(label) })),
  }
}

function DashboardPage() {
  const user = getCurrentUser()
  const [historyItems, setHistoryItems] = useState([])
  const [isLoading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    getMyHistory()
      .then((data) => {
        if (!cancelled) setHistoryItems(data?.items ?? [])
      })
      .catch(() => {
        if (!cancelled) setHistoryItems([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const now = new Date()
  const recentLearning = buildRecentLearning(historyItems)
  const monthlyActivity = buildMonthlyActivity(historyItems, now)
  const weeklyStudy = buildWeeklyStudy(historyItems, now)

  return (
    <LearnerLayout>
      <div className="dashboard-page">
        <div className="dashboard-topbar">
          <div className="dashboard-topbar__greeting">
            <div className="dashboard-topbar__greeting-row">
              <h1>안녕하세요, {user?.name ?? ''}님!</h1>
              <SparkleIcon size={20} className="dashboard-topbar__sparkle" />
            </div>
            <p>오늘도 일본어와 함께 성장하는 하루가 되길 바라요.</p>
          </div>

          <div className="dashboard-topbar__actions">
            <button type="button" className="dashboard-topbar__icon-button" aria-label="알림">
              <BellIcon size={18} />
            </button>
          </div>
        </div>

        {!isLoading && (
          <div className="dashboard-cards-row">
            <RecentLearningCard items={recentLearning} />

            <div className="dashboard-cards-row__right">
              <MonthlyActivityCard data={monthlyActivity} />
              <WeeklyStudyCard data={weeklyStudy} />
            </div>
          </div>
        )}
      </div>
    </LearnerLayout>
  )
}

export default DashboardPage
