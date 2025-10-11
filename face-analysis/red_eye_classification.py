import cv2
from tensorflow.keras.preprocessing import image
import numpy as np
import tensorflow as tf


model = tf.keras.models.load_model('my_model.h5')

def red_eye_classification(eye_crop_image):

    img_resized = cv2.resize(eye_crop_image, (224, 224))
    img_array = image.img_to_array(img_resized)
    img_array = np.expand_dims(img_array, axis=0)
    img_array /= 255.0 
    

    prediction = model.predict(img_array, verbose=0)
    predicted_class_idx = np.argmax(prediction, axis=1)[0]
    confidence = float(np.max(prediction))

    
    return 'red' if predicted_class_idx else 'healthy', confidence

