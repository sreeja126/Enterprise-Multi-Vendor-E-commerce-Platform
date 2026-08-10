import api from './api';

export const createRazorpayOrder = async () => {
  const response = await api.post('/payment/create-order');
  return response.data;
};

export const verifyPayment = async ({ razorpayOrderId, razorpayPaymentId, razorpaySignature }) => {
  const response = await api.post('/payment/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
  });
  return response.data;
};

const paymentService = { createRazorpayOrder, verifyPayment };
export default paymentService;