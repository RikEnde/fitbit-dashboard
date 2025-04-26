import React, { useState } from 'react';
import DailySteps from './DailySteps';
import WeeklySteps from './WeeklySteps';
import WeeklyDetailSteps from './WeeklyDetailSteps';
import DailyDetailSteps from './DailyDetailSteps';

// Define the view options
type ViewType = 'daily' | 'weekly' | 'weeklyDetail' | 'dailyDetail';

function Steps() {
  // State to track the selected view, default to daily
  const [selectedView, setSelectedView] = useState<ViewType>('daily');

  // State to track the selected week's information
  const [selectedWeek, setSelectedWeek] = useState<{
    weekNumber: string;
    startDate: string;
    endDate: string;
  } | null>(null);

  // State to track the selected day's information
  const [selectedDay, setSelectedDay] = useState<{
    date: string;
  } | null>(null);

  // Handle view selection change
  const handleViewChange = (view: ViewType) => {
    setSelectedView(view);
    // Reset selected week and day when switching to a different view
    if (view !== 'weeklyDetail') {
      setSelectedWeek(null);
    }
    if (view !== 'dailyDetail') {
      setSelectedDay(null);
    }
  };

  // Handle week selection
  const handleWeekClick = (weekNumber: string, startDate: string, endDate: string) => {
    setSelectedWeek({ weekNumber, startDate, endDate });
    setSelectedView('weeklyDetail');
  };

  // Handle day selection
  const handleDayClick = (date: string) => {
    setSelectedDay({ date });
    setSelectedView('dailyDetail');
  };

  // Handle date change in daily detail view
  const handleDateChange = (date: string) => {
    // Format the date with time component to match the expected format
    const formattedDate = `${date}T12:00:00Z`;
    setSelectedDay({ date: formattedDate });
  };

  return (
    <div className="steps-container">
      <div className="view-selector">
        <span className="view-label">View: </span>
        <div className="view-buttons">
          <button 
            className={`view-button ${selectedView === 'daily' ? 'active' : ''}`}
            onClick={() => handleViewChange('daily')}
          >
            Daily
          </button>
          <button 
            className={`view-button ${selectedView === 'weekly' ? 'active' : ''}`}
            onClick={() => handleViewChange('weekly')}
          >
            Weekly
          </button>
        </div>
      </div>

      {/* Render the appropriate component based on the selected view */}
      {selectedView === 'daily' && <DailySteps onDayClick={handleDayClick} />}
      {selectedView === 'weekly' && <WeeklySteps onWeekClick={handleWeekClick} />}
      {selectedView === 'weeklyDetail' && selectedWeek && (
        <WeeklyDetailSteps
          weekNumber={selectedWeek.weekNumber}
          startDate={selectedWeek.startDate}
          endDate={selectedWeek.endDate}
          onDayClick={handleDayClick}
          onBackClick={() => handleViewChange('weekly')}
        />
      )}
      {selectedView === 'dailyDetail' && selectedDay && (
        <DailyDetailSteps
          date={selectedDay.date}
          onDateChange={handleDateChange}
          onBackClick={() => handleViewChange('daily')}
        />
      )}
    </div>
  );
}

export default Steps;