// Registration.tsx
'use client'
import { useEffect, useRef, useState } from 'react'
import { maxLength, minLength, pipe, string, trim, email } from 'valibot'
import { useRegistration } from '@/hooks/useRegistration'

import styles from './Registration.module.css'

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

const emailSchema = pipe(
  string(),
  trim(),
  email('Введите корректный email адрес')
)

interface RegistrationProps {
  registration: number;
  setRegistration: (registration: number) => void;
}

export default function Registration({ registration, setRegistration }: RegistrationProps) {
  const { register, isLoading, error, success, reset } = useRegistration()
  const [fieldErrors, setFieldErrors] = useState<{ [key: string]: string }>({})
  const modalRef = useRef<HTMLDivElement>(null)

  // Сброс состояния при открытии модалки
  useEffect(() => {
    if (registration === 1) {
      reset()
      setFieldErrors({})
    }
  }, [registration]) // Убрали reset из зависимостей, так как он теперь стабилен

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

  const handleLoginClick = () => {
    setRegistration(2)
  }

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setFieldErrors({})
    
    const formData = new FormData(e.currentTarget)
    const login = formData.get('login') as string
    const email = formData.get('email') as string
    const password = formData.get('password') as string

    // Валидация на клиенте
    const errors: { [key: string]: string } = {}

    try {
      emailSchema.parse(email)
    } catch (error: any) {
      errors.email = 'Введите корректный email адрес'
    }

    try {
      loginSchema.parse(login)
    } catch (error: any) {
      errors.login = error.message
    }

    try {
      passwordSchema.parse(password)
    } catch (error: any) {
      errors.password = error.message
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    // Отправка на сервер
    const result = await register({ login, email, password })
    
    if (result.fieldErrors) {
      setFieldErrors(result.fieldErrors)
    }

    if (result.success) {
      setTimeout(() => {
        setRegistration(0)
      }, 2000)
    }
  }

  return (
    <section 
      className={registration === 1 ? styles.modalVisible : styles.modalUnvisible}
      onClick={handleOverlayClick}
      ref={modalRef}
    >
      <div className={styles.modalContent}>
        <form onSubmit={handleSubmit} className={styles.registrationForm}>
          <div className={styles.formHeader}>
            <h2>Регистрация</h2>
            <button 
              type="button" 
              className={styles.closeButton}
              onClick={() => setRegistration(0)}
              aria-label="Закрыть регистрацию"
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
            {fieldErrors.login && (
              <span className={styles.errorText}>{fieldErrors.login}</span>
            )}
          </div>

          <div className={styles.inputGroup}>
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="example@gmail.com"
              required
            />
            {fieldErrors.email && (
              <span className={styles.errorText}>{fieldErrors.email}</span>
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
            {fieldErrors.password && (
              <span className={styles.errorText}>{fieldErrors.password}</span>
            )}
          </div>

          {error && !fieldErrors.login && !fieldErrors.email && !fieldErrors.password && (
            <div className={styles.formError}>{error}</div>
          )}

          {success && (
            <div className={styles.successMessage}>
              Регистрация прошла успешно! Вы будете перенаправлены...
            </div>
          )}

          <button 
            type="submit" 
            className={styles.submitButton}
            disabled={isLoading}
          >
            {isLoading ? 'Регистрация...' : 'Зарегистрироваться'}
          </button>

          <div className={styles.loginSwitch}>
            <p className={styles.p}>Уже есть аккаунт?</p>
            <button 
              type="button" 
              className={styles.loginButton}
              onClick={handleLoginClick}
            >
              Войти в аккаунт
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}