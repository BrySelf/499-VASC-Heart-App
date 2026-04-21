import mediapipe as mp
import numpy as np
import cv2

# BASED ON CLASS BY SAMUEL PRÖLL, ADAPTED TO NEWER LIBRARY VERSIONS AND FRAMEWORKS

def fill_roi_mask(point_list, img):
    """Given a list of points and an image, create a mask with the polygon
    defined by the points filled in."""
    h, w, _ = img.shape
    mask = np.zeros((h,w), dtype=np.uint8)
    cv2.fillPoly(mask, [point_list.astype(np.int32)], 255)
    return mask

class ROIDetector:
        def __init__(self):
            self.detector = mp.tasks.vision.FaceLandmarker.create_from_options(
            mp.tasks.vision.FaceLandmarkerOptions(
                base_options=mp.tasks.BaseOptions(model_asset_path="face_landmarker.task"),
                num_faces=1,
                min_face_detection_confidence=0.5,
                min_face_presence_confidence=0.5,
                min_tracking_confidence=0.5
            )
        )
        _lower_face = [200, 431, 411, 340, 349, 120, 111, 187, 211] # mesh indices
        def process(self, frame):
            """Find single face in frame and extract lower half of face"""
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame)
            results = self.detector.detect(mp_image)
            if not results.face_landmarks:
                return None, results
            
            h, w, _ = frame.shape
            landmarks = results.face_landmarks[0]
            coords = np.array([(int(i.x * w), int(i.y * h)) for i in landmarks])

            point_list = coords[self._lower_face]

            mask = fill_roi_mask(point_list, frame)

            return mask, results
        
        def close(self):
            self.detector.close()