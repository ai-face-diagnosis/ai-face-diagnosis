from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
import requests
import cv2
import numpy as np
from crop import detect_face_and_crop

app = FastAPI(title="Coordination Microservice")


FACE_ANALYSIS_URL = "http://face-analysis/analyze"
DISEASE_ANALYSIS_URL = "http://disease-analysis/analyze"

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    file_content = await file.read()
    filename = file.filename
    image = cv2.imdecode(np.frombuffer(file_content, np.uint8), cv2.IMREAD_COLOR)
    
    try:
        face_bytes = await detect_face_and_crop(image, region='face')
        left_eye_bytes = await detect_face_and_crop(image, region='left_eye')
        right_eye_bytes = await detect_face_and_crop(image, region='right_eye')
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": f"Cropping failed: {str(e)}"})
    
    face_files = {
        "face": ("face.jpg", face_bytes, file.content_type),
        "left_eye": ("left_eye.jpg", left_eye_bytes, file.content_type),
        "right_eye": ("right_eye.jpg", right_eye_bytes, file.content_type),
    }
    disease_files = {"file": (filename, file_content, file.content_type)}

    try:
        face_response = requests.post(FACE_ANALYSIS_URL, files=face_files)
        face_response.raise_for_status()
        face_data = face_response.json()
    except requests.RequestException as e:
        return JSONResponse(status_code=500, content={"error": f"Face analysis failed: {str(e)}"})
    
    try:
        disease_response = requests.post(DISEASE_ANALYSIS_URL, files=disease_files)
        disease_response.raise_for_status()
        disease_data = disease_response.json()
    except requests.RequestException as e:
        return JSONResponse(status_code=500, content={"error": f"Disease analysis failed: {str(e)}"})
    
    merged_data = {**face_data, **disease_data}
    
    return JSONResponse(content=merged_data)