CREATE TABLE if not exists chats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE if not exists llm_request_history (
    id BIGSERIAL PRIMARY KEY,
    request_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    llm_response TEXT NOT NULL,
    image_url VARCHAR(255),
    chat_id BIGINT NOT NULL,
    CONSTRAINT fk_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
);

-- Тестовые данные (опционально)
INSERT INTO chats (user_id, title, created_at)
VALUES (1, 'Диагностика кожи', CURRENT_TIMESTAMP);

INSERT INTO llm_request_history (request_time, llm_response, image_url, chat_id)
VALUES (
    CURRENT_TIMESTAMP,
    'Анализ изображения завершен: кожа в норме',
    'http://example.com/image.jpg',
    (SELECT id FROM chats WHERE title = 'Диагностика кожи')
);