import React, {useState} from 'react';
import DashboardScreen from './src/Dashboard';
import CameraScreen from './src/CameraScreen';
import ReviewReadingScreen from './src/ReviewReadingScreen';

type Screen = 'dashboard' | 'camera' | 'review';

export default function App() {
  const [screen, setScreen] = useState<Screen>('dashboard');
  const [measuredBpm, setMeasuredBpm] = useState<number | null>(null);

  if (screen === 'camera') {
    return (
      <CameraScreen
        onBack={() => setScreen('dashboard')}
        onMeasurementComplete={(bpm: number) => {
          setMeasuredBpm(bpm);
          setScreen('review');
        }}
      />
    );
  }

  if (screen === 'review' && measuredBpm !== null) {
    return (
      <ReviewReadingScreen
        bpm={measuredBpm}
        onBack={() => setScreen('camera')}
        onSaved={() => {
          setMeasuredBpm(null);
          setScreen('dashboard');
        }}
      />
    );
  }

  return <DashboardScreen onOpenLog={() => setScreen('camera')} />;
}