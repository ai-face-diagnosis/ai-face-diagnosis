import cv2
import numpy as np
from deepface import DeepFace
import mediapipe as mp
from PIL import Image
from tensorflow.keras.preprocessing import image
from torchvision import models, transforms
from fastapi import FastAPI, UploadFile, File
from typing import Dict
import tensorflow as tf

app = FastAPI()


skin_transform = transforms.Compose([
    transforms.Resize(224),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])


skin_model = models.densenet121(weights=models.DenseNet121_Weights.DEFAULT)
skin_model.eval()

eye_model = tf.keras.models.load_model('red_eye_model.keras')

mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True)
padding = 5

def detect_face_and_crop(image: np.ndarray, region: str = 'face') -> np.ndarray:
    
    rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(rgb_image)
    
    landmarks = results.multi_face_landmarks[0]
    h, w, _ = image.shape
    
    if region == 'face':
        x_min = min([lm.x for lm in landmarks.landmark]) * w
        y_min = min([lm.y for lm in landmarks.landmark]) * h
        x_max = max([lm.x for lm in landmarks.landmark]) * w
        y_max = max([lm.y for lm in landmarks.landmark]) * h
    
    elif region == 'left_eye':
        left_eye_indices = [33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246]
        left_eye_points = [landmarks.landmark[i] for i in left_eye_indices]
        
        x_min = min([lm.x for lm in left_eye_points]) * w - padding  
        y_min = min([lm.y for lm in left_eye_points]) * h - padding
        x_max = max([lm.x for lm in left_eye_points]) * w + padding
        y_max = max([lm.y for lm in left_eye_points]) * h + padding

    elif region == 'right_eye':
        right_eye_indices = [362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398]
        right_eye_points = [landmarks.landmark[i] for i in right_eye_indices]
        
        x_min = min([lm.x for lm in right_eye_points]) * w - padding
        y_min = min([lm.y for lm in right_eye_points]) * h - padding
        x_max = max([lm.x for lm in right_eye_points]) * w + padding
        y_max = max([lm.y for lm in right_eye_points]) * h + padding
    
    crop = image[int(y_min):int(y_max), int(x_min):int(x_max)]
    return crop


def analyze_emotions(image: np.ndarray) -> Dict:

      face_crop = detect_face_and_crop(image, 'face')  
      result = DeepFace.analyze(
          face_crop,
          actions=['emotion'],
      )

      emotions = result[0]['emotion']
      print(emotions.keys())
      stress = emotions.get('sad', 0) + emotions.get('fear', 0)
      fatigue = emotions.get('neutral', 0)
      anxiety = emotions.get('fear', 0)

      return {
          'stress': stress / 100.0,
          'fatigue': fatigue / 100.0,
          'anxiety': anxiety / 100.0
      }


def red_eye_classification(eye_crop_image):

    img_resized = cv2.resize(eye_crop_image, (224, 224))
    img_array = image.img_to_array(img_resized)
    img_array = np.expand_dims(img_array, axis=0)
    img_array /= 255.0


    prediction = eye_model.predict(img_array, verbose=0)
    predicted_class_idx = np.argmax(prediction, axis=1)[0]
    confidence = float(np.max(prediction))


    return 'red' if predicted_class_idx else 'healthy', confidence

def analyze_eyes(image: np.ndarray) -> Dict:
    cropped = detect_face_and_crop(image,'left_eye')
    le_class,le_conf = red_eye_classification(cropped)
    cropped = detect_face_and_crop(image,'right_eye')
    re_class,re_conf = red_eye_classification(cropped)
    return {
        'left_eye_class': le_class,
        'left_eye_confidence': le_conf,
        'rigth_eye_class': re_class,
        'right_eye_confidence':re_conf
    }


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
