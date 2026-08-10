import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getOrderById } from "../services/orderService";

function OrderDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const justPlaced = location.state?.justPlaced;

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const data = await getOrderById(id);
        setOrder(data);
      } catch (err) {
        console.error("Failed to load order", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [id]);

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading order…</p>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-stone-50 flex flex-col items-center justify-center gap-4">
        <h2 className="text-xl font-medium text-rose-600">Order not found.</h2>
        <button
          onClick={() => navigate("/orders")}
          className="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm hover:bg-slate-900 transition"
        >
          Back to Orders
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-3xl mx-auto">

        {justPlaced && (
          <div className="bg-emerald-50 border border-emerald-100 text-emerald-700 rounded-xl px-5 py-4 mb-6 text-sm font-medium">
            🎉 Order placed successfully!
          </div>
        )}

        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl font-serif font-bold text-slate-900">
            Order #{order.id}
          </h1>
          <span className="text-sm font-semibold px-3 py-1.5 rounded-full bg-emerald-50 text-emerald-700">
            {order.status}
          </span>
        </div>

        <p className="text-slate-500 mb-6">
          Placed on {new Date(order.createdAt).toLocaleString()}
        </p>

        <div className="bg-white rounded-2xl border border-stone-100 shadow-sm overflow-hidden mb-6">
          {order.items.map((item, idx) => (
            <div
              key={idx}
              className="flex justify-between items-center px-6 py-4 border-b border-stone-100 last:border-0"
            >
              <div>
                <p className="font-medium text-slate-900">{item.productName}</p>
                <p className="text-sm text-slate-500">
                  ₹{item.priceAtPurchase} × {item.quantity}
                </p>
              </div>
              <p className="font-bold text-slate-900">₹{item.lineTotal}</p>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-6 flex justify-between items-center mb-6">
          <span className="text-lg font-bold text-slate-900">Total Paid</span>
          <span className="text-2xl font-bold text-amber-600">₹{order.totalAmount}</span>
        </div>

        {order.payment && (
          <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-6 mb-6">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Payment Receipt</h2>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">Status</span>
                <span className={`font-semibold ${order.payment.status === "SUCCESS" ? "text-emerald-600" : "text-rose-600"}`}>
                  {order.payment.status}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Method</span>
                <span className="text-slate-900">{order.payment.method}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Transaction ID</span>
                <span className="text-slate-900 font-mono text-xs">{order.payment.transactionId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Paid At</span>
                <span className="text-slate-900">{new Date(order.payment.paidAt).toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}

        <button
          onClick={() => navigate("/orders")}
          className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
        >
          Back to Order History
        </button>

      </div>
    </div>
  );
}

export default OrderDetails;