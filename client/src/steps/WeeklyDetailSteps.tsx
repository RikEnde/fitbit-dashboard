import React from 'react';
import DailySteps from './DailySteps';
import './WeeklyDetailSteps.css';

// Define props for the component
interface WeeklyDetailStepsProps {
  weekNumber: string;
  startDate: string;
  endDate: string;
  onDayClick?: (date: string) => void;
  onBackClick?: () => void;
}

function WeeklyDetailSteps({ 
  weekNumber, 
  startDate, 
  endDate, 
  onDayClick, 
  onBackClick 
}: WeeklyDetailStepsProps) {
  return (
    <>
      <div className="back-button-container">
        <button onClick={onBackClick} className="back-button">
          &larr; Back to Weekly View
        </button>
      </div>
      <DailySteps 
        fromDate={startDate} 
        toDate={endDate} 
        title={`Daily Steps for Week ${weekNumber.substring(4)}, ${weekNumber.substring(0, 4)}`} 
        onDayClick={onDayClick}
      />
    </>
  );
}

export default WeeklyDetailSteps;