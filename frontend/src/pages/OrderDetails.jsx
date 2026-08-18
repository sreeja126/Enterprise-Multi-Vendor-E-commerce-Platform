import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getOrderById, cancelOrderItem, cancelOrder } from "../services/OrderService";
import { requestReturn, getMyReturnRequests } from "../services/returnService";

const STATUS_STYLES = {
  PENDING: "bg-amber-50 text-amber-700 border-amber-200",
  CONFIRMED: "bg-blue-50 text-blue-700 border-blue-200",
  PROCESSING: "bg-indigo-50 text-indigo-700 border-indigo-200",
  SHIPPED: "bg-purple-50 text-purple-700 border-purple-200",
  DELIVERED: "bg-emerald-50 text-emerald-700 border-emerald-200",
  CANCELLED: "bg-rose-50 text-rose-700 border-rose-200",
  RETURNED: "bg-orange-50 text-orange-700 border-orange-200",
  REFUNDED: "bg-slate-100 text-slate-700 border-slate-200",
};

const RETURN_STATUS_STYLES = {
  REQUESTED: "bg-amber-50 text-amber-700 border-amber-200",
  APPROVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
  REJECTED: "bg-rose-50 text-rose-700 border-rose-200",
};

const REFUND_STATUS_STYLES = {
  PENDING: "bg-amber-50 text-amber-700",
  PROCESSED: "bg-emerald-50 text-emerald-700",
  FAILED: "bg-rose-50 text-rose-700",
};

const ORDER_STEPS = ["CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED"];

// A customer can only cancel while an item hasn't started being fulfilled
// yet — matches the backend's own rule in OrderService.cancelOrderItem.
const CANCELLABLE_STATUSES = new Set(["PENDING", "CONFIRMED"]);

function OrderDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const justPlaced = location.state?.justPlaced;

  const [order, setOrder] = useState(null);
  // Maps orderItemId -> that item's return request (if any exists at all),
  // so the UI can show its real status instead of just re-showing the
  // "Request Return" button as if nothing had happened.
  const [returnsByItemId, setReturnsByItemId] = useState({});
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null); // item id, or "order" for whole-order cancel

  useEffect(() => {
    fetchOrder();
    fetchReturns();
  }, [id]);

  const fetchOrder = async () => {
    try {
      const data = await getOrderById(id);
      setOrder(data);
    } catch (err) {
      console.error("Failed to fetch order:", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchReturns = async () => {
    try {
      const allReturns = await getMyReturnRequests();
      const map = {};
      (allReturns || []).forEach((r) => {
        if (String(r.orderId) === String(id)) {
          map[r.orderItemId] = r;
        }
      });
      setReturnsByItemId(map);
    } catch (err) {
      console.error("Failed to fetch return requests:", err);
    }
  };

  const handleCancelItem = async (itemId) => {
    if (!window.confirm("Cancel this item? This can't be undone.")) return;

    setCancellingId(itemId);
    try {
      const updated = await cancelOrderItem(itemId);
      setOrder(updated);
    } catch (err) {
      alert(err.response?.data || "Failed to cancel this item.");
    } finally {
      setCancellingId(null);
    }
  };

  const handleRequestReturn = async (itemId) => {
    const reason = window.prompt("Why are you returning this item?");
    if (reason === null) return; // cancelled the prompt
    if (!reason.trim()) {
      alert("Please provide a reason for the return.");
      return;
    }

    setCancellingId(itemId);
    try {
      await requestReturn(itemId, reason.trim());
      await fetchOrder();
      await fetchReturns();
      alert("Return request submitted. The seller will review it shortly.");
    } catch (err) {
      alert(err.response?.data || "Failed to submit return request.");
    } finally {
      setCancellingId(null);
    }
  };


  const handleCancelWholeOrder = async () => {
    if (!window.confirm("Cancel this entire order? This can't be undone.")) return;

    setCancellingId("order");
    try {
      const updated = await cancelOrder(order.id);
      setOrder(updated);
    } catch (err) {
      alert(err.response?.data || "Failed to cancel this order.");
    } finally {
      setCancellingId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="w-8 h-8 border-3 border-emerald-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-stone-50 flex flex-col items-center justify-center p-4">
        <div className="bg-white border border-stone-200 rounded-2xl p-8 max-w-md w-full text-center shadow-xs">
          <p className="text-slate-600 text-sm font-semibold mb-4">Order not found.</p>
          <button
            onClick={() => navigate("/orders")}
            className="w-full py-2.5 bg-slate-900 text-white rounded-xl text-sm font-semibold hover:bg-slate-800 transition cursor-pointer"
          >
            Back to Orders
          </button>
        </div>
      </div>
    );
  }

  const isCOD =
    order.paymentMethod === "COD" ||
    order.paymentMethod === "cod" ||
    order.payment?.method === "COD";

  const currentStepIndex = ORDER_STEPS.indexOf(order.status);
  const isCancelled = order.status === "CANCELLED" || order.status === "RETURNED";

  const anyItemCancellable = (order.items || []).some((item) =>
    CANCELLABLE_STATUSES.has(item.status)
  );

  return (
    <div className="min-h-screen bg-stone-50/60 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">

        {/* Top Bar */}
        <div className="mb-6 flex items-center justify-between">
          <button
            onClick={() => navigate("/orders")}
            className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-slate-900 transition cursor-pointer"
          >
            ← Back to Orders
          </button>

          {anyItemCancellable && (
            <button
              onClick={handleCancelWholeOrder}
              disabled={cancellingId === "order"}
              className="text-xs font-semibold text-rose-600 hover:text-rose-700 border border-rose-200 hover:bg-rose-50 px-3.5 py-2 rounded-xl transition cursor-pointer disabled:opacity-50"
            >
              {cancellingId === "order" ? "Cancelling…" : "Cancel Order"}
            </button>
          )}
        </div>

        {/* Confirmation banner when coming directly from checkout */}
        {justPlaced && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl p-4 mb-6 flex items-center gap-3 shadow-xs">
            <div className="h-8 w-8 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold shrink-0">
              ✓
            </div>
            <div>
              <h4 className="font-semibold text-sm">Order Confirmed!</h4>
              <p className="text-xs text-emerald-700">
                Your order has been placed. {isCOD ? "Please keep cash ready at delivery." : "Payment successful."}
              </p>
            </div>
          </div>
        )}

        {/* Order Header */}
        <div className="bg-white border border-stone-200/80 rounded-2xl p-6 sm:p-8 shadow-xs mb-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-stone-100">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-serif font-bold text-slate-900">
                  Order #{order.customerOrderNumber || order.id}
                </h1>
                <span
                  className={`text-xs font-bold px-3 py-1 rounded-full border ${
                    STATUS_STYLES[order.status] || "bg-stone-100 text-stone-700"
                  }`}
                >
                  {order.status}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Placed on {new Date(order.createdAt).toLocaleDateString("en-IN", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </p>
            </div>

            <button
              onClick={() => window.print()}
              className="px-3.5 py-2 border border-stone-200 rounded-xl text-xs font-semibold text-slate-700 hover:bg-stone-50 transition cursor-pointer"
            >
              Print Receipt
            </button>
          </div>

          {/* Progress Tracker */}
          {!isCancelled && currentStepIndex !== -1 && (
            <div className="pt-6">
              <div className="grid grid-cols-4 gap-2">
                {ORDER_STEPS.map((step, idx) => {
                  const isCompleted = idx <= currentStepIndex;
                  return (
                    <div key={step} className="flex flex-col items-center text-center">
                      <div
                        className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs ${
                          isCompleted
                            ? "bg-slate-900 text-white"
                            : "bg-stone-100 text-stone-400 border border-stone-200"
                        }`}
                      >
                        {isCompleted ? "✓" : idx + 1}
                      </div>
                      <span
                        className={`text-[10px] sm:text-[11px] font-semibold mt-2 uppercase tracking-wider ${
                          isCompleted ? "text-slate-900" : "text-stone-400"
                        }`}
                      >
                        {step}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* 2-Column Content */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

          {/* Left Column: Items & Payment Details */}
          <div className="lg:col-span-7 space-y-6">

            {/* Items */}
            <div className="bg-white rounded-2xl border border-stone-200/80 shadow-xs overflow-hidden">
              <div className="p-5 border-b border-stone-100 bg-stone-50/50">
                <h2 className="text-base font-bold text-slate-900">
                  Ordered Items ({order.items?.length || 0})
                </h2>
              </div>
              <div className="divide-y divide-stone-100">
                {order.items?.map((item, idx) => {
                  const itemCancellable = CANCELLABLE_STATUSES.has(item.status);
                  const existingReturn = returnsByItemId[item.id];

                  return (
                    <div key={item.id || idx} className="p-5">
                      <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-stone-100 border rounded-lg flex items-center justify-center text-stone-400 font-bold shrink-0">
                            📦
                          </div>
                          <div>
                            <p className="font-semibold text-slate-900 text-sm">{item.productName}</p>
                            <p className="text-xs text-slate-500">₹{item.priceAtPurchase} × {item.quantity}</p>
                            {item.status && (
                              <span
                                className={`inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                                  STATUS_STYLES[item.status] || "bg-stone-100 text-stone-600 border-stone-200"
                                }`}
                              >
                                {item.status}
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="flex flex-col items-end gap-2">
                          <p className="font-bold text-slate-900 text-sm">₹{item.lineTotal}</p>
                          {itemCancellable && (
                            <button
                              onClick={() => handleCancelItem(item.id)}
                              disabled={cancellingId === item.id}
                              className="text-[11px] font-semibold text-rose-600 hover:text-rose-700 transition cursor-pointer disabled:opacity-50"
                            >
                              {cancellingId === item.id ? "Cancelling…" : "Cancel"}
                            </button>
                          )}
                          {item.status === "DELIVERED" && !existingReturn && (
                            <button
                              onClick={() => handleRequestReturn(item.id)}
                              disabled={cancellingId === item.id}
                              className="text-[11px] font-semibold text-slate-600 hover:text-slate-900 border border-stone-200 hover:bg-stone-50 px-2.5 py-1 rounded-lg transition cursor-pointer disabled:opacity-50"
                            >
                              {cancellingId === item.id ? "Submitting…" : "Request Return"}
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Refund from a straight cancellation (no return
                          request involved) — happens when a Razorpay-paid
                          item is cancelled before delivery. */}
                      {item.status === "CANCELLED" && item.refund && (
                        <div className="mt-3 ml-13 bg-stone-50 border border-stone-100 rounded-xl p-3 text-xs">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-slate-500 uppercase tracking-wide text-[10px]">
                              Refund
                            </span>
                            <span
                              className={`font-semibold px-2 py-0.5 rounded-full ${
                                REFUND_STATUS_STYLES[item.refund.status] || "bg-stone-100 text-slate-600"
                              }`}
                            >
                              ₹{item.refund.amount} — {item.refund.status}
                            </span>
                            {item.refund.gatewayRefundId && (
                              <span className="text-slate-400 font-mono text-[10px]">
                                {item.refund.gatewayRefundId}
                              </span>
                            )}
                          </div>
                          {item.refund.failureReason && (
                            <p className="text-rose-500 mt-1">{item.refund.failureReason}</p>
                          )}
                        </div>
                      )}

                      {/* Return/refund status — this is the piece that was
                          missing entirely: once a return is requested, this
                          shows its real status instead of the page looking
                          unchanged. */}
                      {existingReturn && (
                        <div className="mt-3 ml-13 bg-stone-50 border border-stone-100 rounded-xl p-3 text-xs">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="font-semibold text-slate-500 uppercase tracking-wide text-[10px]">
                              Return
                            </span>
                            <span
                              className={`font-semibold px-2 py-0.5 rounded-full border ${
                                RETURN_STATUS_STYLES[existingReturn.status] || "bg-stone-100 text-stone-600 border-stone-200"
                              }`}
                            >
                              {existingReturn.status}
                            </span>
                          </div>
                          <p className="text-slate-600">
                            <span className="font-medium">Your reason:</span> {existingReturn.reason}
                          </p>
                          {existingReturn.resolutionNote && (
                            <p className="text-slate-600 mt-1">
                              <span className="font-medium">Seller's note:</span> {existingReturn.resolutionNote}
                            </p>
                          )}

                          {existingReturn.refund && (
                            <div className="flex items-center gap-2 mt-2 pt-2 border-t border-stone-200">
                              <span className="font-semibold text-slate-500 uppercase tracking-wide text-[10px]">
                                Refund
                              </span>
                              <span
                                className={`font-semibold px-2 py-0.5 rounded-full ${
                                  REFUND_STATUS_STYLES[existingReturn.refund.status] || "bg-stone-100 text-stone-600"
                                }`}
                              >
                                ₹{existingReturn.refund.amount} — {existingReturn.refund.status}
                              </span>
                              {existingReturn.refund.gatewayRefundId && (
                                <span className="text-slate-400 font-mono text-[10px]">
                                  {existingReturn.refund.gatewayRefundId}
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Payment Details */}
            <div className="bg-white rounded-2xl border border-stone-200/80 p-6 shadow-xs">
              <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span>💳</span> Payment Information
              </h2>

              <div className="space-y-2.5 text-xs sm:text-sm">
                <div className="flex justify-between py-1 border-b border-stone-100">
                  <span className="text-slate-500">Payment Method</span>
                  <span className="font-semibold text-slate-900 uppercase">
                    {isCOD ? "Cash on Delivery (COD)" : order.payment?.method || "Online"}
                  </span>
                </div>

                <div className="flex justify-between py-1 border-b border-stone-100">
                  <span className="text-slate-500">Payment Status</span>
                  <span
                    className={`font-semibold px-2 py-0.5 rounded text-xs ${
                      isCOD
                        ? "bg-amber-100 text-amber-800"
                        : "bg-emerald-100 text-emerald-800"
                    }`}
                  >
                    {isCOD ? "Pending (Pay on Delivery)" : order.payment?.status || "Paid Online"}
                  </span>
                </div>

                {!isCOD && order.payment?.transactionId && (
                  <div className="flex justify-between py-1 border-b border-stone-100">
                    <span className="text-slate-500">Transaction ID</span>
                    <span className="font-mono text-slate-800 bg-stone-100 px-2 py-0.5 rounded text-xs">
                      {order.payment.transactionId}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right Column: Address & Total Summary */}
          <div className="lg:col-span-5 space-y-6">

            {/* Address */}
            <div className="bg-white rounded-2xl border border-stone-200/80 p-6 shadow-xs">
              <h2 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
                <span>📍</span> Shipping Address
              </h2>
              <div className="text-xs sm:text-sm text-slate-600 space-y-1">
                <p className="font-bold text-slate-900">
                  {order.shippingFullName || order.address?.fullName}
                </p>
                <p>{order.shippingAddressLine1 || order.address?.addressLine1}</p>
                {(order.shippingAddressLine2 || order.address?.addressLine2) && (
                  <p>{order.shippingAddressLine2 || order.address?.addressLine2}</p>
                )}
                <p>
                  {order.shippingCity || order.address?.city},{" "}
                  {order.shippingState || order.address?.state}{" "}
                  {order.shippingPostalCode || order.address?.postalCode}
                </p>
                <p className="pt-2 font-semibold text-slate-500">
                  📞 {order.shippingPhone || order.address?.phone}
                </p>
              </div>
            </div>

            {/* Total Summary */}
            <div className="bg-white border border-stone-200/80 rounded-2xl p-6 shadow-xs space-y-4">
              <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Summary
              </h2>

              <div className="space-y-2 text-xs sm:text-sm text-slate-600 border-b border-stone-100 pb-3">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span className="font-semibold text-slate-900">₹{order.totalAmount}</span>
                </div>
                <div className="flex justify-between">
                  <span>Shipping</span>
                  <span className="text-emerald-700 font-semibold uppercase text-xs">Free</span>
                </div>
              </div>

              <div className="flex justify-between items-baseline pt-1">
                <span className="text-base font-bold text-slate-900">
                  {isCOD ? "Total Payable (COD)" : "Total Paid"}
                </span>
                <span className="text-2xl font-extrabold text-slate-900">
                  ₹{order.totalAmount}
                </span>
              </div>
            </div>

          </div>

        </div>

      </div>
    </div>
  );
}

export default OrderDetails;