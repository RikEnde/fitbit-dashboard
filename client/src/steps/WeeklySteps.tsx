import React from 'react';
import { gql, useQuery } from '@apollo/client';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

// Define the GraphQL query for weekly steps average
const GET_WEEKLY_STEPS = gql`
  query GetWeeklySteps {
    weeklyStepsAverage(range: {
      from: "2024-01-01T00:00:00Z",
      to: "2024-12-31T23:59:59Z"
    }) {
      weekNumber
      averageSteps
    }
  }
`;

// Define the type for our weekly steps data
interface WeeklyStepsAverageData {
  weeklyStepsAverage: Array<{
    weekNumber: string;
    averageSteps: number;
  }>;
}

// Define props for the component
interface WeeklyStepsProps {
  onWeekClick?: (weekNumber: string, startDate: string, endDate: string) => void;
}

// Format week number for display on x-axis
const formatWeek = (weekNumber: string) => {
  // Extract year and week from the weekNumber (format: YYYYWW)
  const year = parseInt(weekNumber.substring(0, 4));
  const week = parseInt(weekNumber.substring(4));

  // Calculate the first day of the year
  const firstDayOfYear = new Date(year, 0, 1);

  // Calculate the first day of the week
  // Week 1 is the week with the first Thursday of the year
  // To calculate: Add (week - 1) * 7 days to the first day of the year,
  // then adjust to the nearest Monday (day 1)
  const dayOfWeek = firstDayOfYear.getDay(); // 0 = Sunday, 1 = Monday, etc.
  const daysToAdd = (week - 1) * 7 + (dayOfWeek <= 4 ? 1 - dayOfWeek : 8 - dayOfWeek);

  const weekDate = new Date(year, 0, 1 + daysToAdd);

  // Array of month names
  const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

  return months[weekDate.getMonth()];
};

// Calculate the start and end dates of a week
const getWeekDateRange = (weekNumber: string) => {
  // Extract year and week from the weekNumber (format: YYYYWW)
  const year = parseInt(weekNumber.substring(0, 4));
  const week = parseInt(weekNumber.substring(4));

  // Calculate the first day of the year
  const firstDayOfYear = new Date(year, 0, 1);

  // Calculate the first day of the week (Monday)
  const dayOfWeek = firstDayOfYear.getDay(); // 0 = Sunday, 1 = Monday, etc.
  const daysToAdd = (week - 1) * 7 + (dayOfWeek <= 4 ? 1 - dayOfWeek : 8 - dayOfWeek);

  // Start date is Monday of the week at 00:00:00 UTC
  const startDate = new Date(Date.UTC(year, 0, 1 + daysToAdd, 0, 0, 0));

  // End date is Sunday of the week at 23:59:59 UTC
  const endDate = new Date(Date.UTC(year, 0, 1 + daysToAdd + 6, 23, 59, 59));

  return {
    start: startDate.toISOString(),
    end: endDate.toISOString()
  };
};

function WeeklySteps({ onWeekClick }: WeeklyStepsProps) {
  // Execute the query
  const { loading, error, data } = useQuery<WeeklyStepsAverageData>(GET_WEEKLY_STEPS);

  // Handle bar click
  const handleBarClick = (data: any) => {
    if (onWeekClick) {
      const { weekNumber } = data;
      const { start, end } = getWeekDateRange(weekNumber);
      onWeekClick(weekNumber, start, end);
    }
  };

  // Prepare data for the chart
  const chartData = data?.weeklyStepsAverage.map(item => ({
    weekNumber: item.weekNumber,
    steps: item.averageSteps,
    formattedWeek: formatWeek(item.weekNumber)
  })) || [];

  return (
    <>
      {loading && <p>Loading weekly steps data...</p>}
      {error && (
        <div className="error-message">
          <p>Error loading weekly steps data: {error.message}</p>
        </div>
      )}
      {data && (
        <div className="weekly-steps-container">
          <h2>Weekly Steps Average</h2>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 50 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="formattedWeek" 
                  label={{ value: 'Week', position: 'insideBottom', offset: -10 }}
                />
                <YAxis 
                  label={{ value: 'Average Steps', angle: -90, position: 'insideLeft' }}
                />
                <Tooltip 
                  formatter={(value) => [`${Math.round(value as number)} steps`, 'Average Steps']}
                  labelFormatter={(label) => `${label}`}
                />
                <Bar 
                  dataKey="steps" 
                  fill="#82ca9d" 
                  name="Average Steps" 
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

export default WeeklySteps;