'use client'
import { useActionState, useEffect, useRef } from 'react'
import { maxLength, minLength, pipe, string, trim } from 'valibot'
import styles from './Auntification.module.css'

const passwordSchema = pipe(
  string(),
  trim(),
  minLength(6, 'Минимальная длина пароля - 6 символов'),
  maxLength(30, 'Максимальная длина пароля - 30 символов')
)

const loginSchema = pipe(
  string(),
  trim(),
  minLength(5, 'Минимальная длина логина - 5 символов'),
  maxLength(20, 'Максимальная длина логина - 20 символов')
)

interface Auntification {
  registration: number;
  setRegistration: (registration: number) => void;
}

async function auntificationAction(prevState: any, formData: FormData) {
  const login = formData.get('login') as string
  const password = formData.get('password') as string
  
  // Имитация проверки неверных данных
  if (login === 'wronguser' || password === 'wrongpass') {
    return { 
      success: false, 
      error: 'Неверный логин или пароль',
      fieldErrors: { login: 'Неверный логин или пароль' }
    }
  }
  
  // Имитация успешной аутентификации
  return { success: true, error: null, fieldErrors: {} }
}

export default function Auntification({ registration, setRegistration }: Auntification) {
  const [state, action, isPending] = useActionState(auntificationAction, {
    success: false,
    error: null,
    fieldErrors: {}
  })

  const modalRef = useRef<HTMLDivElement>(null)

  // Закрытие по ESC
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && registration !== 0) {
        setRegistration(0)
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [registration, setRegistration])

  // Закрытие по клику вне модалки
  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      setRegistration(0)
    }
  }

  const handleRegistrationClick = () => {
    setRegistration(1) // Переключаем на форму регистрации
  }

  return (
    <section 
      className={registration === 2 ? styles.modalVisible : styles.modalUnvisible}
      onClick={handleOverlayClick}
      ref={modalRef}
    >
      <div className={styles.modalContent}>
        <form action={action} className={styles.auntificationForm}>
          <div className={styles.formHeader}>
            <h2>Вход в аккаунт</h2>
            <button 
              type="button" 
              className={styles.closeButton}
              onClick={() => setRegistration(0)}
              aria-label="Закрыть аутентификацию"
            >
              ×
            </button>
          </div>

          <div className={styles.inputGroup}>
            <label htmlFor="login">Логин</label>
            <input
              type="text"
              id="login"
              name="login"
              placeholder="Введите логин"
              required
              minLength={5}
              maxLength={20}
            />
            {state.fieldErrors?.login && (
              <span className={styles.errorText}>{state.fieldErrors.login}</span>
            )}
          </div>

          <div className={styles.inputGroup}>
            <label htmlFor="password">Пароль</label>
            <input
              type="password"
              id="password"
              name="password"
              placeholder="Введите пароль"
              required
              minLength={6}
              maxLength={30}
            />
          </div>

          {state.error && !state.fieldErrors?.login && (
            <div className={styles.formError}>{state.error}</div>
          )}

          <button 
            type="submit" 
            className={styles.submitButton}
            disabled={isPending}
          >
            {isPending ? 'Вход...' : 'Войти'}
          </button>

          <div className={styles.registrationSwitch}>
            <p className={styles.p}>Нет аккаунта?</p>
            <button 
              type="button" 
              className={styles.registrationButton}
              onClick={handleRegistrationClick}
            >
              Зарегистрироваться
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}