import React from 'react';
import DailySteps from './DailySteps';
import './DailyDetailSteps.css';

// Define props for the component
interface DailyDetailStepsProps {
  date: string;
  onDateChange?: (date: string) => void;
  onBackClick?: () => void;
}

function DailyDetailSteps({ 
  date, 
  onDateChange, 
  onBackClick 
}: DailyDetailStepsProps) {
  return (
    <>
      <div className="back-button-container">
        <button onClick={onBackClick} className="back-button">
          &larr; Back to Daily View
        </button>
      </div>
      <DailySteps 
        fromDate={`${date.split('T')[0]}T00:00:00Z`}
        toDate={`${date.split('T')[0]}T23:59:59Z`}
        title={`Steps for ${new Date(date).toLocaleDateString()}`}
        onDateChange={onDateChange}
      />
    </>
  );
}

export default DailyDetailSteps;