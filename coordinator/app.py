from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
import requests
import io
import json

app = FastAPI(title="Coordination Microservice")

# Assuming the URLs of the other microservices
FACE_ANALYSIS_URL = "http://face-analysis/analyze"
DISEASE_ANALYSIS_URL = "http://disease-analysis/analyze"

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    # Read the file content
    file_content = await file.read()
    filename = file.filename
    
    # Prepare files for requests
    face_files = {"file": (filename, file_content, file.content_type)}
    disease_files = {"file": (filename, file_content, file.content_type)}
    
    # Send to face analysis service
    try:
        face_response = requests.post(FACE_ANALYSIS_URL, files=face_files)
        face_response.raise_for_status()
        face_data = face_response.json()
    except requests.RequestException as e:
        return JSONResponse(status_code=500, content={"error": f"Face analysis failed: {str(e)}"})
    
    # Send to disease analysis service
    try:
        disease_response = requests.post(DISEASE_ANALYSIS_URL, files=disease_files)
        disease_response.raise_for_status()
        disease_data = disease_response.json()
    except requests.RequestException as e:
        return JSONResponse(status_code=500, content={"error": f"Disease analysis failed: {str(e)}"})
    
    # Merge the two JSONs
    merged_data = {**face_data, **disease_data}
    
    return JSONResponse(content=merged_data)