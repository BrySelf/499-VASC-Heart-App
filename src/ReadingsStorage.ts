import AsyncStorage from '@react-native-async-storage/async-storage';
import {HeartRateReading} from './ReadingsTypes';

const STORAGE_KEY = 'heart_rate_readings';

export async function getReadings(): Promise<HeartRateReading[]> {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);

    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw) as HeartRateReading[];

    // Return sorted oldest -> newest for graphing
    return parsed.sort(
      (a, b) =>
        new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime(),
    );
  } catch (error) {
    console.error('Failed to get readings:', error);
    return [];
  }
}

export async function saveReading(
  reading: HeartRateReading,
): Promise<HeartRateReading[]> {
  try {
    const existing = await getReadings();
    const updated = [...existing, reading];

    updated.sort(
      (a, b) =>
        new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime(),
    );

    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    return updated;
  } catch (error) {
    console.error('Failed to save reading:', error);
    throw error;
  }
}

export async function clearReadings(): Promise<void> {
  try {
    await AsyncStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    console.error('Failed to clear readings:', error);
    throw error;
  }
}