const VARIANTS = {
  lg: {
    size: 220,
    rings: [
      { r: 110, className: 'voice-orb__ring voice-orb__ring--outer' },
      { r: 85, className: 'voice-orb__ring voice-orb__ring--mid' },
    ],
    coreRadius: 60,
    micPath:
      'M102.917 108.583C102.917 110.462 103.663 112.264 104.991 113.592C106.32 114.92 108.121 115.667 110 115.667C111.879 115.667 113.68 114.92 115.009 113.592C116.337 112.264 117.083 110.462 117.083 108.583M110 119.917V124.167M110 114.25C111.127 114.25 112.208 113.802 113.005 113.005C113.802 112.208 114.25 111.127 114.25 110V101.5C114.25 100.373 113.802 99.2918 113.005 98.4948C112.208 97.6978 111.127 97.25 110 97.25C108.873 97.25 107.792 97.6978 106.995 98.4948C106.198 99.2918 105.75 100.373 105.75 101.5V110C105.75 111.127 106.198 112.208 106.995 113.005C107.792 113.802 108.873 114.25 110 114.25Z',
    micStrokeWidth: 2.55,
  },
  sm: {
    size: 150,
    rings: [{ r: 75, className: 'voice-orb__ring voice-orb__ring--outer' }],
    coreRadius: 48,
    micPath:
      'M69.1667 73.8333C69.1667 75.3804 69.7812 76.8642 70.8752 77.9581C71.9692 79.0521 73.4529 79.6667 75 79.6667C76.5471 79.6667 78.0308 79.0521 79.1248 77.9581C80.2187 76.8642 80.8333 75.3804 80.8333 73.8333M75 83.1667V86.6667M75 78.5C75.9283 78.5 76.8185 78.1313 77.4749 77.4749C78.1312 76.8185 78.5 75.9283 78.5 75V68C78.5 67.0717 78.1312 66.1815 77.4749 65.5251C76.8185 64.8687 75.9283 64.5 75 64.5C74.0717 64.5 73.1815 64.8687 72.5251 65.5251C71.8687 66.1815 71.5 67.0717 71.5 68V75C71.5 75.9283 71.8687 76.8185 72.5251 77.4749C73.1815 78.1313 74.0717 78.5 75 78.5Z',
    micStrokeWidth: 2.1,
  },
}

// interactive=false(기본값)일 때는 기존과 완전히 동일한 마크업(정적 core+mic)을 렌더링합니다.
// interactive=true일 때만 core+mic 부분을 별도의 <button>으로 분리해 실제 클릭 가능한
// 마이크 버튼으로 만듭니다. 두 렌더링 경로가 분리되어 있어 interactive를 넘기지 않는
// 기존 사용처(Conversation Speaking)의 출력은 전혀 바뀌지 않습니다.
function VoiceOrb({ variant = 'lg', status = 'idle', interactive = false, onActivate, ariaLabel }) {
  const config = VARIANTS[variant]
  const center = config.size / 2

  return (
    <div
      className={`voice-orb voice-orb--${variant} voice-orb--${status}`}
      style={{ width: config.size, height: config.size }}
    >
      {status === 'thinking' && <div className="voice-orb__sheen" aria-hidden="true" />}
      <svg width={config.size} height={config.size} viewBox={`0 0 ${config.size} ${config.size}`} fill="none" aria-hidden="true">
        {config.rings.map((ring) => (
          <circle key={ring.className} className={ring.className} cx={center} cy={center} r={ring.r} />
        ))}
        {!interactive && (
          <>
            <circle className="voice-orb__core" cx={center} cy={center} r={config.coreRadius} />
            <path
              className="voice-orb__mic"
              d={config.micPath}
              strokeWidth={config.micStrokeWidth}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </>
        )}
      </svg>

      {interactive && (
        <button
          type="button"
          className="voice-orb__mic-button"
          style={{
            width: config.coreRadius * 2,
            height: config.coreRadius * 2,
            top: center - config.coreRadius,
            left: center - config.coreRadius,
          }}
          onClick={onActivate}
          aria-label={ariaLabel}
        >
          <svg
            width={config.coreRadius * 2}
            height={config.coreRadius * 2}
            viewBox={`${center - config.coreRadius} ${center - config.coreRadius} ${config.coreRadius * 2} ${config.coreRadius * 2}`}
            fill="none"
            aria-hidden="true"
          >
            <circle className="voice-orb__core" cx={center} cy={center} r={config.coreRadius} />
            <path
              className="voice-orb__mic"
              d={config.micPath}
              strokeWidth={config.micStrokeWidth}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      )}
    </div>
  )
}

export default VoiceOrb
