// types/chat.ts
export interface ChatType {
  id: string
  name: string
  userId: string
  createdAt: string
  isEmpty: boolean
}

export interface ChatHistory {
  chatId: string
  messages: Message[]
}

export interface Message {
  image?: string
  audio?: string
  id: string
  text: string
  timestamp: string
  isUser: boolean
}