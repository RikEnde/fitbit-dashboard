import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export interface TileLayout {
	id: string;
	x: number;
	y: number;
	w: number;
	h: number;
	visible: boolean;
}

export interface Preferences {
	theme: 'dark' | 'light';
	tileLayout: TileLayout[];
	visibleTiles: string[];
}

const defaultPreferences: Preferences = {
	theme: 'dark',
	tileLayout: [
		{ id: 'steps', x: 0, y: 0, w: 1, h: 1, visible: true },
		{ id: 'calories', x: 1, y: 0, w: 1, h: 1, visible: true },
		{ id: 'distance', x: 2, y: 0, w: 1, h: 1, visible: true },
		{ id: 'active-minutes', x: 3, y: 0, w: 1, h: 1, visible: true },
		{ id: 'heart-rate', x: 0, y: 1, w: 2, h: 1, visible: true },
		{ id: 'sleep', x: 2, y: 1, w: 2, h: 1, visible: true }
	],
	visibleTiles: ['steps', 'calories', 'distance', 'active-minutes', 'heart-rate', 'sleep']
};

function loadPreferences(): Preferences {
	if (!browser) return defaultPreferences;

	const saved = localStorage.getItem('fitbit-dashboard-preferences');
	if (saved) {
		try {
			return { ...defaultPreferences, ...JSON.parse(saved) };
		} catch {
			return defaultPreferences;
		}
	}
	return defaultPreferences;
}

function createPreferencesStore() {
	const { subscribe, set, update } = writable<Preferences>(loadPreferences());

	return {
		subscribe,
		set: (value: Preferences) => {
			if (browser) {
				localStorage.setItem('fitbit-dashboard-preferences', JSON.stringify(value));
			}
			set(value);
		},
		update: (updater: (value: Preferences) => Preferences) => {
			update((current) => {
				const updated = updater(current);
				if (browser) {
					localStorage.setItem('fitbit-dashboard-preferences', JSON.stringify(updated));
				}
				return updated;
			});
		},
		toggleTheme: () => {
			update((prefs) => {
				const newTheme = prefs.theme === 'dark' ? 'light' : 'dark';
				const updated = { ...prefs, theme: newTheme };
				if (browser) {
					localStorage.setItem('fitbit-dashboard-preferences', JSON.stringify(updated));
					document.documentElement.classList.toggle('dark', newTheme === 'dark');
				}
				return updated;
			});
		},
		toggleTile: (tileId: string) => {
			update((prefs) => {
				const visible = prefs.visibleTiles.includes(tileId);
				const visibleTiles = visible
					? prefs.visibleTiles.filter((id) => id !== tileId)
					: [...prefs.visibleTiles, tileId];
				const updated = { ...prefs, visibleTiles };
				if (browser) {
					localStorage.setItem('fitbit-dashboard-preferences', JSON.stringify(updated));
				}
				return updated;
			});
		},
		reset: () => {
			if (browser) {
				localStorage.removeItem('fitbit-dashboard-preferences');
			}
			set(defaultPreferences);
		}
	};
}

export const preferences = createPreferencesStore();
