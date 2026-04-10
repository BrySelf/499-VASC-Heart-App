export type ActivityType =
  | 'Resting'
  | 'Walking'
  | 'Running'
  | 'After Exercise'
  | 'Stressed'
  | 'Sleeping'
  | 'Other';

export type HeartRateReading = {
  id: string;
  bpm: number;
  recordedAt: string;
  activityType: ActivityType;
  notes?: string;
};