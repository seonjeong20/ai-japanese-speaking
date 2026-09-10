import { useEffect, useRef, useState } from 'react'

// 기관(Organization) 등록/수정 전용 모달입니다. Manager 계정은 본인이 직접 가입해서
// PENDING → Admin 승인 흐름으로만 생성되므로, Admin이 대신 등록하는 기능은 없습니다.
// mode: 'create' | 'edit' (기관명 수정 시 사용).
export default function AdminRegistrationModal({ mode = 'create', initialValues, customers, onClose, onSubmit }) {
  const isEdit = mode === 'edit'
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
    const nextErrors = {}
    if (!values.name) nextErrors.name = '이름을 입력해주세요.'
    else if (customers.some((row) => row.name.toLowerCase() === values.name.toLowerCase() && row.id !== initialValues?.id)) {
      nextErrors.name = '이미 등록된 기관입니다.'
    }
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) { form.elements[Object.keys(nextErrors)[0]]?.focus(); return }
    onSubmit(values)
    onClose()
  }
  const field = (name, label, control) => <label className="admin-field"><span>{label}</span>{control}{errors[name] && <span id={`admin-error-${name}`} className="admin-field-error" role="alert">{errors[name]}</span>}</label>
  const props = (name) => ({ name, required: true, 'aria-invalid': !!errors[name], 'aria-describedby': errors[name] ? `admin-error-${name}` : undefined })
  const modalTitle = isEdit ? '기관명 수정' : '기관 등록'
  const submitLabel = isEdit ? '저장' : '기관 등록'
  return <dialog ref={dialog} className="admin-modal" aria-labelledby="admin-modal-title" onCancel={(event) => { event.preventDefault(); onClose() }} onClick={(event) => { if (event.target === dialog.current) { const rect = dialog.current.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose() } }}>
    <form noValidate onSubmit={submit}>
      <header><h2 id="admin-modal-title">{modalTitle}</h2><button type="button" className="admin-modal-close" aria-label="모달 닫기" onClick={onClose}>✕</button></header>
      <div className="admin-fields">
        {field('name', '기관명', <input {...props('name')} maxLength={100} defaultValue={initialValues?.name ?? ''} placeholder="예: ABC 어학원" />)}
      </div>
      <footer><button type="button" className="admin-button" onClick={onClose}>취소</button><button type="submit" className="admin-button admin-primary">{submitLabel}</button></footer>
    </form>
  </dialog>
}
