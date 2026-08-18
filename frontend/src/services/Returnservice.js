import api from './api';

export const requestReturn = async (orderItemId, reason) => {
  const response = await api.post(`/returns/items/${orderItemId}`, { reason });
  return response.data;
};

export const getMyReturnRequests = async () => {
  const response = await api.get('/returns/mine');
  return response.data;
};

// ---- Vendor ----

export const getVendorReturnRequests = async () => {
  const response = await api.get('/returns/vendor');
  return response.data;
};

export const approveReturn = async (returnRequestId, resolutionNote) => {
  const response = await api.put(`/returns/${returnRequestId}/approve`, { resolutionNote });
  return response.data;
};

export const rejectReturn = async (returnRequestId, resolutionNote) => {
  const response = await api.put(`/returns/${returnRequestId}/reject`, { resolutionNote });
  return response.data;
};

const returnService = {
  requestReturn,
  getMyReturnRequests,
  getVendorReturnRequests,
  approveReturn,
  rejectReturn,
};
export default returnService;