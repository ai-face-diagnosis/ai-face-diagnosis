// hooks/useChats.ts
'use client'
import { useState } from 'react'
import { ChatType, ChatHistory } from '@/types/chat'

export function useChats() {
  const [isLoading, setIsLoading] = useState(false)

  // Запрос к бэкенду для получения чатов пользователя
  const loadUserChats = async (userId: string): Promise<ChatType[]> => {
    setIsLoading(true)
    try {
      const response = await fetch(`/api/chats?userId=${userId}`)
      
      // Если ошибка - возвращаем пустой массив вместо выброса ошибки
      if (!response.ok) {
        console.log('Не удалось загрузить чаты, возвращаем пустой массив')
        return []
      }
      
      const chats = await response.json()
      return chats
    } catch (error) {
      console.log('Ошибка загрузки чатов, пользователь возможно не авторизован:', error)
      // При любой ошибке возвращаем пустой массив
      return []
    } finally {
      setIsLoading(false)
    }
  }

  // Создание нового чата на бэкенде с возможностью указать название
  const createNewChat = async (userId: string, chatName?: string): Promise<ChatType> => {
    setIsLoading(true)
    try {
      const name = chatName || 'Новый чат' // Используем переданное название или "Новый чат"
      
      const response = await fetch('/api/chats', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId,
          name: name // Используем переданное название
        })
      })
      
      if (!response.ok) {
        // Если не удалось создать на бэкенде, создаем локально
        console.log('Не удалось создать чат на бэкенде, создаем локально')
        throw new Error('Failed to create chat')
      }
      
      const newChat = await response.json()
      return newChat
    } catch (error) {
      console.log('Создаем локальный чат как fallback')
      // Создаем локальный чат как fallback
      const fallbackChat: ChatType = {
        id: `local-${Date.now()}`,
        name: chatName || 'Новый чат', // Используем переданное название или "Новый чат"
        userId: userId,
        createdAt: new Date().toISOString(),
        isEmpty: true
      }
      return fallbackChat
    } finally {
      setIsLoading(false)
    }
  }

  // Загрузка истории чата с бэкенда
  const loadChatHistory = async (chatId: string): Promise<ChatHistory> => {
    setIsLoading(true)
    try {
      const response = await fetch(`/api/chats/${chatId}/history`)
      
      // Если ошибка - возвращаем пустую историю
      if (!response.ok) {
        console.log('Не удалось загрузить историю чата, возвращаем пустую')
        return {
          chatId,
          messages: []
        }
      }
      
      const history = await response.json()
      return history
    } catch (error) {
      console.log('Ошибка загрузки истории чата:', error)
      // При ошибке возвращаем пустую историю
      return {
        chatId,
        messages: []
      }
    } finally {
      setIsLoading(false)
    }
  }

  return {
    isLoading,
    loadUserChats,
    createNewChat,
    loadChatHistory
  }
}