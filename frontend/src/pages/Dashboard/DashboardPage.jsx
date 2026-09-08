import LearnerLayout from '../../components/layout/LearnerLayout'
import RecentLearningCard from '../../components/dashboard/RecentLearningCard'
import MonthlyActivityCard from '../../components/dashboard/MonthlyActivityCard'
import WeeklyStudyCard from '../../components/dashboard/WeeklyStudyCard'
import { BellIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { currentUser, monthlyActivity, recentLearning, weeklyStudy } from '../../data/dashboardMockData'
import './DashboardPage.css'

function DashboardPage() {
  return (
    <LearnerLayout>
      <div className="dashboard-page">
        <div className="dashboard-topbar">
          <div className="dashboard-topbar__greeting">
            <div className="dashboard-topbar__greeting-row">
              <h1>안녕하세요, {currentUser.name}님!</h1>
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

        <div className="dashboard-cards-row">
          <RecentLearningCard items={recentLearning} />

          <div className="dashboard-cards-row__right">
            <MonthlyActivityCard data={monthlyActivity} />
            <WeeklyStudyCard data={weeklyStudy} />
          </div>
        </div>
      </div>
    </LearnerLayout>
  )
}

export default DashboardPage
