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

export interface UploadProgress {
    loaded: number;
    total: number;
    percent: number;
}

export function uploadZipFile(
    file: File,
    stats: string[],
    authHeaderValue: string | null,
    onProgress: (progress: UploadProgress) => void
): Promise<string> {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        formData.append('file', file);
        stats.forEach(s => formData.append('stats', s));

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/import');

        if (authHeaderValue) {
            xhr.setRequestHeader('Authorization', authHeaderValue);
        }

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                onProgress({
                    loaded: e.loaded,
                    total: e.total,
                    percent: Math.round((e.loaded / e.total) * 100)
                });
            }
        };

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    if (response.error) {
                        reject(new Error(response.error));
                    } else {
                        resolve(response.jobId);
                    }
                } catch {
                    reject(new Error('Invalid server response'));
                }
            } else {
                try {
                    const response = JSON.parse(xhr.responseText);
                    reject(new Error(response.error || `Upload failed: ${xhr.status}`));
                } catch {
                    reject(new Error(`Upload failed: ${xhr.status} ${xhr.statusText}`));
                }
            }
        };

        xhr.onerror = () => reject(new Error('Network error during upload'));
        xhr.send(formData);
    });
}
