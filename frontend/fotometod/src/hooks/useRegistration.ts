import { useState, useCallback } from 'react';

interface RegistrationData {
  username: string;
  email: string;
  password: string;
}

interface RegistrationResponse {
  success: boolean;
  error?: string;
  fieldErrors?: {
    username?: string;
    email?: string;
    password?: string;
  };
  token?: string; // JWT после логина
}

export const useRegistration = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const register = async (data: RegistrationData): Promise<RegistrationResponse> => {
    setIsLoading(true);
    setError(null);
    setSuccess(false);

    try {
      // 1️⃣ Отправка регистрации в Orchestration
      const response = await fetch('http://localhost:8084/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: data.username,
          password: data.password,
          email: data.email,
        }),
        credentials: 'include', // если нужна cookie-based auth
      });

      const responseData = await response.json();

      if (response.ok) {
        setSuccess(true);

        // 2️⃣ Опционально: сразу логиним пользователя и получаем JWT
        const loginResponse = await fetch('http://localhost:8084/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: data.username,
            password: data.password,
          }),
        });

        const loginData = await loginResponse.json();
        if (loginResponse.ok && loginData.token) {
          localStorage.setItem('token', loginData.token); // сохраняем JWT
          return { success: true, token: loginData.token };
        }

        return { success: true };
      } else {
        // обработка ошибок
        let errorMessage = 'Произошла ошибка при регистрации';
        const fieldErrors: { [key: string]: string } = {};

        if (response.status === 409) {
          errorMessage = 'Имя пользователя уже существует';
          fieldErrors.username = errorMessage;
        } else if (responseData.message) {
          errorMessage = responseData.message;
        }

        setError(errorMessage);
        return { success: false, error: errorMessage, fieldErrors };
      }
    } catch (err) {
      const errorMessage = 'Не удалось подключиться к серверу';
      setError(errorMessage);
      return { success: false, error: errorMessage, fieldErrors: {} };
    } finally {
      setIsLoading(false);
    }
  };

  const reset = useCallback(() => {
    setError(null);
    setSuccess(false);
  }, []);

  return {
    register,
    isLoading,
    error,
    success,
    reset,
  };
};
