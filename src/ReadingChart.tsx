import React from 'react';
import {View, Text, Dimensions, StyleSheet} from 'react-native';
import {LineChart} from 'react-native-chart-kit';
import {HeartRateReading} from './ReadingsTypes';

type Props = {
  readings: HeartRateReading[];
};

function formatLabel(dateString: string): string {
  const date = new Date(dateString);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${month}/${day}`;
}

export default function ReadingsChart({readings}: Props) {
  if (readings.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No readings to graph yet.</Text>
      </View>
    );
  }

  const screenWidth = Dimensions.get('window').width;

  const labels = readings.map(reading => formatLabel(reading.recordedAt));
  const data = readings.map(reading => reading.bpm);

  return (
    <View style={styles.container}>
      <LineChart
        data={{
          labels,
          datasets: [
            {
              data,
            },
          ],
        }}
        width={screenWidth - 60}
        height={220}
        yAxisSuffix=" bpm"
        chartConfig={{
          backgroundColor: '#141416',
          backgroundGradientFrom: '#141416',
          backgroundGradientTo: '#141416',
          decimalPlaces: 0,
          color: opacity => `rgba(255, 74, 67, ${opacity})`,
          labelColor: opacity => `rgba(255, 255, 255, ${opacity})`,
          propsForDots: {
            r: '4',
            strokeWidth: '2',
            stroke: '#ff4a43',
          },
        }}
        bezier
        style={styles.chart}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
  },
  chart: {
    borderRadius: 20,
  },
  emptyContainer: {
    minHeight: 220,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    color: '#8a8a95',
    fontSize: 16,
  },
});