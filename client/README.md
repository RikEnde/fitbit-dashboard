# Fitbit Data Viewer

This is a React + TypeScript application that retrieves data from a GraphQL API to display Fitbit profile information.

## Features

- Fetches profile data from the GraphQL API
- Displays profile information (display name and email address)
- Displays steps data with a selector to switch between:
  - Daily steps data in a bar graph visualization
  - Weekly average steps data in a bar graph visualization
  - Detailed views for both daily and weekly steps
- Displays heart rate data with a selector to switch between:
  - Daily heart rate data visualization
  - Monthly heart rate data visualization
- Uses Apollo Client for GraphQL integration
- Built with React and TypeScript
- Uses Recharts for data visualization

## Getting Started

### Prerequisites

- Node.js (v14 or later)
- npm (v6 or later)

### Installation

1. Navigate to the client directory:
   ```
   cd client
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Install Recharts library for the DailySteps component:
   ```
   npm install recharts
   ```

4. Start the development server:
   ```
   npm start
   ```

The application will be available at [http://localhost:3000](http://localhost:3000).

## How It Works

The application connects to the GraphQL API at `/graphql` (proxied to the backend server) and:

1. Fetches the profile with ID from environment variable and displays the profile's information.
2. Fetches steps data for the date range 2024-01-01 to 2024-12-31 and displays it as a bar graph:
   - Daily steps view: Shows dates on the x-axis and step counts on the y-axis
   - Weekly steps view: Shows week numbers on the x-axis and average step counts on the y-axis
   - Detailed views: Provides more granular information for both daily and weekly steps
3. Fetches heart rate data and displays it:
   - Daily heart rate view: Shows heart rate data throughout a day
   - Monthly heart rate view: Shows heart rate trends over a month
4. Allows users to switch between different views using dropdown selectors for each data type.

## Project Structure

- `public/` - Static assets
- `src/` - Source code
  - `App.tsx` - Main application component that integrates all components
  - `index.tsx` - Application entry point with Apollo Client setup
  - `profile/` - Profile-related components
    - `Profile.tsx` - Component for displaying profile information
  - `steps/` - Steps data visualization components
    - `Steps.tsx` - Component with selector to switch between daily and weekly steps views
    - `DailySteps.tsx` - Component for displaying daily steps data as a bar graph
    - `WeeklySteps.tsx` - Component for displaying weekly average steps data as a bar graph
    - `DailyDetailSteps.tsx` - Component for displaying detailed daily steps information
    - `WeeklyDetailSteps.tsx` - Component for displaying detailed weekly steps information
  - `heartrate/` - Heart rate data visualization components
    - `HeartRate.tsx` - Component with selector to switch between daily and monthly heart rate views
    - `DailyHeartRate.tsx` - Component for displaying daily heart rate data
    - `MonthlyHeartRate.tsx` - Component for displaying monthly heart rate data
  - `*.css` - Styling files for components
