import csv

from PyQt5.QtWidgets import QMainWindow
import pyqtgraph as pg
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
import numpy as np
import cv2

# BASED ON CLASS BY SAMUEL PRÖLL, ADAPTED TO NEWER LIBRARY VERSIONS AND FRAMEWORKS

class MainWindow(QMainWindow):
    def __init__(self, rppg):
        """MainWindow visualizing the output of the RPPG model.
        """
        super().__init__()

        self.rppg = rppg
        self.rppg.rppg_updated.connect(self.on_rppg_updated)
        self.rppg.capture_finished.connect(self.on_capture_finished)
        self.init_ui()

    def on_rppg_updated(self, output):
        """Update UI based on RppgResults.
        """
        img = output.rawimg.copy()
        draw_facemesh(img, output.landmarks, tesselate=True, contour=True)
        self.img.setImage(img)
        # self.line.setData(y=output.signal[-200:-1])

        if output.bpm > 0:
            self.bpm_label.setText(f"BPM: {int(output.bpm)}")

    def on_capture_finished(self, bpm):
        """Handle capture finished event - update BPM label with final BPM.
        """
        self.bpm_label.setText(f"Final BPM: {int(bpm)}")
        self.bpm_label.setColor('w')
        print(f"Capture finished. Final BPM: {bpm}")

    def init_ui(self):
        """Initialize window with pyqtgraph image view box in the center.
        """
        self.setWindowTitle("FaceMesh detection in PyQt")

        layout = pg.GraphicsLayoutWidget()
        self.img = pg.ImageItem(axisOrder="row-major")
        vb = layout.addViewBox(invertX=True, invertY=True, lockAspect=True)
        vb.addItem(self.img)
        self.plot = layout.addPlot(row=1, col=0)
        # self.line = self.plot.plot(pen=pg.mkPen('r', width=2))

        # adding BPM label
        self.bpm_label = pg.TextItem(text="BPM:--", color='w', anchor=(0, 0))
        self.bpm_label.setFont(pg.QtGui.QFont("Arial", 12))
        self.plot.addItem(self.bpm_label)

        self.setCentralWidget(layout)


# we have to manually draw since mp got rid of drawing utils
def draw_facemesh(img, results, tesselate=False,
                  contour=False, irises=False):
    """Draw all facemesh landmarks found in an image.

    Irises are only drawn if the corresponding landmarks are present,
    which requires FaceMesh to be initialized with refine=True.
    """
    if results is None or results.face_landmarks is None:
        return

    for face_landmarks in results.face_landmarks:
        h, w, _ = img.shape

        pixel_points = []
        for lm in face_landmarks:
            x = int(lm.x * w)
            y = int(lm.y * h)
            pixel_points.append((x, y))

        if tesselate:
            for connection in vision.FaceLandmarksConnections.FACE_LANDMARKS_TESSELATION:
                cv2.line(img, pixel_points[connection.start], pixel_points[connection.end], (0, 255, 0), 1)

        if contour:
            for connection in vision.FaceLandmarksConnections.FACE_LANDMARKS_CONTOURS:
                cv2.line(img, pixel_points[connection.start], pixel_points[connection.end], (255, 0, 0), 1)

        if irises:
            for connection in vision.FaceLandmarksConnections.FACE_LANDMARKS_IRISES:
                cv2.line(img, pixel_points[connection.start], pixel_points[connection.end], (0, 0, 255), 1)