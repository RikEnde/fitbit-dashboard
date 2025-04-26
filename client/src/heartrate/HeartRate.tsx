import React, { useState } from 'react';
import MonthlyHeartRate from './MonthlyHeartRate';
import DailyHeartRate from './DailyHeartRate';

function HeartRate() {
  // State to track the selected date
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  // Handle date selection
  const handleDateSelect = (date: string) => {
    setSelectedDate(date);
  };

  return (
    <div className="heart-rate-container">
      {selectedDate ? (
        <>
          <div className="back-button-container">
            <button 
              onClick={() => setSelectedDate(null)} 
              className="back-button"
            >
              &larr; Back to Monthly View
            </button>
          </div>
          <DailyHeartRate selectedDate={selectedDate} />
        </>
      ) : (
        <MonthlyHeartRate onDateSelect={handleDateSelect} />
      )}
    </div>
  );
}

export default HeartRate;