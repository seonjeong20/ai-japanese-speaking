import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { getCurrentUser, hasValidSession } from '../../api/client'
import { Role } from '../../data/enums'

// 로그인 성공 시 이동하는 역할별 기본 화면입니다. SignInForm과 동일한 정책을 공유합니다.
const ROLE_HOME_PATH = {
  [Role.LEARNER]: '/dashboard',
  [Role.MANAGER]: '/manager/dashboard',
  [Role.ADMIN]: '/admin/dashboard',
}

// 토큰이 없거나 만료된 상태로 보호된 라우트에 진입하면 로그인 화면으로 돌려보냅니다.
// allowedRoles가 주어지면, 로그인은 되어 있으나 역할이 다른 사용자는 자신의 기본 화면으로 돌려보냅니다.
function ProtectedRoute({ allowedRoles }) {
  const location = useLocation()

  if (!hasValidSession()) {
    return <Navigate to="/" replace state={{ from: location }} />
  }

  if (allowedRoles) {
    const user = getCurrentUser()
    if (!user || !allowedRoles.includes(user.role)) {
      return <Navigate to={ROLE_HOME_PATH[user?.role] || '/'} replace />
    }
  }

  return <Outlet />
}

export default ProtectedRoute
