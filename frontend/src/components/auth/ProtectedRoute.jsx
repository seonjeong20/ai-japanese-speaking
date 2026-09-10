import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { hasValidSession } from '../../api/client'

// 토큰이 없거나 만료된 상태로 보호된 라우트에 진입하면 로그인 화면으로 돌려보냅니다.
function ProtectedRoute() {
  const location = useLocation()

  if (!hasValidSession()) {
    return <Navigate to="/" replace state={{ from: location }} />
  }

  return <Outlet />
}

export default ProtectedRoute
