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
				// Theme colors (resolved via CSS variables for light/dark mode)
				theme: {
					bg: 'var(--theme-bg)',
					card: 'var(--theme-card)',
					border: 'var(--theme-border)',
					hover: 'var(--theme-hover)',
					text: 'var(--theme-text)',
					'text-secondary': 'var(--theme-text-secondary)',
					'text-muted': 'var(--theme-text-muted)',
					'text-dim': 'var(--theme-text-dim)',
					'text-bright': 'var(--theme-text-bright)'
				}
			}
		}
	},
	plugins: []
};
