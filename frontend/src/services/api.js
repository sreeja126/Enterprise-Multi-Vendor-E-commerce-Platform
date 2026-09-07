import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8080/api",
});

API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

   if (token) {
    config.headers.Authorization = `Bearer ${token.trim()}`;
  }


  return config;
});

// Response interceptor: centralizes two things every single page would
// otherwise have to handle individually —
// 1. An expired/invalid session (401) logs the user out and sends them
//    back to login, instead of leaving them stuck staring at a broken
//    page with a dead token.
// 2. A network failure (backend unreachable, CORS issue, offline) gets a
//    clear, consistent message instead of axios's raw "Network Error" —
//    every page's existing `err.response?.data || 'fallback'` pattern
//    will now surface something readable for this case too, since
//    err.response is undefined for network errors but err.message isn't.
API.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || "";
    const isAuthEndpoint =
      requestUrl.includes("/auth/login") || requestUrl.includes("/auth/register");

    if (error.response?.status === 401 && !isAuthEndpoint) {
      const hadSession = Boolean(localStorage.getItem("token"));

      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("isVendor");
      localStorage.removeItem("viewAs");
      window.dispatchEvent(new Event("storage"));

      // Only force a redirect if there actually was a session that just
      // expired/became invalid — avoids bouncing a visitor who was never
      // logged in and simply hit something requiring auth.
      if (hadSession && window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    } else if (!error.response) {
      error.message =
        "Unable to reach the server. Please check your internet connection and try again.";
    }

    return Promise.reject(error);
  }
);

export default API;
