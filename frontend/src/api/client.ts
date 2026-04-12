import axios from 'axios';

/**
 * Module-level credentials store.
 * Updated by AuthContext on login/logout so the request interceptor
 * can inject the Authorization header without touching React state.
 *
 * Pattern: this acts as the "subject" side of the Observer relationship —
 * AuthContext observes login events and pushes updates here.
 */
const credentialsStore = { encoded: '' };

export const credentialsSetter = {
  set(username: string, password: string) {
    credentialsStore.encoded = btoa(`${username}:${password}`);
  },
  clear() {
    credentialsStore.encoded = '';
  },
};

/** Pre-configured Axios instance scoped to /api */
const client = axios.create({ baseURL: '/api' });

/** Inject Basic Auth header on every outgoing request */
client.interceptors.request.use((config) => {
  if (credentialsStore.encoded) {
    config.headers['Authorization'] = `Basic ${credentialsStore.encoded}`;
  }
  return config;
});

export default client;
