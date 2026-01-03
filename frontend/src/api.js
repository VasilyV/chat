import axios from 'axios';

    const envBase = import.meta.env.VITE_API_URL;

// If env var is undefined (not present), default to dev backend.
// If env var is '' (empty string), keep it (same-origin).
const baseURL = envBase === undefined ? 'http://localhost:8080' : envBase;

export const api = axios.create({
  baseURL,
  withCredentials: true,
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const { response, config } = err;
    if (!response) throw err;

    // don’t loop
    const isAuthCall =
      config?.url?.includes('/api/auth/login') ||
      config?.url?.includes('/api/auth/refresh') ||
      config?.url?.includes('/api/auth/logout');

    if (response.status === 401 && !config._retry && !isAuthCall) {
      config._retry = true;
      try {
        await api.post('/api/auth/refresh');     // server sets new accessToken cookie
        return api(config);                     // retry original request
      } catch (e) {
        // refresh failed => user is truly logged out
        throw e;
      }
    }

    throw err;
  }
);
