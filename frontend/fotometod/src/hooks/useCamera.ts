// hooks/useCamera.ts
'use client'; // Добавляем эту директиву

import { useState, useRef, useCallback } from 'react'

interface UseCameraReturn {
  takePhoto: () => Promise<File | null>
  isTakingPhoto: boolean
  error: string | null
}

export function useCamera(): UseCameraReturn {
  const [isTakingPhoto, setIsTakingPhoto] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)

  const takePhoto = useCallback(async (): Promise<File | null> => {
    // Проверяем, доступна ли камера
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      console.warn('Камера не доступна в этом окружении')
      return null
    }

    setIsTakingPhoto(true)
    setError(null)
    
    let stream: MediaStream | null = null

    try {
      // Запускаем камеру с обработкой ошибок
      stream = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: 'user' } 
      }).catch(err => {
        console.warn('Не удалось получить доступ к камере:', err)
        return null
      })

      // Если камера недоступна, просто возвращаем null
      if (!stream) {
        return null
      }

      // Создаем временный видео элемент
      const video = document.createElement('video')
      videoRef.current = video
      video.srcObject = stream
      video.playsInline = true
      
      // Ждем готовности видео
      await new Promise((resolve, reject) => {
        video.onloadedmetadata = () => resolve(true)
        video.onerror = reject
        video.play().catch(reject)
      })

      // Создаем canvas для съемки фото
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      
      const context = canvas.getContext('2d')
      if (!context) {
        throw new Error('Не удалось получить контекст canvas')
      }

      // Делаем снимок
      context.drawImage(video, 0, 0, canvas.width, canvas.height)

      // Конвертируем в blob и затем в File
      return new Promise((resolve) => {
        canvas.toBlob(async (blob) => {
          if (blob) {
            // Создаем File из blob
            const file = new File([blob], `photo-${Date.now()}.jpg`, { 
              type: 'image/jpeg' 
            })
            resolve(file)
          } else {
            resolve(null)
          }
        }, 'image/jpeg', 0.8)
      })

    } catch (err) {
      console.warn('Ошибка при съемке фото:', err)
      return null
    } finally {
      // Останавливаем камеру
      if (stream) {
        stream.getTracks().forEach(track => track.stop())
      }
      setIsTakingPhoto(false)
    }
  }, [])

  return {
    takePhoto,
    isTakingPhoto,
    error
  }
}