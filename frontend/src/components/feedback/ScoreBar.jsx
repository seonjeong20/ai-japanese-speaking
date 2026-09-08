function ScoreBar({ label, score, size = 'md' }) {
  return (
    <div className={`score-bar score-bar--${size}`}>
      <div className="score-bar__row">
        <p className="score-bar__label">{label}</p>
        <p className="score-bar__score">{score}점</p>
      </div>
      <div className="score-bar__track">
        <div className="score-bar__fill" style={{ width: `${score}%` }} />
      </div>
    </div>
  )
}

export default ScoreBar
