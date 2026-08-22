import api from './api';

export const getDashboardSummary = async () => {
  const response = await api.get('/admin/dashboard/summary');
  return response.data;
};

export const getAdminVendors = async () => {
  const response = await api.get('/admin/vendors');
  return response.data;
};

export const getAdminOrders = async () => {
  const response = await api.get('/admin/orders');
  return response.data;
};

export const getAdminCommissions = async () => {
  const response = await api.get('/admin/commissions');
  return response.data;
};

export const getSystemStatus = async () => {
  const response = await api.get('/admin/system/status');
  return response.data;
};
export const getAdminReport = async () => {
  const response = await api.get('/admin/reports/business');
  return response.data;
};