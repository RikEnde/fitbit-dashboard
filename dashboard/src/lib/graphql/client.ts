import {cacheExchange, Client, fetchExchange} from '@urql/svelte';
import {get} from 'svelte/store';
import {authHeader} from '$stores/auth';

export const client = new Client({
	url: '/graphql',
	fetchOptions: () => {
		const header = get(authHeader);
		return header ? {headers: {Authorization: header}} : {};
	},
	exchanges: [cacheExchange, fetchExchange]
});
