export const currentUser = {
  name: 'seonjeong',
  initial: 'S',
}

export const recentLearning = [
  {
    id: 1,
    type: '일반 회화',
    title: '카페에서 친구와 대화하기',
    date: '9월 8일',
    duration: 24,
  },
  {
    id: 2,
    type: '면접 회화',
    title: 'Backend Developer 면접 연습',
    date: '9월 7일',
    duration: 18,
  },
  {
    id: 3,
    type: '일반 회화',
    title: '직장 동료와 점심시간',
    date: '9월 5일',
    duration: 31,
  },
  {
    id: 4,
    type: '면접 회화',
    title: '자기소개 연습',
    date: '9월 3일',
    duration: 27,
  },
]

// 학습량에 따른 잔디 셀 색상 구간: 0분 / 1~20분 / 21~45분 / 46분 이상
export const monthlyActivity = {
  month: '2025년 9월',
  label: '9월 학습 기록',
  weeks: [
    [60, 0, 35, 15, 0, 60, 0],
    [0, 35, 0, 60, 15, 0, 35],
    [15, 0, 0, 60, 35, 0, 0],
    [0, 60, 0, 35, 0, 15, 0],
    [60, 35],
  ],
}

export const weeklyStudy = {
  weekLabel: '이번 주',
  days: [
    { label: '월', minutes: 20 },
    { label: '화', minutes: 45 },
    { label: '수', minutes: 15 },
    { label: '목', minutes: 60 },
    { label: '금', minutes: 40 },
    { label: '토', minutes: 25 },
    { label: '일', minutes: 17 },
  ],
}
