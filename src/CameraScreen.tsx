import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
  StyleSheet,
  NativeModules,
  Dimensions,
} from 'react-native';
import { Camera, useCameraDevice } from 'react-native-vision-camera';

const { OpenCVModule } = NativeModules;
const { width } = Dimensions.get('window');

interface CameraScreenProps {
  onBack: () => void;
  onMeasurementComplete: (bpm: number) => void;
}

export default function CameraScreen({
  onBack,
  onMeasurementComplete,
}: CameraScreenProps) {
  const device = useCameraDevice('front');
  const [isMeasuring, setIsMeasuring] = useState(false);
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState(
    'Align your face and press start',
  );

  // Wrapped in useCallback to prevent the "missing dependency" lint error
  const handleFinalize = useCallback(async () => {
    setStatusText('Analyzing signal...');
    try {
      // Calls the native FFT logic we put in Kotlin
      const bpm = await OpenCVModule.getFinalBPM();
      onMeasurementComplete(Math.round(bpm));
    } catch (error) {
      console.error('Finalize error:', error);
      setStatusText('Measurement failed. Try again.');
      setIsMeasuring(false);
    }
  }, [onMeasurementComplete]);

  // Polling loop: Asks Kotlin "how many frames have you collected?"
  useEffect(() => {
    let interval: any;

    if (isMeasuring) {
      interval = setInterval(async () => {
        try {
          const status = await OpenCVModule.getStatus();
          const currentProgress = status.frameCount / 900;
          setProgress(currentProgress);

          if (status.frameCount >= 900) {
            clearInterval(interval);
            handleFinalize();
          }
        } catch (error) {
          console.error('Polling error:', error);
        }
      }, 200);
    }

    return () => clearInterval(interval);
  }, [isMeasuring, handleFinalize]);

  const handleStartMeasurement = async () => {
    try {
      await OpenCVModule.startMeasurement();
      setIsMeasuring(true);
      setStatusText('Stay still... measuring');
    } catch (error) {
      console.error('Start error:', error);
    }
  };

  const stopMeasurement = async () => {
    try {
      await OpenCVModule.stopMeasurement();
      setIsMeasuring(false);
      setProgress(0);
      setStatusText('Measurement cancelled');
    } catch (error) {
      console.error('Stop error:', error);
    }
  };

  if (!device)
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.errorText}>No Camera Found</Text>
      </SafeAreaView>
    );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.cameraContainer}>
        <Camera
          style={StyleSheet.absoluteFill}
          device={device}
          isActive={true}
        />

        <View style={styles.overlay}>
          <View style={styles.statusBadge}>
            <Text style={styles.statusText}>{statusText}</Text>
          </View>

          {isMeasuring && (
            <View style={styles.progressWrapper}>
              <View style={styles.progressBarBg}>
                <View
                  style={[
                    styles.progressBarFill,
                    { width: `${Math.min(progress * 100, 100)}%` },
                  ]}
                />
              </View>
              <Text style={styles.percentageText}>
                {Math.round(progress * 100)}%
              </Text>
            </View>
          )}
        </View>
      </View>

      <View style={styles.controls}>
        {!isMeasuring ? (
          <>
            <TouchableOpacity
              style={styles.primaryButton}
              onPress={handleStartMeasurement}
            >
              <Text style={styles.buttonText}>Start Measurement</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.backButton} onPress={onBack}>
              <Text style={styles.backButtonText}>Back</Text>
            </TouchableOpacity>
          </>
        ) : (
          <TouchableOpacity style={styles.stopButton} onPress={stopMeasurement}>
            <Text style={styles.buttonText}>Cancel</Text>
          </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  cameraContainer: {
    flex: 1,
    overflow: 'hidden',
    borderRadius: 20,
    margin: 10,
    backgroundColor: '#111',
  },
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'space-between',
    padding: 20,
    alignItems: 'center',
  },
  statusBadge: {
    backgroundColor: 'rgba(0,0,0,0.6)',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 30,
    marginTop: 20,
  },
  statusText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  progressWrapper: {
    width: '100%',
    alignItems: 'center',
    marginBottom: 40,
  },
  progressBarBg: {
    width: width * 0.8,
    height: 12,
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderRadius: 6,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: '#00FF88', // Nice health-focused green
  },
  percentageText: {
    color: '#fff',
    marginTop: 8,
    fontWeight: 'bold',
  },
  controls: {
    padding: 20,
    paddingBottom: 40,
  },
  primaryButton: {
    backgroundColor: '#007AFF',
    padding: 18,
    borderRadius: 15,
    alignItems: 'center',
    marginBottom: 12,
  },
  stopButton: {
    backgroundColor: '#FF3B30',
    padding: 18,
    borderRadius: 15,
    alignItems: 'center',
  },
  backButton: {
    padding: 15,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  backButtonText: {
    color: '#999',
    fontSize: 16,
  },
  errorText: {
    color: '#fff',
    textAlign: 'center',
    marginTop: 50,
  },
});
