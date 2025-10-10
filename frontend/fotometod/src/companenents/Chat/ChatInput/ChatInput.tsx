'use client'
import { useState, useRef, useEffect } from 'react'
import styles from './ChatInput.module.css'

interface ChatInputProps {
  onSendMessage: (text: string, image?: File) => void
}

export default function ChatInput({ onSendMessage }: ChatInputProps) {
  const [inputText, setInputText] = useState('')
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string>('')
  const fileInputRef = useRef<HTMLInputElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`
    }
  }, [inputText])

  const handleSendMessage = () => {
    if (inputText.trim() || selectedImage) {
      onSendMessage(inputText.trim(), selectedImage || undefined)
      setInputText('')
      setSelectedImage(null)
      setImagePreview('')
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  const handleAttachClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file && file.type.startsWith('image/')) {
      setSelectedImage(file)
      const reader = new FileReader()
      reader.onload = (e) => {
        setImagePreview(e.target?.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleRemoveImage = () => {
    setSelectedImage(null)
    setImagePreview('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <>
    {imagePreview && (
        <div className={styles.imagePreview}>
          <img
            src={imagePreview}
            alt="Preview"
          />
          <button
            className={styles.removeImage}
            onClick={handleRemoveImage}
            aria-label="Удалить изображение"
          >
            ×
          </button>
        </div>
      )}
      <div className={styles.chatInputContainer}>
        <input
          type="file"
          ref={fileInputRef}
          className={styles.fileInput}
          accept="image/*"
          onChange={handleFileSelect}
        />
        
        <button
          className={styles.attachButton}
          onClick={handleAttachClick}
          aria-label="Прикрепить изображение"
        >
          <svg viewBox="0 0 24 24">
            <path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/>
          </svg>
        </button>

        <textarea
          ref={textareaRef}
          className={styles.chatInput}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Введите сообщение..."
          rows={1}
        />

        <button
          className={styles.sendButton}
          onClick={handleSendMessage}
          disabled={!inputText.trim() && !selectedImage}
        >
          Отправить
        </button>
      </div>
    </>
  )
}