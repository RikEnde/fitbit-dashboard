import React, { useState } from 'react';
import { gql, useQuery } from '@apollo/client';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

// Define the GraphQL query for heart rates
const GET_HEART_RATES = gql`
  query GetHeartRates($from: DateTime!, $to: DateTime!) {
    heartRates(range: {
      from: $from,
      to: $to
    }, limit: 20000, offset: 0) {
      id
      bpm
      confidence
      time
    }
  }
`;

// Define the type for our heart rate data
interface HeartRatesData {
  heartRates: Array<{
    id: string;
    bpm: number;
    confidence: number;
    time: string;
  }>;
}

// Define props for the component
interface DailyHeartRateProps {
  selectedDate: string | null;
}

// Format time for display on x-axis
const formatTime = (dateTimeString: string) => {
  // Convert to US Eastern Time
  const date = new Date(dateTimeString);
  const options: Intl.DateTimeFormatOptions = { timeZone: 'America/New_York', hour: '2-digit', hour12: false };
  return new Intl.DateTimeFormat('en-US', options).format(date);
};

function DailyHeartRate({ selectedDate }: DailyHeartRateProps) {
  // State to track the current date
  const [currentDate, setCurrentDate] = useState<string>(selectedDate || new Date().toISOString().split('T')[0]);

  // Calculate the start and end of the selected day
  const startOfDay = new Date(`${currentDate}T00:00:00Z`);
  const endOfDay = new Date(`${currentDate}T23:59:59Z`);

  // Format dates for GraphQL query
  const fromDate = startOfDay.toISOString();
  const toDate = endOfDay.toISOString();

  // Execute the query
  const { loading, error, data } = useQuery<HeartRatesData>(
    GET_HEART_RATES,
    {
      variables: { from: fromDate, to: toDate }
    }
  );

  // Prepare data for the chart
  const chartData = React.useMemo(() => {
    if (!data?.heartRates.length) return [];

    // Use raw data points to show peaks
    return data.heartRates.map(item => {
      const date = new Date(item.time);
      const options: Intl.DateTimeFormatOptions = { 
        timeZone: 'America/New_York', 
        hour: '2-digit', 
        minute: '2-digit', 
        hour12: false 
      };
      const formattedTime = new Intl.DateTimeFormat('en-US', options).format(date);

      return {
        time: item.time,
        bpm: item.bpm,
        formattedTime: formattedTime
      };
    }).sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime());
  }, [data]);

  // Handle date navigation
  const handlePreviousDay = () => {
    const prevDay = new Date(currentDate);
    prevDay.setDate(prevDay.getDate() - 1);
    setCurrentDate(prevDay.toISOString().split('T')[0]);
  };

  const handleNextDay = () => {
    const nextDay = new Date(currentDate);
    nextDay.setDate(nextDay.getDate() + 1);
    setCurrentDate(nextDay.toISOString().split('T')[0]);
  };

  // Format the current date for display
  const formattedDate = new Date(currentDate).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <>
      {loading && <p>Loading heart rate data...</p>}
      {error && (
        <div className="error-message">
          <p>Error loading heart rate data: {error.message}</p>
        </div>
      )}
      <div className="daily-heart-rate-container">
        <h2>Daily Heart Rate</h2>
        <div className="date-navigation">
          <button onClick={handlePreviousDay}>&lt; Previous Day</button>
          <span>{formattedDate}</span>
          <button onClick={handleNextDay}>Next Day &gt;</button>
        </div>
        {data && data.heartRates.length > 0 ? (
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={400}>
              <LineChart 
                data={chartData} 
                margin={{ top: 20, right: 30, left: 20, bottom: 50 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="formattedTime" 
                  label={{ value: 'Time (US Eastern)', position: 'insideBottom', offset: -10 }}
                  interval="preserveStartEnd"  // Only show start and end ticks to avoid overcrowding
                />
                <YAxis 
                  label={{ value: 'BPM', angle: -90, position: 'insideLeft' }}
                  domain={['dataMin - 10', 'dataMax + 10']}
                />
                <Tooltip 
                  formatter={(value) => [`${value} bpm`, 'Heart Rate']}
                  labelFormatter={(label) => {
                    return `Time (US Eastern): ${label}`;
                  }}
                />
                <Line 
                  type="linear" 
                  dataKey="bpm" 
                  stroke="#ff7300" 
                  dot={false} 
                  name="Heart Rate" 
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="no-data-message">
            <p>No heart rate data available for this day.</p>
          </div>
        )}
      </div>
    </>
  );
}

export default DailyHeartRate;