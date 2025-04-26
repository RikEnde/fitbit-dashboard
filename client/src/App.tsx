import React, { useState } from 'react';
import './App.css';
import Profile from './profile/Profile';
import Steps from './steps/Steps';
import HeartRate from './heartrate/HeartRate';

function App() {
    // State to track which component is currently selected
    const [selectedComponent, setSelectedComponent] = useState<'steps' | 'heartrate'>('steps');

    return (
        <div className="App">
            <header className="App-header">
                <h1>Fitbit Data Viewer</h1>
            </header>
            <div className="button-bar">
                <button 
                    className={`icon-button ${selectedComponent === 'steps' ? 'active' : ''}`}
                    onClick={() => setSelectedComponent('steps')}
                >
                    👟 Steps
                </button>
                <button 
                    className={`icon-button ${selectedComponent === 'heartrate' ? 'active' : ''}`}
                    onClick={() => setSelectedComponent('heartrate')}
                >
                    ❤️ Heart Rate
                </button>
            </div>
            <main className="App-main">
                <Profile/>
                {selectedComponent === 'steps' && <Steps/>}
                {selectedComponent === 'heartrate' && <HeartRate/>}
            </main>
        </div>
    );
}

export default App;
