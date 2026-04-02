import React, {useEffect, useState} from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  StatusBar,
} from 'react-native';
import {Camera, useCameraDevice} from 'react-native-vision-camera';

type Screen = 'dashboard' | 'camera' | 'log';

export default function App() {
  const [screen, setScreen] = useState<Screen>('dashboard');

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="light-content" backgroundColor="#050505" />
      <View style={styles.container}>
        {screen === 'dashboard' && (
          <DashboardScreen onOpenCamera={() => setScreen('camera')} />
        )}

        {screen === 'camera' && (
          <CameraScreen
            onBack={() => setScreen('dashboard')}
            onFinish={() => setScreen('log')}
          />
        )}

        {screen === 'log' && <LogScreen onSave={() => setScreen('dashboard')} />}
      </View>
    </SafeAreaView>
  );
}

function TopRightProfile() {
  return (
    <TouchableOpacity style={styles.profileButton}>
      <Text style={styles.profileIcon}>◔</Text>
    </TouchableOpacity>
  );
}

function DashboardScreen({onOpenCamera}: {onOpenCamera: () => void}) {
  return (
    <View style={styles.screen}>
      <View style={styles.topRow}>
        <Text style={styles.screenLabel}>Dashboard</Text>
        <TopRightProfile />
      </View>

      <View style={styles.cardLarge}>
        <Text style={styles.cardTitle}>Recent Heart Rate</Text>
      </View>

      <View style={styles.cardTall}>
        <Text style={styles.cardMuted}>Graph with logged heart rate</Text>
      </View>

      <View style={styles.cardSmall}>
        <Text style={styles.cardMuted}>last log</Text>
      </View>

      <TouchableOpacity style={styles.bottomButton} onPress={onOpenCamera}>
        <Text style={styles.plusIcon}>＋</Text>
        <Text style={styles.bottomButtonText}>Log New Reading</Text>
      </TouchableOpacity>
    </View>
  );
}

function CameraScreen({
  onBack,
  onFinish,
}: {
  onBack: () => void;
  onFinish: () => void;
}) {
  const [hasPermission, setHasPermission] = useState(false);
  const [permissionChecked, setPermissionChecked] = useState(false);

  const device = useCameraDevice('back');

  useEffect(() => {
    const getPermission = async () => {
      const current = await Camera.getCameraPermissionStatus();

      if (current === 'granted') {
        setHasPermission(true);
        setPermissionChecked(true);
        return;
      }

      const next = await Camera.requestCameraPermission();
      setHasPermission(next === 'granted');
      setPermissionChecked(true);
    };

    getPermission().catch(err => {
      console.error('Permission error:', err);
      setPermissionChecked(true);
    });
  }, []);

  return (
    <View style={styles.screen}>
      <View style={styles.topRow}>
        <Text style={styles.screenLabel}>Camera section</Text>
        <TopRightProfile />
      </View>

      <View style={styles.searchPill}>
        <Text style={styles.searchText}>Camera Preview</Text>
      </View>

      <View style={styles.cameraBox}>
        {!permissionChecked ? (
          <Text style={styles.cameraOverlayText}>Checking permission...</Text>
        ) : !hasPermission ? (
          <Text style={styles.cameraOverlayText}>Camera permission denied</Text>
        ) : device == null ? (
          <Text style={styles.cameraOverlayText}>No camera device found</Text>
        ) : (
          <>
            <Camera
              style={StyleSheet.absoluteFill}
              device={device}
              isActive={true}
              resizeMode="cover"
            />
          </>
        )}
      </View>

      <Text style={styles.recordingLabel}>Recording</Text>

      <View style={styles.cameraBottomArea}>
        <TouchableOpacity style={styles.recordButton} onPress={onFinish}>
          <Text style={styles.recordIcon}>▶</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.secondaryButton} onPress={onBack}>
        <Text style={styles.secondaryButtonText}>Back</Text>
      </TouchableOpacity>
    </View>
  );
}

