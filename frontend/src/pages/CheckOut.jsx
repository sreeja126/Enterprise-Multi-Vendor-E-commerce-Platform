import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCart } from "../services/cartService";
import { createRazorpayOrder, verifyPayment } from "../services/paymentService";
import { getAddresses, addAddress } from "../services/addressService";

const emptyForm = {
  fullName: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
};

function Checkout() {
  const navigate = useNavigate();

  const [cart, setCart] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [savingAddress, setSavingAddress] = useState(false);

  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    fetchCart();
    fetchAddresses();
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

  const fetchAddresses = async () => {
    try {
      const data = await getAddresses();
      setAddresses(data);

      const defaultAddr = data.find((a) => a.default) || data[0];
      if (defaultAddr) {
        setSelectedAddressId(defaultAddr.id);
      } else {
        setShowAddForm(true);
      }
    } catch (err) {
      console.error("Failed to load addresses", err);
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSaveAddress = async (e) => {
    e.preventDefault();
    setSavingAddress(true);
    try {
      await addAddress(form);
      const updated = await getAddresses();
      setAddresses(updated);
      const newest = updated[updated.length - 1];
      setSelectedAddressId(newest.id);
      setShowAddForm(false);
      setForm(emptyForm);
    } catch (err) {
      alert(err.response?.data || "Failed to save address.");
    } finally {
      setSavingAddress(false);
    }
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      alert("Please add or select a shipping address first.");
      return;
    }

    setPlacing(true);

    try {
      const razorpayOrder = await createRazorpayOrder();

      const options = {
        key: razorpayOrder.keyId,
        amount: razorpayOrder.amountInPaise,
        currency: razorpayOrder.currency,
        name: "ShopStack",
        description: "Order Payment",
        order_id: razorpayOrder.razorpayOrderId,

        handler: async (response) => {
          try {
            const order = await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              addressId: selectedAddressId,
            });

            navigate(`/orders/${order.id}`, { state: { justPlaced: true } });
          } catch (err) {
            alert(err.response?.data || "Payment verification failed. Please contact support if you were charged.");
          } finally {
            setPlacing(false);
          }
        },

        modal: {
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

        {/* Shipping Address */}
        <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-slate-900">Shipping Address</h2>
            {addresses.length > 0 && !showAddForm && (
              <button
                onClick={() => setShowAddForm(true)}
                className="text-emerald-700 hover:text-emerald-800 text-sm font-semibold"
              >
                + Add New
              </button>
            )}
          </div>

          {addresses.length > 0 && (
            <div className="space-y-3 mb-4">
              {addresses.map((addr) => (
                <label
                  key={addr.id}
                  className={`flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition ${
                    selectedAddressId === addr.id
                      ? "border-emerald-600 bg-emerald-50"
                      : "border-stone-200 hover:border-stone-300"
                  }`}
                >
                  <input
                    type="radio"
                    name="address"
                    checked={selectedAddressId === addr.id}
                    onChange={() => setSelectedAddressId(addr.id)}
                    className="mt-1"
                  />
                  <div className="text-sm">
                    <p className="font-semibold text-slate-900">
                      {addr.fullName}
                      {addr.default && (
                        <span className="ml-2 text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                          Default
                        </span>
                      )}
                    </p>
                    <p className="text-slate-600">
                      {addr.addressLine1}{addr.addressLine2 ? `, ${addr.addressLine2}` : ""}
                    </p>
                    <p className="text-slate-600">
                      {addr.city}, {addr.state} {addr.postalCode}, {addr.country}
                    </p>
                    <p className="text-slate-500">{addr.phone}</p>
                  </div>
                </label>
              ))}
            </div>
          )}

          {showAddForm && (
            <form onSubmit={handleSaveAddress} className="space-y-3 border-t border-stone-100 pt-4">
              <div className="grid grid-cols-2 gap-3">
                <input
                  name="fullName" placeholder="Full Name" required
                  value={form.fullName} onChange={handleFormChange}
                  className="border border-stone-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  name="phone" placeholder="Phone" required
                  value={form.phone} onChange={handleFormChange}
                  className="border border-stone-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
              <input
                name="addressLine1" placeholder="Address Line 1" required
                value={form.addressLine1} onChange={handleFormChange}
                className="w-full border border-stone-300 rounded-lg px-3 py-2 text-sm"
              />
              <input
                name="addressLine2" placeholder="Address Line 2 (optional)"
                value={form.addressLine2} onChange={handleFormChange}
                className="w-full border border-stone-300 rounded-lg px-3 py-2 text-sm"
              />
              <div className="grid grid-cols-3 gap-3">
                <input
                  name="city" placeholder="City" required
                  value={form.city} onChange={handleFormChange}
                  className="border border-stone-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  name="state" placeholder="State" required
                  value={form.state} onChange={handleFormChange}
                  className="border border-stone-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  name="postalCode" placeholder="Postal Code" required
                  value={form.postalCode} onChange={handleFormChange}
                  className="border border-stone-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
              <input
                name="country" placeholder="Country" required
                value={form.country} onChange={handleFormChange}
                className="w-full border border-stone-300 rounded-lg px-3 py-2 text-sm"
              />

              <div className="flex gap-3 pt-1">
                <button
                  type="submit"
                  disabled={savingAddress}
                  className="flex-1 bg-emerald-700 hover:bg-emerald-800 text-white py-2.5 rounded-lg text-sm font-semibold transition"
                >
                  {savingAddress ? "Saving…" : "Save Address"}
                </button>
                {addresses.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowAddForm(false)}
                    className="px-4 border border-stone-300 text-slate-600 rounded-lg text-sm font-medium hover:bg-stone-100 transition"
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>
          )}
        </div>

        {/* Order Summary */}
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
          disabled={placing || !selectedAddressId}
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