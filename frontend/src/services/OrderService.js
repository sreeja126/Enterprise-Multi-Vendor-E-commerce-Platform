import api from './api';

// Converts the current cart into a real order (validates stock, snapshots
// prices, reduces stock, clears the cart — all server-side, atomically).
export const checkout = async () => {
  const response = await api.post('/checkout');
  return response.data;
};

export const getOrderHistory = async () => {
  const response = await api.get('/orders');
  return response.data;
};

export const getOrderById = async (id) => {
  const response = await api.get(`/orders/${id}`);
  return response.data;
};

const orderService = {
  checkout,
  getOrderHistory,
  getOrderById,
};

export default orderService;