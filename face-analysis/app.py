from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
import cv2
import numpy as np
from transformers import AutoImageProcessor, AutoModelForImageClassification
from PIL import Image
import torch
import mediapipe as mp

app = FastAPI()

# Load models and processors globally
emotion_processor = AutoImageProcessor.from_pretrained("trpakov/vit-face-expression")
emotion_model = AutoModelForImageClassification.from_pretrained("trpakov/vit-face-expression")

skin_processor = AutoImageProcessor.from_pretrained("dima806/skin_types_image_detection")
skin_model = AutoModelForImageClassification.from_pretrained("dima806/skin_types_image_detection")

eye_processor = AutoImageProcessor.from_pretrained("MichalMlodawski/open-closed-eye-classification-mobilev2")
eye_model = AutoModelForImageClassification.from_pretrained("MichalMlodawski/open-closed-eye-classification-mobilev2")

# MediaPipe for potential enhancements (not fully utilized here but loaded for future)
mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True, min_detection_confidence=0.5)

def analyze_emotions(img: np.ndarray) -> dict:
    """
    Analyze emotions from the face image using a Vision Transformer model.
    Emotions can indicate overall state, e.g., sad or neutral may suggest tiredness.
    """
    try:
        pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        inputs = emotion_processor(pil_img, return_tensors="pt")
        with torch.no_grad():
            outputs = emotion_model(**inputs)
            logits = outputs.logits
            predicted_idx = logits.argmax(-1).item()
            predicted_label = emotion_model.config.id2label[predicted_idx]
            probs = torch.softmax(logits, dim=-1).tolist()[0]
            emotions = {emotion_model.config.id2label[i]: prob for i, prob in enumerate(probs)}
        return {"dominant_emotion": predicted_label, "emotion_probabilities": emotions}
    except Exception as e:
        return {"error": str(e)}

def analyze_eyes(img: np.ndarray) -> dict:
    """
    Analyze eye state: open/closed, redness/yellowness levels, dark circles, and puffiness as indicators of general state like fatigue.
    Focus on descriptive levels rather than diagnoses.
    """
    try:
        pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        inputs = eye_processor(pil_img, return_tensors="pt")
        with torch.no_grad():
            outputs = eye_model(**inputs)
            logits = outputs.logits
            predicted_idx = logits.argmax(-1).item()
            state = eye_model.config.id2label[predicted_idx]
        
        if state == "closed":  # Adjust based on actual labels, assuming 'closed' and 'open'
            return {"state": "closed", "indicators": "unable to analyze colors further"}
        
        # Color analysis for redness and yellowness
        hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        
        # Red mask
        lower_red1 = np.array([0, 70, 70])
        upper_red1 = np.array([10, 255, 255])
        mask_red1 = cv2.inRange(hsv, lower_red1, upper_red1)
        
        lower_red2 = np.array([170, 70, 70])
        upper_red2 = np.array([180, 255, 255])
        mask_red2 = cv2.inRange(hsv, lower_red2, upper_red2)
        
        mask_red = mask_red1 + mask_red2
        red_ratio = np.sum(mask_red > 0) / (img.shape[0] * img.shape[1])
        
        # Yellow mask
        lower_yellow = np.array([20, 100, 100])
        upper_yellow = np.array([30, 255, 255])
        mask_yellow = cv2.inRange(hsv, lower_yellow, upper_yellow)
        yellow_ratio = np.sum(mask_yellow > 0) / (img.shape[0] * img.shape[1])
        
        # Dark circles: brightness in lower region
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        h, w = gray.shape
        lower_region = gray[int(h * 0.6):, :]  # Lower 40% assumed as under-eye
        lower_mean = np.mean(lower_region)
        dark_circles = "low"
        if lower_mean < 80:
            dark_circles = "high"
        elif lower_mean < 120:
            dark_circles = "medium"
        
        # Puffiness: variance in lower region (high variance may indicate texture changes from puffiness)
        lower_variance = cv2.Laplacian(lower_region, cv2.CV_64F).var()
        puffiness = "low" if lower_variance < 50 else "high"  # Arbitrary threshold
        
        redness_level = "high" if red_ratio > 0.05 else "low"
        yellowness_level = "high" if yellow_ratio > 0.05 else "low"
        
        return {
            "state": "open",
            "redness": redness_level,
            "yellowness": yellowness_level,
            "dark_circles": dark_circles,
            "puffiness": puffiness,
            "notes": "High levels may indicate fatigue or strain"
        }
    except Exception as e:
        return {"error": str(e)}

def analyze_skin(img: np.ndarray) -> dict:
    """
    Analyze skin type and texture as indicators of general state (e.g., dry skin may suggest dehydration or fatigue).
    """
    try:
        pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        inputs = skin_processor(pil_img, return_tensors="pt")
        with torch.no_grad():
            outputs = skin_model(**inputs)
            logits = outputs.logits
            predicted_idx = logits.argmax(-1).item()
            predicted_label = skin_model.config.id2label[predicted_idx]
            probs = torch.softmax(logits, dim=-1).tolist()[0]
            types = {skin_model.config.id2label[i]: prob for i, prob in enumerate(probs)}
        
        # Add texture analysis
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        texture_variance = cv2.Laplacian(gray, cv2.CV_64F).var()
        texture = "smooth" if texture_variance < 100 else "rough"  # Arbitrary threshold
        
        return {"skin_type": predicted_label, "type_probabilities": types, "texture": texture}
    except Exception as e:
        return {"error": str(e)}

@app.post("/analyze")
async def analyze_face(
    face: UploadFile = File(...),
    left_eye: UploadFile = File(...),
    right_eye: UploadFile = File(...)
):
    # Read images
    face_bytes = await face.read()
    left_eye_bytes = await left_eye.read()
    right_eye_bytes = await right_eye.read()

    # Decode
    face_img = cv2.imdecode(np.frombuffer(face_bytes, np.uint8), cv2.IMREAD_COLOR)
    left_eye_img = cv2.imdecode(np.frombuffer(left_eye_bytes, np.uint8), cv2.IMREAD_COLOR)
    right_eye_img = cv2.imdecode(np.frombuffer(right_eye_bytes, np.uint8), cv2.IMREAD_COLOR)

    face_result = analyze_emotions(face_img)
    skin_result = analyze_skin(face_img)
    left_result = analyze_eyes(left_eye_img)
    right_result = analyze_eyes(right_eye_img)


    return JSONResponse({
        "face_analysis": face_result,
        "skin_analysis": skin_result,
        "left_eye_analysis": left_result,
        "right_eye_analysis": right_result,
    })