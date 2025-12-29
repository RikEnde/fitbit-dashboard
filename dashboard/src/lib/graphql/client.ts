import {cacheExchange, Client, fetchExchange} from '@urql/svelte';

export const client = new Client({
	url: '/graphql',
	exchanges: [cacheExchange, fetchExchange]
});
