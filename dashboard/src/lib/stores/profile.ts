import { writable } from 'svelte/store';

export interface Profile {
	id: string;
	fullName: string;
	displayName: string;
	firstName: string;
	lastName: string;
	emailAddress: string;
	dateOfBirth: string;
	memberSince: string;
	gender: string;
	height: number;
	weight: number;
	strideLengthWalking: number;
	strideLengthRunning: number;
	weightUnit: string;
	distanceUnit: string;
	heightUnit: string;
	timezone: string;
	avatar: string | null;
}

export const profile = writable<Profile | null>(null);
export const profileLoading = writable<boolean>(false);
export const profileError = writable<string | null>(null);
