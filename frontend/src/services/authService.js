import API from "./api";

export const registerUser = (userData) =>
  API.post("/auth/register", userData);

export const loginUser = (loginData) =>
  API.post("/auth/login", loginData);