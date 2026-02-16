import {writable, derived} from 'svelte/store';
import {browser} from '$app/environment';

interface Credentials {
	username: string;
	password: string;
}

const storedCredentials: Credentials | null = browser
	? (() => {
		const saved = sessionStorage.getItem('fitbit-auth');
		if (saved) {
			try {
				return JSON.parse(saved);
			} catch {
				return null;
			}
		}
		return null;
	})()
	: null;

export const credentials = writable<Credentials | null>(storedCredentials);

export const authHeader = derived(credentials, ($credentials) => {
	if (!$credentials) return null;
	const encoded = btoa(`${$credentials.username}:${$credentials.password}`);
	return `Basic ${encoded}`;
});

export function login(username: string, password: string) {
	const creds = {username, password};
	if (browser) {
		sessionStorage.setItem('fitbit-auth', JSON.stringify(creds));
	}
	credentials.set(creds);
}

export function logout() {
	if (browser) {
		sessionStorage.removeItem('fitbit-auth');
		window.location.href = '/';
	}
	credentials.set(null);
}
