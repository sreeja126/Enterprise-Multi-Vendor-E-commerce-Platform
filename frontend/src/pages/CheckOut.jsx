import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCart } from "../services/cartService";
import { createRazorpayOrder, verifyPayment } from "../services/paymentService";

function Checkout() {
  const navigate = useNavigate();

  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    fetchCart();
  }, []);

  const fetchCart = async () => {
    try {
      const data = await getCart();
      setCart(data);
    } catch (err) {
      console.error("Failed to load cart", err);
    } finally {
      setLoading(false);
    }
  };

  const handlePlaceOrder = async () => {
    setPlacing(true);

    try {
      // Step 1: ask our backend to create a Razorpay order for the
      // current cart total.
      const razorpayOrder = await createRazorpayOrder();

      // Step 2: open Razorpay's actual payment modal.
      const options = {
        key: razorpayOrder.keyId,
        amount: razorpayOrder.amountInPaise,
        currency: razorpayOrder.currency,
        name: "ShopStack",
        description: "Order Payment",
        order_id: razorpayOrder.razorpayOrderId,

        // Called by Razorpay only after the customer completes payment
        // successfully in the modal.
        handler: async (response) => {
          try {
            // Step 3: send Razorpay's response back to our backend so it
            // can verify the signature and only THEN create the real
            // order. We never create the order client-side or trust this
            // callback firing as proof of payment on its own.
            const order = await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });

            navigate(`/orders/${order.id}`, { state: { justPlaced: true } });
          } catch (err) {
            alert(err.response?.data || "Payment verification failed. Please contact support if you were charged.");
          } finally {
            setPlacing(false);
          }
        },

        modal: {
          // Customer closed the payment modal without paying.
          ondismiss: () => {
            setPlacing(false);
          },
        },

        theme: {
          color: "#0f172a",
        },
      };

      if (!window.Razorpay) {
        alert("Payment gateway failed to load. Please refresh and try again.");
        setPlacing(false);
        return;
      }

      const razorpayInstance = new window.Razorpay(options);
      razorpayInstance.open();

    } catch (err) {
      alert(err.response?.data || "Failed to start payment. Please review your cart.");
      setPlacing(false);
      fetchCart();
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading checkout…</p>
      </div>
    );
  }

  const items = cart?.items || [];

  if (items.length === 0) {
    return (
      <div className="min-h-screen bg-stone-50 flex flex-col items-center justify-center gap-4">
        <h2 className="text-xl font-medium text-slate-600">Your cart is empty.</h2>
        <button
          onClick={() => navigate("/products")}
          className="bg-emerald-700 hover:bg-emerald-800 text-white px-6 py-3 rounded-lg font-semibold transition"
        >
          Browse Products
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-3xl mx-auto">

        <h1 className="text-4xl font-serif font-bold text-slate-900 mb-8">
          Checkout
        </h1>

        <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-6 mb-6">
          <h2 className="text-lg font-bold text-slate-900 mb-4">Order Summary</h2>

          <div className="space-y-3">
            {items.map((item) => (
              <div key={item.id} className="flex justify-between text-sm">
                <span className="text-slate-600">
                  {item.productName} <span className="text-slate-400">× {item.quantity}</span>
                </span>
                <span className="font-medium text-slate-900">₹{item.lineTotal}</span>
              </div>
            ))}
          </div>

          <div className="flex justify-between text-xl font-bold text-slate-900 border-t border-stone-100 pt-4 mt-4">
            <span>Total</span>
            <span>₹{cart.totalAmount}</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-6 mb-6">
          <h2 className="text-lg font-bold text-slate-900 mb-2">Payment</h2>
          <p className="text-slate-500 text-sm">
            You'll be asked to complete payment via Razorpay in a secure popup.
            This is running in test mode — no real money is charged.
          </p>
        </div>

        <button
          onClick={handlePlaceOrder}
          disabled={placing}
          className="w-full bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white py-4 rounded-lg font-semibold text-lg transition"
        >
          {placing ? "Processing…" : `Pay ₹${cart.totalAmount} with Razorpay`}
        </button>

        <button
          onClick={() => navigate("/cart")}
          className="w-full mt-3 border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
        >
          Back to Cart
        </button>

      </div>
    </div>
  );
}

export default Checkout;