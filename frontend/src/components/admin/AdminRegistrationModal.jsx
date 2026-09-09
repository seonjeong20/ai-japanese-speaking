import { useEffect, useRef, useState } from 'react'

// kind: 'manager' | 'customer'(기관). mode: 'create' | 'edit' (기관명 수정 시 사용).
export default function AdminRegistrationModal({ kind, mode = 'create', initialValues, customers, managers = [], onClose, onSubmit }) {
  const isManager = kind === 'manager'
  const isEdit = mode === 'edit'
  const activeCustomers = customers.filter((row) => row.status === 'ACTIVE')
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
    if (isManager) values.email = values.email.trim().toLowerCase()
    const nextErrors = {}
    if (!values.name) nextErrors.name = '이름을 입력해주세요.'
    if (isManager) {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) nextErrors.email = '올바른 이메일을 입력해주세요.'
      if (managers.some((row) => row.email.toLowerCase() === values.email)) nextErrors.email = '이미 등록된 담당자 이메일입니다.'
      if (!activeCustomers.some((row) => row.id === values.customerId)) nextErrors.customerId = '소속 기관을 선택해주세요.'
      if (!['ACTIVE', 'INACTIVE'].includes(values.status)) nextErrors.status = '상태를 선택해주세요.'
    } else if (
      customers.some((row) => row.name.toLowerCase() === values.name.toLowerCase() && row.id !== initialValues?.id)
    ) {
      nextErrors.name = '이미 등록된 기관입니다.'
    }
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) { form.elements[Object.keys(nextErrors)[0]]?.focus(); return }
    onSubmit(values)
    onClose()
  }
  const field = (name, label, control) => <label className="admin-field"><span>{label}</span>{control}{errors[name] && <span id={`admin-error-${name}`} className="admin-field-error" role="alert">{errors[name]}</span>}</label>
  const props = (name) => ({ name, required: true, 'aria-invalid': !!errors[name], 'aria-describedby': errors[name] ? `admin-error-${name}` : undefined })
  const modalTitle = isManager ? '담당자 등록' : isEdit ? '기관명 수정' : '기관 등록'
  const submitLabel = isManager ? '담당자 등록' : isEdit ? '저장' : '기관 등록'
  return <dialog ref={dialog} className="admin-modal" aria-labelledby="admin-modal-title" onCancel={(event) => { event.preventDefault(); onClose() }} onClick={(event) => { if (event.target === dialog.current) { const rect = dialog.current.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose() } }}>
    <form noValidate onSubmit={submit}>
      <header><h2 id="admin-modal-title">{modalTitle}</h2><button type="button" className="admin-modal-close" aria-label="모달 닫기" onClick={onClose}>✕</button></header>
      <div className="admin-fields">
        {field('name', isManager ? '이름' : '기관명', <input {...props('name')} maxLength={100} defaultValue={initialValues?.name ?? ''} placeholder={isManager ? '예: 홍길동' : '예: ABC 어학원'} />)}
        {isManager && field('email', '이메일', <input {...props('email')} type="email" maxLength={254} placeholder="you@company.com" />)}
        {isManager && field('customerId', '소속 기관', <select {...props('customerId')} defaultValue=""><option value="" disabled>기관을 선택해주세요</option>{activeCustomers.map((row) => <option key={row.id} value={row.id}>{row.name}</option>)}</select>)}
        {isManager && field('department', '부서 (선택)', <input name="department" maxLength={100} placeholder="예: 교육운영팀" />)}
        {isManager && field('status', '계정 상태', <select {...props('status')} defaultValue="ACTIVE"><option value="ACTIVE">활성</option><option value="INACTIVE">비활성</option></select>)}
      </div>
      <footer><button type="button" className="admin-button" onClick={onClose}>취소</button><button type="submit" className="admin-button admin-primary">{submitLabel}</button></footer>
    </form>
  </dialog>
}
