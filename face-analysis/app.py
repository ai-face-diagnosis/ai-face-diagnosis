import cv2
import numpy as np
from deepface import DeepFace
import mediapipe as mp
from PIL import Image
import torch
from torchvision import models, transforms
from fastapi import FastAPI, UploadFile, File
from typing import Dict

app = FastAPI()

mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True)


skin_transform = transforms.Compose([
    transforms.Resize(224),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])


skin_model = models.densenet121(weights=models.DenseNet121_Weights.DEFAULT)
skin_model.eval()

def detect_face_and_crop(image: np.ndarray, region: str = 'face') -> np.ndarray:

    if image is None:
        raise ValueError("Изображение не загружено")
    
    rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(rgb_image)
    
    landmarks = results.multi_face_landmarks[0]
    h, w, _ = image.shape
    
    if region == 'face':
        x_min = min([lm.x for lm in landmarks.landmark]) * w
        y_min = min([lm.y for lm in landmarks.landmark]) * h
        x_max = max([lm.x for lm in landmarks.landmark]) * w
        y_max = max([lm.y for lm in landmarks.landmark]) * h
        crop = image[int(y_min):int(y_max), int(x_min):int(x_max)]
    
    elif region == 'eyes':
        left_eye = landmarks.landmark[33]  
        right_eye = landmarks.landmark[263]
        x_min = min(left_eye.x, right_eye.x) * w - 50
        y_min = min(left_eye.y, right_eye.y) * h - 50
        x_max = max(left_eye.x, right_eye.x) * w + 50
        y_max = max(left_eye.y, right_eye.y) * h + 50
        crop = image[int(y_min):int(y_max), int(x_min):int(x_max)]

    
    return crop

def analyze_emotions(image: np.ndarray) -> Dict:

      face_crop = detect_face_and_crop(image, 'face')  
      result = DeepFace.analyze(
          face_crop,
          actions=['emotion'],
      )

      emotions = result[0]['emotion']
      stress = emotions.get('sad', 0) + emotions.get('fear', 0)
      fatigue = emotions.get('neutral', 0)
      anxiety = emotions.get('fear', 0)

      return {
          'stress': stress / 100.0,
          'fatigue': fatigue / 100.0,
          'anxiety': anxiety / 100.0
      }


def analyze_eyes(image: np.ndarray) -> Dict:
    try:
        eye_crop = detect_face_and_crop(image, 'eyes')
        hsv = cv2.cvtColor(eye_crop, cv2.COLOR_BGR2HSV)
        
        red_lower = np.array([0, 70, 50])
        red_upper = np.array([10, 255, 255])
        red_mask = cv2.inRange(hsv, red_lower, red_upper)
        redness = np.sum(red_mask) / (eye_crop.shape[0] * eye_crop.shape[1])
        
        gray = cv2.cvtColor(eye_crop, cv2.COLOR_BGR2GRAY)
        dark_circles = 1 - (np.mean(gray) / 255) 
        
        yellow_lower = np.array([20, 100, 100])
        yellow_upper = np.array([30, 255, 255])
        yellow_mask = cv2.inRange(hsv, yellow_lower, yellow_upper)
        yellowness = np.sum(yellow_mask) / (eye_crop.shape[0] * eye_crop.shape[1])
        
        return {
            'redness': redness,
            'dark_circles': dark_circles,
            'yellowness': yellowness
        }
    except Exception as e:
        return {'error': str(e)}

def analyze_photo(image: np.ndarray) -> Dict:
    results = {
        'emotions': analyze_emotions(image),
        'eyes': analyze_eyes(image)
    }
    return results

# image = cv2.imread('mogger_2.jpg')
# results = analyze_photo(image)
# print(results)


@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    contents = await file.read()
    image = cv2.imdecode(np.frombuffer(contents, np.uint8), cv2.IMREAD_COLOR)
    results = analyze_photo(image)
    return results
