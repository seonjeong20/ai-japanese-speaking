import { Navigate, Route, Routes } from 'react-router-dom'
import AuthPage from './pages/Auth/AuthPage'
import DashboardPage from './pages/Dashboard/DashboardPage'
import LearningPage from './pages/Learning/LearningPage'
import ConversationSetupPage from './pages/ConversationSetup/ConversationSetupPage'
import InterviewSetupPage from './pages/InterviewSetup/InterviewSetupPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<AuthPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/learning" element={<LearningPage />} />
      <Route path="/conversation/setup" element={<ConversationSetupPage />} />
      <Route path="/interview/setup" element={<InterviewSetupPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
