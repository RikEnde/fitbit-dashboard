import React, { useState } from 'react';
import { gql, useQuery } from '@apollo/client';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

// Define the GraphQL query for heart rates per interval
const GET_HEART_RATES_PER_INTERVAL = gql`
  query GetHeartRatesPerInterval($from: DateTime!, $to: DateTime!) {
    heartRatesPerInterval(range: {
      from: $from,
      to: $to
    }) {
      timeInterval
      bpmSum
    }
  }
`;

// Define the type for our heart rate data
interface HeartRatesPerIntervalData {
  heartRatesPerInterval: Array<{
    timeInterval: string;
    bpmSum: number;
  }>;
}

// Define props for the component
interface MonthlyHeartRateProps {
  onDateSelect: (date: string) => void;
}

// Format date for display on x-axis
const formatDateTime = (dateTimeString: string) => {
  const date = new Date(dateTimeString);
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// Format date for tooltip
const formatDateForTooltip = (dateTimeString: string) => {
  const date = new Date(dateTimeString);
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
};

function MonthlyHeartRate({ onDateSelect }: MonthlyHeartRateProps) {
  // State to track the current month
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date());

  // Calculate the start and end of the current month
  const startOfMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
  const endOfMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0, 23, 59, 59);

  // Format dates for GraphQL query
  const fromDate = startOfMonth.toISOString();
  const toDate = endOfMonth.toISOString();

  // Execute the query
  const { loading, error, data } = useQuery<HeartRatesPerIntervalData>(
    GET_HEART_RATES_PER_INTERVAL,
    {
      variables: { from: fromDate, to: toDate }
    }
  );

  // Prepare data for the chart
  const chartData = data?.heartRatesPerInterval.map(item => ({
    timeInterval: item.timeInterval,
    bpmSum: item.bpmSum,
    formattedTime: formatDateTime(item.timeInterval)
  })) || [];

  // Handle month navigation
  const handlePreviousMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
  };

  // Handle click on a data point to select a date
  const handleDataPointClick = (data: any) => {
    if (data && data.activePayload && data.activePayload.length > 0) {
      const clickedData = data.activePayload[0].payload;
      onDateSelect(clickedData.timeInterval.split('T')[0]); // Extract just the date part
    }
  };

  return (
    <>
      {loading && <p>Loading heart rate data...</p>}
      {error && (
        <div className="error-message">
          <p>Error loading heart rate data: {error.message}</p>
        </div>
      )}
      {data && (
        <div className="monthly-heart-rate-container">
          <h2>Monthly Heart Rate</h2>
          <div className="month-navigation">
            <button onClick={handlePreviousMonth}>&lt; Previous Month</button>
            <span>{currentMonth.toLocaleString('default', { month: 'long', year: 'numeric' })}</span>
            <button onClick={handleNextMonth}>Next Month &gt;</button>
          </div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={400}>
              <LineChart 
                data={chartData} 
                margin={{ top: 20, right: 30, left: 20, bottom: 50 }}
                onClick={handleDataPointClick}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="formattedTime" 
                  label={{ value: 'Date/Time', position: 'insideBottom', offset: -10 }}
                  angle={-45}
                  textAnchor="end"
                />
                <YAxis 
                  label={{ value: 'Heart Rate Sum', angle: -90, position: 'insideLeft' }}
                />
                <Tooltip 
                  formatter={(value) => [`${value}`, 'Heart Rate Sum']}
                  labelFormatter={(label, payload: any) => {
                    if (payload && payload.length > 0) {
                      return formatDateForTooltip(payload[0].payload.timeInterval);
                    }
                    return label;
                  }}
                />
                <Line 
                  type="monotone" 
                  dataKey="bpmSum" 
                  stroke="#8884d8" 
                  activeDot={{ r: 8 }} 
                  name="Heart Rate Sum" 
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <p className="help-text">Click on a data point to view detailed heart rate for that day.</p>
        </div>
      )}
    </>
  );
}

export default MonthlyHeartRate;