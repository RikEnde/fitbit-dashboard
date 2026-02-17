import {writable} from 'svelte/store';

export interface StatResult {
    fileCount: number;
}

export interface UserResult {
    user: string;
    stats: Record<string, StatResult>;
}

export interface ImportResponse {
    results: UserResult[];
}

export interface ImportJobState {
    jobId: string;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED';
    message: string | null;
    results: ImportResponse | null;
    error: string | null;
}

export const importJob = writable<ImportJobState | null>(null);
