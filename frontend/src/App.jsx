import { useEffect } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AUTH_EXPIRED_EVENT } from './api/client'
import { Role } from './data/enums'
import ProtectedRoute from './components/auth/ProtectedRoute'
import AuthPage from './pages/Auth/AuthPage'
import AuthCallbackPage from './pages/AuthCallback/AuthCallbackPage'
import DashboardPage from './pages/Dashboard/DashboardPage'
import LearningPage from './pages/Learning/LearningPage'
import ConversationSetupPage from './pages/ConversationSetup/ConversationSetupPage'
import InterviewSetupPage from './pages/InterviewSetup/InterviewSetupPage'
import ConversationSpeakingPage from './pages/ConversationSpeaking/ConversationSpeakingPage'
import InterviewSpeakingPage from './pages/InterviewSpeaking/InterviewSpeakingPage'
import ConversationFeedbackPage from './pages/ConversationFeedback/ConversationFeedbackPage'
import InterviewFeedbackPage from './pages/InterviewFeedback/InterviewFeedbackPage'
import MyHistoryPage from './pages/MyHistory/MyHistoryPage'
import HistoryDetailPage from './pages/HistoryDetail/HistoryDetailPage'
import SettingsPage from './pages/Settings/SettingsPage'

function App() {
  const navigate = useNavigate()

  // 백엔드 API가 401(토큰 만료/무효)을 반환하면 client.js가 이 이벤트를 쏩니다.
  // 어느 화면에 있든 즉시 로그인 화면으로 돌려보냅니다.
  useEffect(() => {
    const handleAuthExpired = () => navigate('/', { replace: true })
    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  }, [navigate])

  return (
    <Routes>
      <Route path="/" element={<AuthPage />} />
      <Route path="/auth/callback/:provider" element={<AuthCallbackPage />} />
      <Route element={<ProtectedRoute allowedRoles={[Role.LEARNER]} />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/learning" element={<LearningPage />} />
        <Route path="/conversation/setup" element={<ConversationSetupPage />} />
        <Route path="/interview/setup" element={<InterviewSetupPage />} />
        <Route path="/conversation/speaking" element={<ConversationSpeakingPage />} />
        <Route path="/interview/speaking" element={<InterviewSpeakingPage />} />
        <Route path="/conversation/feedback" element={<ConversationFeedbackPage />} />
        <Route path="/interview/feedback" element={<InterviewFeedbackPage />} />
        <Route path="/history" element={<MyHistoryPage />} />
        <Route path="/history/:id" element={<HistoryDetailPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
