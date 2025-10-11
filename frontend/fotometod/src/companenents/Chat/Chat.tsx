'use client'
import { useState } from 'react'
import ChatMessages from '@/companenents/ChatMessages/ChatMessages'
import ChatInput from './ChatInput/ChatInput'
import { MessageType } from '@/companenents/Message/Message'
import styles from './Chat.module.css'

interface ChatProps {
  initialMessages?: MessageType[]
  onSendMessage?: (text: string, image?: File) => void
}

export default function Chat({ initialMessages = [], onSendMessage }: ChatProps) {
  const [messages, setMessages] = useState<MessageType[]>(initialMessages)

  const handleSendMessage = (text: string, image?: File) => {
    const newMessage: MessageType = {
      id: Date.now().toString(),
      text,
      isOwn: true,
      timestamp: new Date(),
      image: image ? URL.createObjectURL(image) : undefined
    }

    setMessages(prev => [...prev, newMessage])
    
    if (onSendMessage) {
      onSendMessage(text, image)
    }
  }

  return (
    <div className={styles.chatContainer}>
      <ChatMessages messages={messages} />
      <ChatInput onSendMessage={handleSendMessage} />
    </div>
  )
}