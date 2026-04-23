import React, {useEffect, useState} from 'react';
import {
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Alert,
} from 'react-native';
import ReadingsChart from './ReadingChart';
import {getReadings, clearReadings} from './ReadingsStorage';
import {HeartRateReading} from './ReadingsTypes';

const C = {
  bgBase:    '#0e0e0e', // page background
  bgCard:    '#1a1a1a', // card surfaces
  bgRaised:  '#222222', // list item backgrounds, raised elements
  bgActive:  '#2a2a2a', // selected / active state
  border:    '#242424', // card and list borders
  textHi:    '#f0f0f0', // primary values — BPM numbers
  textMid:   '#cccccc', // card headings, entry titles
  textLo:    '#888888', // labels, hints, secondary info
  textFaint: '#444444', // axis labels, placeholders, muted notes
  red:       '#ff3c3c', // accent — BPM value
  green:     '#3cc864', // normal status
  amber:     '#ffb43c', // elevated status
} as const;

const S = {xs: 4, sm: 8, md: 12, lg: 16, xl: 20, xxl: 24} as const;
const R = {sm: 8, md: 12, lg: 16, xl: 20, xxl: 24, pill: 999} as const;

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

  const loadReadings = async () => {
    const data = await getReadings();
    setReadings(data);
  };

  useEffect(() => {
    loadReadings();
  }, []);

  const handleClearHistory = () => {
    Alert.alert(
      'Clear History',
      'Are you sure you want to delete all heart rate readings?',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Clear All',
          style: 'destructive',
          onPress: async () => {
            await clearReadings();
            setReadings([]);
          },
        },
      ],
    );
  };

  const latest = readings.length > 0 ? readings[readings.length - 1] : null;

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <View style={styles.pageHeader}></View>
        <Text style={styles.title}>Heart Rate Dashboard</Text>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Most Recent Reading</Text>
          {latest ? (
            <View style={styles.bpmDisplayContainer}>
              <Text style={styles.latestBpmNumber}>{latest.bpm}</Text>
              <Text style={styles.bpmUnitText}> BPM</Text>
              <View style={styles.latestMetaContainer}>
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
              </View>
            </View>
          ) : (
            <Text style={styles.emptyText}>No readings saved yet.</Text>
          )}
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Your Heart Rate Graph</Text>
          <ReadingsChart readings={readings} />
        </View>

        <View style={[styles.card, styles.historyCard]}>
          <View style={styles.historyHeader}>
            <Text style={styles.sectionTitle}>Reading History</Text>
            {readings.length > 0 && (
              <TouchableOpacity onPress={handleClearHistory}>
                <Text style={styles.clearButtonText}>Clear All</Text>
              </TouchableOpacity>
            )}
          </View>

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
          <View style={styles.buttonContent}>
            <Text style={styles.buttonIcon}>＋</Text>
            <Text style={styles.buttonText}>Log New Reading</Text>
          </View>
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
  pageHeader: {
    marginBottom: S.sm,
    marginTop: S.xxl,
  },
  title: {
    color: '#fff',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 14,
    marginTop: 28,
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
  historyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  sectionTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '700',
  },
  clearButtonText: {
    color: '#ff4a43',
    fontSize: 14,
    fontWeight: '600',
  },
  bpmDisplayContainer: {
    flexDirection: 'row',
    alignItems: 'baseline',
    flexWrap: 'wrap',
  },
  latestBpmNumber: {
    color: '#ff4a43',
    fontSize: 48,
    fontWeight: '800',
    fontFamily: 'DMMono-Medium',
  },
  bpmUnitText: {
    color: '#ff4a43',
    fontSize: 20,
    fontWeight: '600',
    marginLeft: 4,
    fontFamily: 'DMMono-Regular',
  },
  latestMetaContainer: {
    width: '100%',
    marginTop: 8,
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
    marginBottom: 14,
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonIcon: {
    color: '#fff',
    fontSize: 22,
    marginRight: 8,
    fontWeight: '700',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});
