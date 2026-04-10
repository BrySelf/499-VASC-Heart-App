import React, {useEffect, useState} from 'react';
import {
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import ReadingsChart from './ReadingChart';
import {getReadings} from './ReadingsStorage';
import {HeartRateReading} from './ReadingsTypes';

type Props = {
  onOpenLog: () => void;
};

function formatDateTime(iso: string): {date: string; time: string} {
  const date = new Date(iso);

  return {
    date: date.toLocaleDateString(),
    time: date.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    }),
  };
}

export default function DashboardScreen({onOpenLog}: Props) {
  const [readings, setReadings] = useState<HeartRateReading[]>([]);

  useEffect(() => {
    const loadReadings = async () => {
      const data = await getReadings();
      setReadings(data);
    };

    loadReadings();
  }, []);

  const latest = readings.length > 0 ? readings[readings.length - 1] : null;

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Heart Rate Dashboard</Text>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Most Recent Reading</Text>
          {latest ? (
            <>
              <Text style={styles.latestBpm}>{latest.bpm} BPM</Text>
              <Text style={styles.metaText}>
                {formatDateTime(latest.recordedAt).date} at{' '}
                {formatDateTime(latest.recordedAt).time}
              </Text>
              <Text style={styles.metaText}>
                Activity: {latest.activityType}
              </Text>
              {latest.notes ? (
                <Text style={styles.metaText}>Notes: {latest.notes}</Text>
              ) : null}
            </>
          ) : (
            <Text style={styles.emptyText}>No readings saved yet.</Text>
          )}
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Chronological Heart Rate Graph</Text>
          <ReadingsChart readings={readings} />
        </View>

        <View style={[styles.card, styles.historyCard]}>
          <Text style={styles.sectionTitle}>Reading History</Text>

          {readings.length === 0 ? (
            <Text style={styles.emptyText}>No history yet.</Text>
          ) : (
            <FlatList
              data={[...readings].reverse()}
              keyExtractor={item => item.id}
              renderItem={({item}) => {
                const formatted = formatDateTime(item.recordedAt);

                return (
                  <View style={styles.readingItem}>
                    <Text style={styles.readingBpm}>{item.bpm} BPM</Text>
                    <Text style={styles.readingText}>
                      {formatted.date} at {formatted.time}
                    </Text>
                    <Text style={styles.readingText}>
                      Activity: {item.activityType}
                    </Text>
                    {item.notes ? (
                      <Text style={styles.readingText}>
                        Notes: {item.notes}
                      </Text>
                    ) : null}
                  </View>
                );
              }}
            />
          )}
        </View>

        <TouchableOpacity style={styles.button} onPress={onOpenLog}>
          <Text style={styles.buttonText}>Log New Reading</Text>
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
    padding: 16,
  },
  title: {
    color: '#fff',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 14,
  },
  card: {
    backgroundColor: '#141416',
    borderRadius: 20,
    padding: 16,
    marginBottom: 14,
  },
  historyCard: {
    flex: 1,
  },
  sectionTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 10,
  },
  latestBpm: {
    color: '#ff4a43',
    fontSize: 30,
    fontWeight: '800',
    marginBottom: 6,
  },
  metaText: {
    color: '#cfcfd3',
    fontSize: 14,
    marginBottom: 4,
  },
  emptyText: {
    color: '#8a8a95',
    fontSize: 15,
  },
  readingItem: {
    backgroundColor: '#222',
    borderRadius: 14,
    padding: 12,
    marginBottom: 10,
  },
  readingBpm: {
    color: '#ff4a43',
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 4,
  },
  readingText: {
    color: '#ddd',
    fontSize: 14,
    marginBottom: 2,
  },
  button: {
    backgroundColor: '#ff4a43',
    borderRadius: 24,
    minHeight: 46,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});