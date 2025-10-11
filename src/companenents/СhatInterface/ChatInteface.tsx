// components/ChatInterface.tsx
'use client'
import { ChatHistory } from '@/types/chat'
import ChatMessages from '@/companenents/ChatMessages/ChatMessages'
import styles from './ChatInteface.module.css'

interface ChatInterfaceProps {
  currentChat: ChatHistory | null
  isLoading: boolean
  onCreateNewChat: () => void
}

export default function ChatInterface({ 
  currentChat, 
  isLoading,
  onCreateNewChat 
}: ChatInterfaceProps) {
  if (!currentChat) {
    return (
      <div className={styles.emptyState}>
        <div className={styles.emptyContent}>
          <h3>Нет активного чата</h3>
          <p>Выберите существующий чат или создайте новый</p>
          <button 
            className={styles.createButton}
            onClick={onCreateNewChat}
            disabled={isLoading}
          >
            {isLoading ? 'Создание...' : 'Создать новый чат'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.chatInterface}>
      <ChatMessages 
        history={currentChat}
        isLoading={isLoading}
      />
    </div>
  )
}