from fastapi import FastAPI, UploadFile, File, HTTPException
from huggingface_hub import snapshot_download
import os
import tensorflow as tf
from tensorflow.keras.models import load_model
from tensorflow.keras.applications.efficientnet_v2 import preprocess_input
from tensorflow.keras.utils import img_to_array
from PIL import Image
import numpy as np
import io
import traceback  # ← добавили

app = FastAPI()

MODEL_DIR = "./skin-model"
MODEL_FILE = os.path.join(MODEL_DIR, "skin_model.keras")
CLASSES = ['Acne', 'Carcinoma', 'Eczema', 'Keratosis', 'Milia', 'Rosacea']
model = None

def load_skin_model():
    global model
    if not os.path.exists(MODEL_FILE):
        print("Downloading model from Hugging Face...")
        snapshot_download(repo_id="Tanishq77/skin-condition-classifier", local_dir=MODEL_DIR)
    
    if not os.path.exists(MODEL_FILE):
        raise RuntimeError("Model file not found after download.")
    
    print("Loading model...")
    model = load_model(MODEL_FILE)
    print("Model loaded successfully!")

# Загружаем модель при запуске
load_skin_model()

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    try:
        file_content = await file.read()
        if not file_content:
            raise ValueError("Empty file received")

        # Отладка: тип и размер
        print(f"📄 Received file: {file.filename}, size: {len(file_content)} bytes")

        # Открытие и конвертация
        img = Image.open(io.BytesIO(file_content)).convert('RGB')
        print(f"🖼️  Image mode: {img.mode}, size: {img.size}")

        img = img.resize((224, 224))
        img_array = img_to_array(img)
        print(f"📊 Array shape before expand: {img_array.shape}")

        img_array = np.expand_dims(img_array, axis=0)
        img_array = preprocess_input(img_array)
        print(f"📤 Input shape for model: {img_array.shape}")

        predictions = model.predict(img_array, verbose=0)
        predicted_class = CLASSES[np.argmax(predictions)]
        confidence = np.max(predictions) * 100
        probabilities = {cls: float(prob * 100) for cls, prob in zip(CLASSES, predictions[0])}

        return {
            "condition": predicted_class,
            "confidence": float(confidence),
            "probabilities": probabilities
        }

    except Exception as e:
        # 🔥 КРИТИЧЕСКИ ВАЖНО: вывести ошибку в лог
        print("💥 EXCEPTION in disease-analysis:")
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=f"Processing failed: {str(e)}")