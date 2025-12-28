import axios from 'axios'
export const api=axios.create({baseURL:import.meta.env.VITE_API_URL||'http://localhost:8080',withCredentials:true})

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const { response, config } = err;
    if (!response) throw err;

    // don’t loop
    const isAuthCall =
      config?.url?.includes("/api/auth/login") ||
      config?.url?.includes("/api/auth/refresh") ||
      config?.url?.includes("/api/auth/logout");

    if (response.status === 401 && !config._retry && !isAuthCall) {
      config._retry = true;
      try {
        await api.post("/api/auth/refresh");     // server sets new accessToken cookie
        return api(config);                     // retry original request
      } catch (e) {
        // refresh failed => user is truly logged out
        throw e;
      }
    }

    throw err;
  }
);
