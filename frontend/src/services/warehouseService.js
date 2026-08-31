import api from './api';

// ---- Warehouse management ----
export const getWarehouses = async () => {
  const response = await api.get('/admin/warehouses');
  return response.data;
};

export const createWarehouse = async (payload) => {
  const response = await api.post('/admin/warehouses', payload);
  return response.data;
};

export const updateWarehouse = async (id, payload) => {
  const response = await api.put(`/admin/warehouses/${id}`, payload);
  return response.data;
};

export const deleteWarehouse = async (id) => {
  const response = await api.delete(`/admin/warehouses/${id}`);
  return response.data;
};

// ---- Stock receiving ----
export const getWarehouseStock = async (warehouseId) => {
  const response = await api.get(`/admin/warehouses/${warehouseId}/stock`);
  return response.data;
};

export const receiveStock = async (warehouseId, productId, quantity) => {
  const response = await api.post(`/admin/warehouses/${warehouseId}/stock`, { productId, quantity });
  return response.data;
};
// ---- Allocate an existing/older order ----
export const allocateExistingOrder = async (orderId) => {
  const response = await api.post(`/admin/warehouses/orders/${orderId}/allocate`);
  return response.data;
};
// ---- Fulfillment queues ----
// status: 'ALLOCATED' (to pick) | 'PICKED' (to pack) | 'PACKED' (to ship) | 'READY_FOR_SHIPMENT'
export const getWarehouseQueue = async (warehouseId, status) => {
  const response = await api.get(`/admin/warehouses/${warehouseId}/queue/${status}`);
  return response.data;
};

// ---- Pick / Pack / Ready actions ----
export const pickAllocation = async (allocationId) => {
  const response = await api.patch(`/admin/warehouses/allocations/${allocationId}/pick`);
  return response.data;
};

export const packAllocation = async (allocationId) => {
  const response = await api.patch(`/admin/warehouses/allocations/${allocationId}/pack`);
  return response.data;
};

export const markReadyForShipment = async (allocationId) => {
  const response = await api.patch(`/admin/warehouses/allocations/${allocationId}/ready`);
  return response.data;
};

// ---- Per-order fulfillment view + movement history ----
export const getAllocationsForOrder = async (orderId) => {
  const response = await api.get(`/admin/warehouses/orders/${orderId}/allocations`);
  return response.data;
};

export const getWarehouseMovements = async (warehouseId) => {
  const response = await api.get(`/admin/warehouses/${warehouseId}/movements`);
  return response.data;
};