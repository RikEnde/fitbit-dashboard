import React from 'react';
import { gql, useQuery } from '@apollo/client';

// Get profile ID from environment variable or use default if not available
const profileId = process.env.REACT_APP_PROFILE_ID || "???";

// Define the GraphQL query to fetch the profile with ID from environment variable
const GET_PROFILE = gql`
    query GetProfile {
        profile(id: "${profileId}") {
            id
            aboutMe
            fullName
            displayName
            emailAddress
            avatar
        }
    }
`;

// Define the type for our profile data
interface ProfileData {
  profile: {
    id: string;
    aboutMe: string;
    displayName: string;
    fullName: string;
    emailAddress: string;
    avatar: string;
  } | null;
}

function Profile() {
  // Execute the query
  const { loading, error, data } = useQuery<ProfileData>(GET_PROFILE);

  return (
    <>
      {loading && <p>Loading profile data...</p>}
      {error && (
        <div className="error-message">
          <p>Error loading profile: {error.message}</p>
        </div>
      )}
      {data && data.profile && (
        <div className="profile-container">
          <h2>Profile Information</h2>
          <div className="profile-info">
            {data.profile.avatar && (
              <div className="avatar-container">
                <img 
                  src={`data:image/jpeg;base64,${data.profile.avatar}`} 
                  alt="Profile Avatar" 
                  className="profile-avatar" 
                />
              </div>
            )}
            <p><strong>Display name:</strong> {data.profile.displayName}</p>
            <p><strong>About me:</strong> {data.profile.aboutMe}</p>
            <p><strong>Full name:</strong> {data.profile.fullName}</p>
            <p><strong>Email address:</strong> {data.profile.emailAddress}</p>
          </div>
        </div>
      )}
    </>
  );
}

export default Profile;
