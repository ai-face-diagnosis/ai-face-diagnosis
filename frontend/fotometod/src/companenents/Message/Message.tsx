'use client'
import styles from './Message.module.css'

export interface MessageType {
  id: string
  text: string
  isOwn: boolean
  timestamp: Date
  image?: string
}

interface MessageProps {
  message: MessageType
}

export default function Message({ message }: MessageProps) {
  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('ru-RU', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  }

  return (
    <div
      className={`${styles.message} ${
        message.isOwn ? styles.messageOwn : styles.messageOther
      }`}
    >
      <p className={styles.messageText}>{message.text}</p>
      {message.image && (
        <img
          src={message.image}
          alt="Attached"
          className={styles.messageImage}
        />
      )}
      <div className={styles.messageTime}>
        {formatTime(message.timestamp)}
      </div>
    </div>
  )
}