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
import traceback

app = FastAPI()

MODEL_DIR = "./skin-model"
MODEL_FILE = os.path.join(MODEL_DIR, "skin_model.keras")
CLASSES = ['Acne', 'Carcinoma', 'Eczema', 'Keratosis', 'Milia', 'Rosacea']
model = None

def load_skin_model():
    global model
    if not os.path.exists(MODEL_FILE):
        snapshot_download(repo_id="Tanishq77/skin-condition-classifier", local_dir=MODEL_DIR)
    
    if not os.path.exists(MODEL_FILE):
        raise RuntimeError("Model file not found after download.")
    
    model = load_model(MODEL_FILE)

# Загружаем модель при запуске
load_skin_model()

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    try:
        file_content = await file.read()
        if not file_content:
            raise ValueError("Empty file received")

        img = Image.open(io.BytesIO(file_content)).convert('RGB')
        img = img.resize((224, 224))
        img_array = img_to_array(img)
        img_array = np.expand_dims(img_array, axis=0)
        img_array = preprocess_input(img_array)

        predictions = model.predict(img_array, verbose=0)
        max_prob = np.max(predictions)
        predicted_class = CLASSES[np.argmax(predictions)]
        confidence = max_prob * 100
        probabilities = {cls: float(prob * 100) for cls, prob in zip(CLASSES, predictions[0])}

        # Если ни одно заболевание не превышает 50% уверенности — считаем кожу здоровой
        if max_prob < 0.5:
            return {
                "condition": "Healthy skin",
                "confidence": float((1 - max_prob) * 100),  # Уверенность в здоровье = 100% - max(болезнь)
                "probabilities": probabilities
            }
        else:
            return {
                "condition": predicted_class,
                "confidence": float(confidence),
                "probabilities": probabilities
            }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Processing failed: {str(e)}")