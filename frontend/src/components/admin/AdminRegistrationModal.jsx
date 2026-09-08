import { useEffect, useRef, useState } from 'react'

export default function AdminRegistrationModal({ kind, customers, managers = [], onClose, onSubmit }) {
  const isManager = kind === 'manager'
  const dialog = useRef(null)
  const [errors, setErrors] = useState({})
  useEffect(() => {
    const previous = document.activeElement
    const element = dialog.current
    element.showModal()
    element.querySelector('input')?.focus()
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { element.close(); document.body.style.overflow = overflow; previous?.focus() }
  }, [])
  const submit = (event) => {
    event.preventDefault()
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    values.name = values.name.trim()
    values.email = values.email.trim().toLowerCase()
    const nextErrors = {}
    if (!values.name) nextErrors.name = '이름을 입력해주세요.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) nextErrors.email = '올바른 이메일을 입력해주세요.'
    if (isManager && managers.some((row) => row.email.toLowerCase() === values.email)) nextErrors.email = '이미 등록된 담당자 이메일입니다.'
    if (!isManager && customers.some((row) => row.name.toLowerCase() === values.name.toLowerCase())) nextErrors.name = '이미 등록된 고객사입니다.'
    if (isManager && !customers.some((row) => row.id === values.customerId)) nextErrors.customerId = '소속 고객사를 선택해주세요.'
    if (!isManager && (!values.contractStart || !form.elements.contractStart.validity.valid)) nextErrors.contractStart = '올바른 계약 시작일을 선택해주세요.'
    if (!['ACTIVE', 'INACTIVE'].includes(values.status)) nextErrors.status = '상태를 선택해주세요.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) { form.elements[Object.keys(nextErrors)[0]]?.focus(); return }
    onSubmit(values)
    onClose()
  }
  const field = (name, label, control) => <label className="admin-field"><span>{label}</span>{control}{errors[name] && <span id={`admin-error-${name}`} className="admin-field-error" role="alert">{errors[name]}</span>}</label>
  const props = (name) => ({ name, required: true, 'aria-invalid': !!errors[name], 'aria-describedby': errors[name] ? `admin-error-${name}` : undefined })
  return <dialog ref={dialog} className="admin-modal" aria-labelledby="admin-modal-title" onCancel={(event) => { event.preventDefault(); onClose() }} onClick={(event) => { if (event.target === dialog.current) { const rect = dialog.current.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose() } }}>
    <form noValidate onSubmit={submit}>
      <header><h2 id="admin-modal-title">{isManager ? '고객사 담당자 등록' : '고객사 등록'}</h2><button type="button" className="admin-modal-close" aria-label="모달 닫기" onClick={onClose}>✕</button></header>
      <div className="admin-fields">
        {field('name', isManager ? '이름' : '고객사/기관명', <input {...props('name')} maxLength={100} placeholder={isManager ? '예: 홍길동' : '예: ABC 테크놀로지'} />)}
        {field('email', isManager ? '이메일' : '담당자 이메일', <input {...props('email')} type="email" maxLength={254} placeholder={isManager ? 'you@company.com' : 'admin@company.com'} />)}
        {isManager ? field('customerId', '소속 고객사', <select {...props('customerId')} defaultValue=""><option value="" disabled>고객사를 선택해주세요</option>{customers.map((row) => <option key={row.id} value={row.id}>{row.name}</option>)}</select>) : field('contractStart', '계약 시작일', <input {...props('contractStart')} type="date" min="1900-01-01" max="9999-12-31" />)}
        {field('status', isManager ? '계정 상태' : '서비스 상태', <select {...props('status')} defaultValue="ACTIVE"><option value="ACTIVE">활성</option><option value="INACTIVE">비활성</option></select>)}
      </div>
      <footer><button type="button" className="admin-button" onClick={onClose}>취소</button><button type="submit" className="admin-button admin-primary">{isManager ? '담당자 등록' : '고객사 등록'}</button></footer>
    </form>
  </dialog>
}
