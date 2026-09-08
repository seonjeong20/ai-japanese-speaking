import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import LearningModeCard from '../../components/learning/LearningModeCard'
import './LearningPage.css'

const LEARNING_MODES = [
  {
    id: 'conversation',
    type: 'conversation',
    title: '일반 회화',
    description: '다양한 상황에서 자연스럽게\n일본어로 대화하는 연습을 해요.',
  },
  {
    id: 'interview',
    type: 'interview',
    title: '면접 회화',
    description: '실제 면접처럼 질문을 받고\n일본어로 답변하며 연습해요.',
  },
]

function LearningPage() {
  const navigate = useNavigate()

  const handleStartConversation = () => navigate('/conversation/setup')
  const handleStartInterview = () => navigate('/interview/setup')

  const handlersByType = {
    conversation: handleStartConversation,
    interview: handleStartInterview,
  }

  return (
    <LearnerLayout>
      <div className="learning-page">
        <div className="learning-page__header">
          <h1 className="learning-page__title">Learning</h1>
          <p className="learning-page__subtitle">
            어떤 대화를 해볼까요?
            <br />
            상황에 맞는 일본어를 연습해보세요.
          </p>
        </div>

        <div className="learning-page__modes">
          {LEARNING_MODES.map((mode) => (
            <LearningModeCard
              key={mode.id}
              title={mode.title}
              description={mode.description}
              type={mode.type}
              onStart={handlersByType[mode.type]}
            />
          ))}
        </div>
      </div>
    </LearnerLayout>
  )
}

export default LearningPage
