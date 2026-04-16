import React, {useEffect, useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {Camera, useCameraDevice} from 'react-native-vision-camera';

type Props = {
  onBack: () => void;
  onMeasurementComplete: (bpm: number) => void;
};

export default function CameraScreen({
  onBack,
  onMeasurementComplete,
}: Props) {
  const device = useCameraDevice('back');

  const [hasPermission, setHasPermission] = useState(false);
  const [permissionChecked, setPermissionChecked] = useState(false);
  const [isMeasuring, setIsMeasuring] = useState(false);

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

  const handleStartMeasurement = async () => {
    if (!hasPermission || device == null) {
      Alert.alert('Camera unavailable', 'Camera permission or device is missing.');
      return;
    }

    try {
      setIsMeasuring(true);

      // Replace this with your OpenCV-based measurement later.
    //   await new Promise(resolve => setTimeout(resolve, 3000));

      const simulatedBpm = 78;
      onMeasurementComplete(simulatedBpm);
    } catch (error) {
      console.error('Measurement error:', error);
      Alert.alert('Measurement failed', 'Could not calculate heart rate.');
    } finally {
      setIsMeasuring(false);
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
                style={StyleSheet.absoluteFill}
                device={device}
                isActive={!isMeasuring}
              />
              <View style={styles.overlay}>
                <Text style={styles.overlayText}>
                  Center face in frame with good lighting
                </Text>
              </View>
            </>
          )}
        </View>

        <Text style={styles.subtitle}>
          {isMeasuring ? 'Measuring...' : 'Ready to measure'}
        </Text>

        {isMeasuring ? (
          <ActivityIndicator size="large" />
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