function LogScreen({onSave}: {onSave: () => void}) {
  return (
    <View style={styles.screen}>
      <View style={styles.topRow}>
        <Text style={styles.screenLabel}>Log Screen</Text>
        <TopRightProfile />
      </View>

      <View style={styles.cardLargeCentered}>
        <Text style={styles.cardTitleCentered}>Calculated Heart Rate</Text>
      </View>

      <View style={styles.cardTallCentered}>
        <Text style={styles.cardTitleCentered}>Options for logging activity type</Text>
      </View>

      <TouchableOpacity style={styles.bottomButtonCentered} onPress={onSave}>
        <Text style={styles.bottomButtonText}>Log Reading</Text>
      </TouchableOpacity>
    </View>
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
  },
  screen: {
    flex: 1,
    paddingHorizontal: 22,
    paddingTop: 10,
    paddingBottom: 24,
    backgroundColor: '#050505',
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    minHeight: 42,
    marginBottom: 12,
  },
  screenLabel: {
    color: '#b88cff',
    fontSize: 12,
    fontWeight: '600',
  },
  profileButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#1c1c22',
    alignItems: 'center',
    justifyContent: 'center',
  },
  profileIcon: {
    color: '#8a8a95',
    fontSize: 14,
  },
  cardLarge: {
    backgroundColor: '#141416',
    borderRadius: 24,
    minHeight: 112,
    paddingHorizontal: 22,
    justifyContent: 'center',
    marginBottom: 14,
  },
  cardLargeCentered: {
    backgroundColor: '#141416',
    borderRadius: 24,
    minHeight: 112,
    paddingHorizontal: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 14,
  },
  cardTall: {
    backgroundColor: '#141416',
    borderRadius: 24,
    minHeight: 178,
    paddingHorizontal: 22,
    justifyContent: 'center',
    marginBottom: 14,
  },
  cardTallCentered: {
    backgroundColor: '#141416',
    borderRadius: 24,
    minHeight: 190,
    paddingHorizontal: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 18,
  },
  cardSmall: {
    backgroundColor: '#141416',
    borderRadius: 20,
    minHeight: 42,
    paddingHorizontal: 18,
    justifyContent: 'center',
    marginBottom: 18,
  },
  cardTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  cardTitleCentered: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
    textAlign: 'center',
  },
  cardMuted: {
    color: '#5e5e66',
    fontSize: 15,
    textAlign: 'center',
  },
  searchPill: {
    alignSelf: 'center',
    backgroundColor: '#141416',
    borderRadius: 20,
    paddingHorizontal: 30,
    minHeight: 30,
    justifyContent: 'center',
    marginBottom: 28,
    minWidth: 160,
  },
  searchText: {
    color: '#8e8e95',
    textAlign: 'center',
    fontSize: 12,
  },
  cameraBox: {
    backgroundColor: '#141416',
    borderRadius: 24,
    minHeight: 220,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
    position: 'relative',
  },
  cameraOverlayText: {
    color: '#d0d0d6',
    textAlign: 'center',
    fontSize: 16,
    paddingHorizontal: 20,
  },
  cameraHintOverlay: {
    position: 'absolute',
    bottom: 12,
    left: 12,
    right: 12,
    backgroundColor: 'rgba(0,0,0,0.45)',
    borderRadius: 12,
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  cameraHintText: {
    color: '#ffffff',
    textAlign: 'center',
    fontSize: 12,
  },
  recordingLabel: {
    color: '#35353a',
    textAlign: 'center',
    fontSize: 16,
    marginBottom: 28,
  },
  cameraBottomArea: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
    marginBottom: 18,
  },
  recordButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#ff2d20',
    alignItems: 'center',
    justifyContent: 'center',
  },
  recordIcon: {
    color: '#ffffff',
    fontSize: 16,
    marginLeft: 2,
  },
  bottomButton: {
    marginTop: 'auto',
    backgroundColor: '#ff4a43',
    borderRadius: 24,
    minHeight: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  bottomButtonCentered: {
    marginTop: 'auto',
    backgroundColor: '#ff4a43',
    borderRadius: 24,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  bottomButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  plusIcon: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  secondaryButton: {
    alignSelf: 'center',
    marginTop: 8,
    paddingHorizontal: 18,
    paddingVertical: 8,
  },
  secondaryButtonText: {
    color: '#b88cff',
    fontSize: 14,
    fontWeight: '600',
  },
});