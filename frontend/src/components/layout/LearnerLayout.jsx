import Sidebar from './Sidebar'
import './LearnerLayout.css'

function LearnerLayout({ children }) {
  return (
    <div className="learner-layout">
      <Sidebar />
      <main className="learner-layout__content">{children}</main>
    </div>
  )
}

export default LearnerLayout
