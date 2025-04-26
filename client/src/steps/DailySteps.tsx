import React, { useState } from 'react';
import { gql, useQuery } from '@apollo/client';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import './DailySteps.css';

// Define the GraphQL query for daily steps sum
const GET_DAILY_STEPS = gql`
  query GetDailySteps($from: DateTime!, $to: DateTime!) {
    dailyStepsSum(range: {
      from: $from,
      to: $to
    }) {
      date
      totalSteps
    }
  }
`;

// Define the GraphQL query for steps (used for specific date)
const GET_STEPS = gql`
  query GetSteps($from: DateTime!, $to: DateTime!, $limit: Int!, $offset: Int!) {
    steps(range: {
      from: $from,
      to: $to
    }, limit: $limit, offset: $offset) {
      id
      value
      dateTime
    }
  }
`;

// Define the type for our daily steps data
interface DailyStepsSumData {
  dailyStepsSum: Array<{
    date: string;
    totalSteps: number;
  }>;
}

// Define the type for our steps data
interface StepsData {
  steps: Array<{
    id: number;
    value: number;
    dateTime: string;
  }>;
}

// Define props for the component
interface DailyStepsProps {
  fromDate?: string;
  toDate?: string;
  title?: string;
  onDayClick?: (date: string) => void;
  onDateChange?: (date: string) => void;
}

// Format date for display on x-axis
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return `${date.getMonth() + 1}/${date.getDate()}`;
};

function DailySteps({ fromDate, toDate, title = "Daily Steps", onDayClick, onDateChange }: DailyStepsProps) {
  // Set default date range if not provided
  const from = fromDate || "2024-01-01T00:00:00Z";
  const to = toDate || "2024-12-31T23:59:59Z";

  // Determine if we're displaying a specific date
  const isSpecificDate = !!(fromDate && toDate && 
    fromDate.split('T')[0] === toDate.split('T')[0]);

  // Extract the date part for the date picker
  const [currentDate, setCurrentDate] = useState<string>(
    isSpecificDate ? fromDate!.split('T')[0] : ""
  );

  // Execute the appropriate query
  const dailyStepsQuery = useQuery<DailyStepsSumData>(GET_DAILY_STEPS, {
    variables: { from, to },
    skip: isSpecificDate // Skip this query if we're displaying a specific date
  });

  const stepsQuery = useQuery<StepsData>(GET_STEPS, {
    variables: { from, to, limit: 1000, offset: 0 },
    skip: !isSpecificDate // Skip this query if we're not displaying a specific date
  });

  // Combine loading and error states
  const loading = isSpecificDate ? stepsQuery.loading : dailyStepsQuery.loading;
  const error = isSpecificDate ? stepsQuery.error : dailyStepsQuery.error;

  // Handle bar click
  const handleBarClick = (data: any) => {
    if (onDayClick) {
      onDayClick(data.date);
    }
  };

  // Handle navigation to previous day
  const handlePreviousDay = () => {
    if (isSpecificDate && currentDate) {
      const date = new Date(currentDate);
      date.setDate(date.getDate() - 1);
      const newDate = date.toISOString().split('T')[0];
      setCurrentDate(newDate);
      if (onDateChange) {
        onDateChange(newDate);
      }
    }
  };

  // Handle navigation to next day
  const handleNextDay = () => {
    if (isSpecificDate && currentDate) {
      const date = new Date(currentDate);
      date.setDate(date.getDate() + 1);
      const newDate = date.toISOString().split('T')[0];
      setCurrentDate(newDate);
      if (onDateChange) {
        onDateChange(newDate);
      }
    }
  };

  // Handle date picker change
  const handleDateChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newDate = event.target.value;
    setCurrentDate(newDate);
    if (onDateChange) {
      onDateChange(newDate);
    }
  };

  // Prepare data for the chart
  let chartData: Array<{date: string, steps: number, formattedDate: string}> = [];

  if (isSpecificDate && stepsQuery.data) {
    // Process steps data for a specific date
    // Group steps by hour
    const hourlySteps: {[hour: string]: number} = {};

    stepsQuery.data.steps.forEach(step => {
      const dateTime = new Date(step.dateTime);
      const hour = dateTime.getHours().toString().padStart(2, '0');
      const hourKey = `${dateTime.toISOString().split('T')[0]}T${hour}:00:00Z`;

      if (!hourlySteps[hourKey]) {
        hourlySteps[hourKey] = 0;
      }
      hourlySteps[hourKey] += step.value;
    });

    // Convert hourlySteps object to array and sort by hour
    chartData = Object.entries(hourlySteps)
      .map(([dateTime, steps]) => {
        const hour = new Date(dateTime).getHours();
        return {
          date: dateTime,
          steps,
          hour,  // Store the numeric hour for sorting
          formattedDate: `${hour}:00`
        };
      })
      .sort((a, b) => a.hour - b.hour);
  } else if (dailyStepsQuery.data) {
    // Process daily steps sum data
    chartData = dailyStepsQuery.data.dailyStepsSum
      .map(item => ({
        date: item.date,
        steps: item.totalSteps,
        formattedDate: formatDate(item.date)
      }))
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
  }

  return (
    <>
      {loading && <p>Loading steps data...</p>}
      {error && (
        <div className="error-message">
          <p>Error loading steps data: {error.message}</p>
        </div>
      )}
      {chartData.length > 0 && (
        <div className="daily-steps-container">
          <h2>{title}</h2>
          {isSpecificDate && (
            <div className="date-navigation">
              <button 
                className="nav-button prev-button" 
                onClick={handlePreviousDay}
                aria-label="Previous Day"
              >
                &larr; Previous Day
              </button>
              <input 
                type="date" 
                value={currentDate}
                onChange={handleDateChange}
                className="date-picker"
              />
              <button 
                className="nav-button next-button" 
                onClick={handleNextDay}
                aria-label="Next Day"
              >
                Next Day &rarr;
              </button>
            </div>
          )}
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 50 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="formattedDate" 
                  label={{ value: 'Date', position: 'insideBottom', offset: -10 }}
                  angle={-45}
                  textAnchor="end"
                />
                <YAxis 
                  label={{ value: 'Steps', angle: -90, position: 'insideLeft' }}
                />
                <Tooltip 
                  formatter={(value) => [`${value} steps`, 'Steps']}
                  labelFormatter={(label) => `Date: ${label}`}
                />
                <Bar 
                  dataKey="steps" 
                  fill="#8884d8" 
                  name="Steps" 
                  onClick={handleBarClick}
                  cursor="pointer"
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </>
  );
}

export default DailySteps;