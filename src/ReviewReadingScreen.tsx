import React, {useState} from 'react';
import {
  Alert,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {saveReading} from './ReadingsStorage';
import {ActivityType, HeartRateReading} from './ReadingsTypes';

type Props = {
  bpm: number;
  onBack: () => void;
  onSaved: () => void;
};

const activities: ActivityType[] = [
  'Resting',
  'Walking',
  'Running',
  'After Exercise',
  'Stressed',
  'Sleeping',
  'Other',
];

export default function ReviewReadingScreen({bpm, onBack, onSaved}: Props) {
  const [activityType, setActivityType] = useState<ActivityType>('Resting');
  const [notes, setNotes] = useState('');

  const handleSave = async () => {
    const reading: HeartRateReading = {
      id: Date.now().toString(),
      bpm,
      recordedAt: new Date().toISOString(),
      activityType,
      notes: notes.trim(),
    };

    try {
      await saveReading(reading);
      Alert.alert('Saved', 'Heart rate reading saved.');
      onSaved();
    } catch (error) {
      console.error('Save error:', error);
      Alert.alert('Error', 'Could not save reading.');
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Review Reading</Text>

        <View style={styles.card}>
          <Text style={styles.sectionLabel}>Measured BPM</Text>
          <Text style={styles.bpmValue}>{bpm} BPM</Text>

          <Text style={styles.sectionLabel}>Activity</Text>
          <View style={styles.activityContainer}>
            {activities.map(activity => (
              <TouchableOpacity
                key={activity}
                style={[
                  styles.activityButton,
                  activityType === activity && styles.activityButtonSelected,
                ]}
                onPress={() => setActivityType(activity)}>
                <Text
                  style={[
                    styles.activityButtonText,
                    activityType === activity && styles.activityButtonTextSelected,
                  ]}>
                  {activity}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.sectionLabel}>Notes</Text>
          <TextInput
            value={notes}
            onChangeText={setNotes}
            placeholder="Optional notes"
            placeholderTextColor="#777"
            multiline
            style={styles.notesInput}
          />
        </View>

        <TouchableOpacity style={styles.primaryButton} onPress={handleSave}>
          <Text style={styles.primaryButtonText}>Save Reading</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryButton} onPress={onBack}>
          <Text style={styles.secondaryButtonText}>Back to Camera</Text>
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
  },
  card: {
    backgroundColor: '#141416',
    borderRadius: 20,
    padding: 16,
    marginBottom: 20,
  },
  sectionLabel: {
    color: '#fff',
    fontSize: 16,
    marginTop: 12,
    marginBottom: 8,
  },
  bpmValue: {
    color: '#ff4a43',
    fontSize: 34,
    fontWeight: '800',
    marginBottom: 8,
  },
  activityContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  activityButton: {
    backgroundColor: '#222',
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginRight: 8,
    marginBottom: 8,
  },
  activityButtonSelected: {
    backgroundColor: '#ff4a43',
  },
  activityButtonText: {
    color: '#ddd',
  },
  activityButtonTextSelected: {
    color: '#fff',
    fontWeight: '700',
  },
  notesInput: {
    minHeight: 90,
    backgroundColor: '#222',
    borderRadius: 12,
    color: '#fff',
    paddingHorizontal: 12,
    paddingVertical: 10,
    textAlignVertical: 'top',
  },
  primaryButton: {
    backgroundColor: '#ff4a43',
    minHeight: 46,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
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