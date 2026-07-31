import axios from "axios";

const API_URL = "http://localhost:8080/api/users";

const getToken = () => {
  const token = localStorage.getItem("token");
  console.log("JWT TOKEN:", token);
  return token;
};

export const getProfile = async () => {
  return await axios.get(`${API_URL}/profile`, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });
};

export const updateProfile = async (userData) => {
  return await axios.put(`${API_URL}/profile`, userData, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });
};
