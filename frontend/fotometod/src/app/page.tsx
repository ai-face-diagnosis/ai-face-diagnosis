'use client'
import Image from "next/image";
import styles from "./page.module.css";
import Header from "@/companenents/Header/Header";
import { useEffect, useState } from "react";
import Registration from "@/companenents/Registration/Registration";
import Auntification from "@/companenents/Auntification/Auntification";
import ChatSelector from '@/companenents/ChatSelector/ChatSelector'
import { useChats } from '@/hooks/useChats'
import { ChatType, ChatHistory } from '@/types/chat'
import { MessageType } from '@/companenents/Message/Message'
import Chat from "@/companenents/Chat/Chat";

export default function Page() {
  const [registration, setRegistration] = useState<number>(0)
  const [userId] = useState('user-123')
  const [chats, setChats] = useState<ChatType[]>([])
  const [currentChat, setCurrentChat] = useState<ChatHistory | null>(null)
  const [isChatSelectorOpen, setIsChatSelectorOpen] = useState(false)
  const [isInitializing, setIsInitializing] = useState(true)
  const [chatMessages, setChatMessages] = useState<MessageType[]>([])
  const [chatKey, setChatKey] = useState<string>('') // ключ для пересоздания чата
  const [accentColor, setAccentColor] = useState('#059669')
  const { isLoading, loadUserChats, createNewChat, loadChatHistory } = useChats()

  const convertToMessageType = (history: ChatHistory): MessageType[] => {
    return history.messages.map(message => ({
      id: message.id,
      text: message.text,
      isOwn: message.isUser,
      timestamp: new Date(message.timestamp),
      image: message.image,
      audio: message.audio
    }))
  }

  useEffect(() => {
    const initializeChats = async () => {
      try {
        const userChats = await loadUserChats(userId)
        setChats(userChats)
        
        if (userChats.length > 0) {
          const firstChatHistory = await loadChatHistory(userChats[0].id)
          setCurrentChat(firstChatHistory)
          setChatMessages(convertToMessageType(firstChatHistory))
          setChatKey(firstChatHistory.chatId) // устанавливаем ключ
        } else {
          const newChat = await createNewChat(userId)
          setChats([newChat])
          const emptyHistory: ChatHistory = {
            chatId: newChat.id,
            messages: []
          }
          setCurrentChat(emptyHistory)
          setChatMessages([])
          setChatKey(newChat.id) // устанавливаем ключ
        }
      } catch (error) {
        console.log('Ошибка инициализации чатов, создаем локальный чат')
        const localChat: ChatType = {
          id: `local-${Date.now()}`,
          name: 'Мой чат',
          userId: userId,
          createdAt: new Date().toISOString(),
          isEmpty: true
        }
        setChats([localChat])
        const emptyHistory: ChatHistory = {
          chatId: localChat.id,
          messages: []
        }
        setCurrentChat(emptyHistory)
        setChatMessages([])
        setChatKey(localChat.id) // устанавливаем ключ
      } finally {
        setIsInitializing(false)
      }
    }

    initializeChats()
  }, [userId])

  // Обработчик выбора чата из селектора
  const handleChatSelect = async (chatId: string) => {
    try {
      const history = await loadChatHistory(chatId)
      setCurrentChat(history)
      setChatMessages(convertToMessageType(history))
      setChatKey(chatId) // обновляем ключ при смене чата
      setIsChatSelectorOpen(false)
    } catch (error) {
      console.log('Ошибка выбора чата:', error)
      const emptyHistory: ChatHistory = {
        chatId,
        messages: []
      }
      setCurrentChat(emptyHistory)
      setChatMessages([])
      setChatKey(chatId) // обновляем ключ
      setIsChatSelectorOpen(false)
    }
  }

  // Создание нового чата
const handleCreateNewChat = async (chatName?: string) => {
  try {
    const newChat = await createNewChat(userId, chatName) // передаем название чата
    setChats(prev => [newChat, ...prev])
    
    const emptyHistory: ChatHistory = {
      chatId: newChat.id,
      messages: []
    }
    setCurrentChat(emptyHistory)
    setChatMessages([])
    setChatKey(newChat.id)
    setIsChatSelectorOpen(false)
  } catch (error) {
    console.log('Ошибка создания чата:', error)
  }
}

const handleSendMessage = (text: string, image?: File, audio?: Blob) => {
  console.log('Отправка сообщения в чат:', currentChat?.chatId, text, image, audio)
  
  let audioUrl: string | undefined
  if (audio) {
    audioUrl = URL.createObjectURL(audio)
  }

  const tempMessage: MessageType = {
    id: `temp-${Date.now()}`,
    text: audio ? '' : text, // не передаем текст если есть аудио
    isOwn: true,
    timestamp: new Date(),
    image: image ? URL.createObjectURL(image) : undefined,
    audio: audioUrl
  }

  setChatMessages(prev => [...prev, tempMessage])
  }

  const handleOpenChatSelector = () => {
    setIsChatSelectorOpen(true)
  }

  if (isInitializing) {
    return (
      <div className={styles.loadingContainer}>
        <p>Загрузка чатов...</p>
      </div>
    )
  }

  return (
    <>
      <Header setRegistration={setRegistration} onChatButtonClick={handleOpenChatSelector} accentColor = {accentColor} setAccentColor = {setAccentColor} />
      <main className={styles.main}>
        <Chat 
          key={chatKey}
          initialMessages={chatMessages}
          onSendMessage={handleSendMessage}
          accentColor = {accentColor}
        />
        
        {registration === 1 && (
          <Registration registration={registration} setRegistration={setRegistration} />
        )}
        {registration === 2 && (
          <Auntification registration={registration} setRegistration={setRegistration} />
        )}
      </main>

<ChatSelector
  isOpen={isChatSelectorOpen}
  onClose={() => setIsChatSelectorOpen(false)}
  chats={chats}
  onChatSelect={handleChatSelect}
  selectedChatId={currentChat?.chatId}
  onCreateNewChat={handleCreateNewChat} // теперь принимает chatName
  isLoading={isLoading}
/>
    </>
  );
}