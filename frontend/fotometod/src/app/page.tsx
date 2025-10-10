'use client'
import Image from "next/image";
import styles from "./page.module.css";
import Header from "@/companenents/Header/Header";
import { useState } from "react";
import Registration from "@/companenents/Registration/Registration";
import Auntification from "@/companenents/Auntification/Auntification";
import Chat from "@/companenents/Chat/Chat";

export default function Page() {
  const [registration, setRegistration] = useState<number>(0)
  
  const initialMessages: [] = [
    {
      id: '1',
      text: 'Привет! Как дела?',
      isOwn: false,
      timestamp: new Date(Date.now() - 3600000)
    },
    {
      id: '2',
      text: 'Привет! Все отлично, спасибо!',
      isOwn: true,
      timestamp: new Date(Date.now() - 3500000)
    }
  ]

  const handleSendMessage = (text: string, image?: File) => {
    // Здесь будет логика отправки на сервер
    console.log('Отправка сообщения:', text, image)
    
    // В реальном приложении здесь будет:
    // 1. Отправка на сервер
    // 2. Обновление сообщений через WebSocket или polling
  }

  return (
    <>
      <Header setRegistration={setRegistration}/>
      <main className={styles.main}>
        <Chat 
        initialMessages={initialMessages}
        onSendMessage={handleSendMessage}
      />
      {registration === 1 &&
      <Registration registration={registration} setRegistration={setRegistration} />
      }
            {registration === 2 &&
      <Auntification registration={registration} setRegistration={setRegistration} />
      }
      </main>
      </>
  );
}
