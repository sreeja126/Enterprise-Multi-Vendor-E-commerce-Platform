import { useEffect, useState } from "react";
import { getVendorOrderItems, updateOrderItemStatus } from "../services/OrderService";

const STATUS_STYLES = {
  PENDING: "bg-stone-100 text-stone-600",
  CONFIRMED: "bg-blue-50 text-blue-700",
  PROCESSING: "bg-amber-50 text-amber-700",
  SHIPPED: "bg-indigo-50 text-indigo-700",
  DELIVERED: "bg-emerald-50 text-emerald-700",
  CANCELLED: "bg-rose-50 text-rose-700",
  RETURNED: "bg-rose-50 text-rose-700",
  REFUNDED: "bg-slate-100 text-slate-700",
};

// Only the forward moves a vendor can make from each status. CANCELLED is
// always offered separately (as long as the item isn't already terminal).
const NEXT_STATUS = {
  CONFIRMED: "PROCESSING",
  PROCESSING: "SHIPPED",
  SHIPPED: "DELIVERED",
};

const TERMINAL = new Set(["DELIVERED", "CANCELLED", "RETURNED", "REFUNDED"]);

function VendorOrders() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    fetchItems();
  }, []);

  const fetchItems = async () => {
    try {
      const data = await getVendorOrderItems();
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to load orders", err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (itemId, newStatus) => {
    setBusyId(itemId);
    try {
      const updated = await updateOrderItemStatus(itemId, newStatus);
      setItems((prev) => prev.map((i) => (i.id === itemId ? updated : i)));
    } catch (err) {
      alert(err.response?.data || "Failed to update status.");
    } finally {
      setBusyId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading orders…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-5xl mx-auto">

        <div className="mb-8">
          <h1 className="text-4xl font-serif font-bold text-slate-900">
            Orders
          </h1>
          <p className="text-slate-500 mt-1">
            {items.length} item{items.length !== 1 ? "s" : ""} sold, across all your orders
          </p>
        </div>

        {items.length === 0 ? (
          <div className="text-center mt-16 bg-white border border-stone-100 rounded-2xl py-16 px-6">
            <h2 className="text-2xl font-semibold text-slate-700">
              No orders yet
            </h2>
            <p className="text-slate-500 mt-2">
              Orders containing your products will show up here.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {items.map((item) => {
              const nextStatus = NEXT_STATUS[item.status];
              const isTerminal = TERMINAL.has(item.status);

              return (
                <div
                  key={item.id}
                  className="bg-white rounded-2xl border border-stone-100 shadow-sm p-5"
                >
                  <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">

                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-xs text-slate-400">Order #{item.orderId}</span>
                        <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${STATUS_STYLES[item.status] || "bg-stone-100 text-stone-600"}`}>
                          {item.status}
                        </span>
                      </div>
                      <p className="font-semibold text-slate-900">{item.productName}</p>
                      <p className="text-sm text-slate-500">
                        ₹{item.priceAtPurchase} × {item.quantity} = <span className="font-medium text-slate-700">₹{item.lineTotal}</span>
                      </p>
                      <p className="text-xs text-slate-400 mt-1">
                        {new Date(item.orderCreatedAt).toLocaleString()}
                      </p>
                    </div>

                    <div className="text-sm text-slate-600 md:text-right">
                      <p className="font-medium text-slate-900">{item.shippingFullName}</p>
                      <p>
                        {item.shippingAddressLine1}
                        {item.shippingAddressLine2 ? `, ${item.shippingAddressLine2}` : ""}
                      </p>
                      <p>
                        {item.shippingCity}, {item.shippingState} {item.shippingPostalCode}
                      </p>
                      <p>{item.shippingPhone}</p>
                    </div>

                  </div>

                  {!isTerminal && (
                    <div className="flex gap-2 mt-4 pt-4 border-t border-stone-100">
                      {nextStatus && (
                        <button
                          onClick={() => handleUpdateStatus(item.id, nextStatus)}
                          disabled={busyId === item.id}
                          className="px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-700 hover:bg-emerald-800 text-white transition disabled:opacity-60"
                        >
                          {busyId === item.id ? "Updating…" : `Mark as ${nextStatus}`}
                        </button>
                      )}
                      <button
                        onClick={() => {
                          if (window.confirm("Cancel this item? This can't be undone.")) {
                            handleUpdateStatus(item.id, "CANCELLED");
                          }
                        }}
                        disabled={busyId === item.id}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-rose-200 text-rose-600 hover:bg-rose-50 transition disabled:opacity-60"
                      >
                        Cancel
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default VendorOrders;