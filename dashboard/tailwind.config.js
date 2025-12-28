/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js,svelte,ts}'],
	darkMode: 'class',
	theme: {
		extend: {
			colors: {
				// Fitbit color palette
				fitbit: {
					steps: '#00B0B9',
					calories: '#FF6B35',
					distance: '#7C3AED',
					active: '#22C55E',
					heartrate: '#EF4444',
					sleep: '#6366F1'
				},
				// Dark mode background colors
				dark: {
					bg: '#1a1a2e',
					card: '#252542',
					border: '#3d3d5c'
				}
			}
		}
	},
	plugins: []
};
