import api from './api';

export const createRazorpayOrder = async () => {
  const response = await api.post('/payment/create-order');
  return response.data;
};

// Added addressId to the function signature and payload body
export const verifyPayment = async ({ razorpayOrderId, razorpayPaymentId, razorpaySignature, addressId }) => {
  const response = await api.post('/payment/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
    addressId, // <-- Include this
  });
  return response.data;
};

const paymentService = { createRazorpayOrder, verifyPayment };
export default paymentService;