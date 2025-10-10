from fastapi import FastAPI, UploadFile, File

app = FastAPI()

@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    # Заглушка: возвращает фиктивные данные
    return {
        "face_detected": True,
        "bounding_box": [100, 100, 200, 200],
        "landmarks": {"left_eye": [120, 130], "right_eye": [180, 130]}
    }