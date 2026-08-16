import api from './api';

export const createRazorpayOrder = async () => {
  const response = await api.post('/payment/create-order');
  return response.data;
};

export const verifyPayment = async ({ razorpayOrderId, razorpayPaymentId, razorpaySignature, addressId }) => {
  const response = await api.post('/payment/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
    addressId,
  });
  return response.data;
};

// Cash on Delivery — creates a real order directly, no Razorpay involved.
export const placeCodOrder = async (addressId) => {
  const response = await api.post('/checkout/cod', { addressId });
  return response.data;
};

// ---- Buy Now: single-product express checkout, independent of the cart ----

export const createRazorpayOrderForProduct = async (productId, quantity = 1) => {
  const response = await api.post('/payment/create-order/buy-now', { productId, quantity });
  return response.data;
};

export const verifyBuyNowPayment = async ({
  razorpayOrderId, razorpayPaymentId, razorpaySignature, addressId, productId, quantity,
}) => {
  const response = await api.post('/payment/verify/buy-now', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
    addressId,
    productId,
    quantity,
  });
  return response.data;
};

export const placeCodBuyNowOrder = async (addressId, productId, quantity = 1) => {
  const response = await api.post('/checkout/cod/buy-now', { addressId, productId, quantity });
  return response.data;
};

const paymentService = {
  createRazorpayOrder,
  verifyPayment,
  placeCodOrder,
  createRazorpayOrderForProduct,
  verifyBuyNowPayment,
  placeCodBuyNowOrder,
};
export default paymentService;