FROM python:3.12-slim

WORKDIR /app

# Устанавливаем зависимости
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Копируем код
COPY . .

EXPOSE 8083

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8083"]

