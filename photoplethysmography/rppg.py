from collections import namedtuple
import numpy as np
from PyQt5.QtCore import pyqtSignal, QObject
import mediapipe as mp
import cv2
import scipy as sp
from detector import ROIDetector
from camera import Camera
from livesosfilter import LiveSosFilter

# BASED ON CLASS BY SAMUEL PRÖLL, ADAPTED TO NEWER LIBRARY VERSIONS AND FRAMEWORKS

# bpm is a new field for us, not in original code by Samuel Pröll
RppgResults = namedtuple("RppgResults", ["rawimg", "roi_mask", "landmarks", "signal", "bpm"])

class RPPG(QObject):

    rppg_updated = pyqtSignal(RppgResults)
    capture_finished = pyqtSignal(float)

    def __init__(self, parent=None, video=0, filter_function=None):
        """rPPG model processing incoming frames and emitting calculation
        outputs."""
        super().__init__(parent=parent)

        self._cam = Camera(video=video, parent=parent)
        self._cam.frame_received.connect(self.on_frame_received)

        # first class functions are so fun common python W 
        if filter_function is None:
            self.filter_function = lambda x: x # unfiltered
        else:
            self.filter_function = filter_function

        self.signal = []

        self.detector = ROIDetector()

        self.target_frames = 30*30 # 30 seconds at 30 fps, for BPM calculation
        self.is_capturing = True

    def on_frame_received(self, frame):
        """Process new frame - find face mesh and emit outputs.
        """
        rawimg = frame.copy()
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        roi_mask, results = self.detector.process(rgb_frame)
        
        if roi_mask is not None:
            r, g, b, a = cv2.mean(rawimg, mask=roi_mask)
            # self.signal.append((r, g, b))
            self.signal.append(self.filter_function(g))
        else:
            self.signal.append(self.signal[-1] if len(self.signal) > 0 else 0)
        
        bpm = 0
        window_size = 900 # 30 seconds at 30 fps
        if len(self.signal) >= window_size:
            self.is_capturing = False
            bpm = self.get_bpm_from_fft(self.signal, fs=30)
            self.capture_finished.emit(bpm)
            self.stop()

        self.rppg_updated.emit(RppgResults(
            rawimg=rawimg, roi_mask=roi_mask, landmarks=results, signal=self.signal, bpm=bpm))
    
    def start(self):
        """Launch the camera thread.
        """
        self._cam.start()

    def stop(self):
        """Stop the camera thread and clean up the detector.
        """
        self._cam.stop()
        self.detector.close()

    # new stuff for actual rPPG HR extraction, not in original code by Samuel Pröll
    def get_bpm_from_fft(self, signal, fs=30):
        """Calculate BPM from signal using FFT."""
        # doing this to avoid the filter initialization artifact and an issue of trailing
        if len(signal) > 900:
            signal = signal[60:900]
        elif len(signal) > 60:
            signal = signal[60:]
        else:
            return 0

        n_fft = 2048 # zero-pad to a higher power of 2 to increase frequency resolution 

        freqs = np.fft.rfftfreq(n_fft, d=1/fs)
        fft = np.fft.rfft(signal - np.mean(signal), n=n_fft) # remove DC component
        mag = np.abs(fft)

        lower_bound = 1.1 # 66 BPM 
        upper_bound = 2 # 120 BPM
        lower_idx = np.where(freqs >= lower_bound)[0][0]
        upper_idx = np.where(freqs <= upper_bound)[0][-1] + 1 # add 1 to include
        best_freq_idx = np.argmax(mag[lower_idx:upper_idx]) + lower_idx
        bpm = freqs[best_freq_idx] * 60
        return bpm

# helper function to create a live filter with the right parameters for heartbeat extraction
def get_heartbeat_filter(order=4, cutoff=(1, 2), btype="bandpass", fs=30, output="ba"):
    """Create a live buttersworth filter with lfilter"""
    coefficients = sp.signal.iirfilter(order, Wn=cutoff, btype=btype, fs=fs, ftype="butter",
                                       output=output)
    # remember * unpacks the list of coefficients into separate arguments for LiveFilter
    return LiveSosFilter(coefficients)