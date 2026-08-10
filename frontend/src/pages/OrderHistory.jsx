import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getOrderHistory } from "../services/orderService";

const STATUS_STYLES = {
  PENDING: "bg-amber-50 text-amber-700",
  CONFIRMED: "bg-emerald-50 text-emerald-700",
  PROCESSING: "bg-blue-50 text-blue-700",
  SHIPPED: "bg-indigo-50 text-indigo-700",
  DELIVERED: "bg-emerald-50 text-emerald-700",
  CANCELLED: "bg-rose-50 text-rose-700",
  RETURNED: "bg-rose-50 text-rose-700",
  REFUNDED: "bg-slate-100 text-slate-700",
};

function OrderHistory() {
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const data = await getOrderHistory();
        setOrders(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to load orders", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading your orders…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-4xl mx-auto">

        <h1 className="text-4xl font-serif font-bold text-slate-900 mb-8">
          Order History
        </h1>

        {orders.length === 0 ? (
          <div className="text-center mt-16 bg-white border border-stone-100 rounded-2xl py-16 px-6">
            <h2 className="text-2xl font-semibold text-slate-700">
              No orders yet
            </h2>
            <button
              onClick={() => navigate("/products")}
              className="mt-6 bg-emerald-700 hover:bg-emerald-800 text-white px-6 py-3 rounded-lg font-semibold transition"
            >
              Browse Products
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div
                key={order.id}
                onClick={() => navigate(`/orders/${order.id}`)}
                className="bg-white rounded-2xl border border-stone-100 shadow-sm p-5 cursor-pointer hover:shadow-md transition"
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold text-slate-900">Order #{order.id}</span>
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${STATUS_STYLES[order.status] || "bg-stone-100 text-stone-600"}`}>
                    {order.status}
                  </span>
                </div>
                <p className="text-slate-500 text-sm mb-1">
                  {new Date(order.createdAt).toLocaleString()}
                </p>
                <p className="text-slate-600 text-sm">
                  {order.items?.length || 0} item{order.items?.length !== 1 ? "s" : ""} · <span className="font-bold text-slate-900">₹{order.totalAmount}</span>
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default OrderHistory;