import React, {useEffect, useRef, useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  NativeModules,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import RNFS from 'react-native-fs';
import {Camera, useCameraDevice} from 'react-native-vision-camera';

type Props = {
  onBack: () => void;
  onMeasurementComplete: (bpm: number) => void;
};

const {OpenCVModule} = NativeModules;

export default function CameraScreen({
  onBack,
  onMeasurementComplete,
}: Props) {
  const device = useCameraDevice('back');
  const cameraRef = useRef<Camera | null>(null);

  const [hasPermission, setHasPermission] = useState(false);
  const [permissionChecked, setPermissionChecked] = useState(false);
  const [isMeasuring, setIsMeasuring] = useState(false);
  const [statusText, setStatusText] = useState('Ready to measure');

  const measuringRef = useRef(false);
  const frameCountRef = useRef(0);

  useEffect(() => {
    const checkPermission = async () => {
      try {
        const status = Camera.getCameraPermissionStatus();

        if (status === 'granted') {
          setHasPermission(true);
          setPermissionChecked(true);
          return;
        }

        const nextStatus = await Camera.requestCameraPermission();
        setHasPermission(nextStatus === 'granted');
        setPermissionChecked(true);
      } catch (error) {
        console.error('Permission error:', error);
        setPermissionChecked(true);
      }
    };

    checkPermission();
  }, []);

  const readFileAsBase64 = async (filePath: string): Promise<string> => {
    return RNFS.readFile(filePath, 'base64');
  };

  const stopMeasurement = async () => {
    measuringRef.current = false;
    setIsMeasuring(false);
    setStatusText('Ready to measure');

    try {
      await OpenCVModule.resetMeasurement();
    } catch (error) {
      console.warn('Failed to reset measurement:', error);
    }
  };

  const captureAndProcessFrame = async () => {
    if (!measuringRef.current) {
      return;
    }

    if (!cameraRef.current || !device) {
      await stopMeasurement();
      Alert.alert('Camera unavailable', 'Camera reference or device is missing.');
      return;
    }

    try {
      setStatusText(`Capturing frame ${frameCountRef.current + 1}...`);

      const photo = await cameraRef.current.takePhoto({
        flash: 'off',
      });

      if (!photo?.path) {
        throw new Error('Photo path was not returned.');
      }

      const base64Image = await readFileAsBase64(photo.path);

      frameCountRef.current += 1;
      setStatusText(`Processing frame ${frameCountRef.current}...`);

      const bpm = await OpenCVModule.processFrame(
        base64Image,
        Date.now(),
        30,
      );

      if (typeof bpm === 'number' && !Number.isNaN(bpm)) {
        measuringRef.current = false;
        setIsMeasuring(false);
        setStatusText(`Measurement complete: ${Math.round(bpm)} BPM`);
        onMeasurementComplete(Math.round(bpm));
        return;
      }

      setTimeout(() => {
        captureAndProcessFrame().catch(error => {
          console.error('Measurement loop error:', error);
        });
      }, 150);
    } catch (error) {
      console.error('Measurement error:', error);
      await stopMeasurement();
      Alert.alert('Measurement failed', 'Could not calculate heart rate.');
    }
  };

  const handleStartMeasurement = async () => {
    if (!hasPermission || device == null) {
      Alert.alert('Camera unavailable', 'Camera permission or device is missing.');
      return;
    }

    try {
      await OpenCVModule.resetMeasurement();

      frameCountRef.current = 0;
      measuringRef.current = true;
      setIsMeasuring(true);
      setStatusText('Starting measurement...');

      captureAndProcessFrame().catch(error => {
        console.error('Initial capture error:', error);
      });
    } catch (error) {
      console.error('Start measurement error:', error);
      Alert.alert('Measurement failed', 'Could not start heart rate measurement.');
      setIsMeasuring(false);
      measuringRef.current = false;
      setStatusText('Ready to measure');
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Measure Heart Rate</Text>

        <View style={styles.cameraCard}>
          {!permissionChecked ? (
            <Text style={styles.infoText}>Checking camera permission...</Text>
          ) : !hasPermission ? (
            <Text style={styles.infoText}>Camera permission denied.</Text>
          ) : device == null ? (
            <Text style={styles.infoText}>No back camera found.</Text>
          ) : (
            <>
              <Camera
                ref={cameraRef}
                style={StyleSheet.absoluteFill}
                device={device}
                isActive={true}
                photo={true}
              />
              <View style={styles.overlay}>
                <Text style={styles.overlayText}>
                  Center face in frame with good lighting
                </Text>
              </View>
            </>
          )}
        </View>

        <Text style={styles.subtitle}>{statusText}</Text>

        {isMeasuring ? (
          <>
            <ActivityIndicator size="large" />
            <TouchableOpacity style={styles.stopButton} onPress={stopMeasurement}>
              <Text style={styles.stopButtonText}>Stop Measurement</Text>
            </TouchableOpacity>
          </>
        ) : (
          <TouchableOpacity style={styles.primaryButton} onPress={handleStartMeasurement}>
            <View style={styles.buttonContent}>
              <Text style={styles.buttonIcon}>▶</Text>
              <Text style={styles.primaryButtonText}>Start Measurement</Text>
            </View>
          </TouchableOpacity>
        )}

        <TouchableOpacity style={styles.secondaryButton} onPress={onBack}>
          <Text style={styles.secondaryButtonText}>Back</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#050505',
  },
  container: {
    flex: 1,
    backgroundColor: '#050505',
    padding: 20,
  },
  title: {
    color: '#fff',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 16,
    marginTop: 28,
  },
  cameraCard: {
    flex: 1,
    borderRadius: 20,
    overflow: 'hidden',
    backgroundColor: '#141416',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  infoText: {
    color: '#d0d0d6',
    fontSize: 16,
    textAlign: 'center',
    paddingHorizontal: 20,
  },
  overlay: {
    position: 'absolute',
    left: 12,
    right: 12,
    bottom: 12,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 12,
    backgroundColor: 'rgba(0,0,0,0.45)',
  },
  overlayText: {
    color: '#fff',
    textAlign: 'center',
    fontSize: 13,
  },
  subtitle: {
    color: '#cfcfd3',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
  },
  primaryButton: {
    backgroundColor: '#ff4a43',
    minHeight: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  stopButton: {
    backgroundColor: '#333',
    minHeight: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 12,
    marginBottom: 12,
  },
  stopButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonIcon: {
    color: '#fff',
    fontSize: 18,
    marginRight: 10,
    fontWeight: '700',
  },
  primaryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 40,
  },
  secondaryButtonText: {
    color: '#b88cff',
    fontSize: 15,
    fontWeight: '600',
  },
});