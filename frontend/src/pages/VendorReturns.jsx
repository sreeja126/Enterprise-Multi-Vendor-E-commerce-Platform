import { useEffect, useState } from "react";
import { getVendorReturnRequests, approveReturn, rejectReturn } from "../services/returnService";

const STATUS_STYLES = {
  REQUESTED: "bg-amber-50 text-amber-700 border-amber-200",
  APPROVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
  REJECTED: "bg-rose-50 text-rose-700 border-rose-200",
};

const REFUND_STYLES = {
  PENDING: "bg-amber-50 text-amber-700",
  PROCESSED: "bg-emerald-50 text-emerald-700",
  FAILED: "bg-rose-50 text-rose-700",
};

function VendorReturns() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const data = await getVendorReturnRequests();
      setRequests(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to load return requests", err);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    if (!window.confirm("Approve this return? This will restore stock and process a refund.")) return;

    setBusyId(id);
    try {
      const updated = await approveReturn(id);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      alert(err.response?.data || "Failed to approve return.");
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (id) => {
    const note = window.prompt("Reason for rejecting this return (optional):") || "";

    setBusyId(id);
    try {
      const updated = await rejectReturn(id, note);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      alert(err.response?.data || "Failed to reject return.");
    } finally {
      setBusyId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading return requests…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-4xl mx-auto">

        <div className="mb-8">
          <h1 className="text-4xl font-serif font-bold text-slate-900">
            Return Requests
          </h1>
          <p className="text-slate-500 mt-1">
            {requests.length} request{requests.length !== 1 ? "s" : ""} across your products
          </p>
        </div>

        {requests.length === 0 ? (
          <div className="text-center mt-16 bg-white border border-stone-100 rounded-2xl py-16 px-6">
            <h2 className="text-2xl font-semibold text-slate-700">
              No return requests yet
            </h2>
          </div>
        ) : (
          <div className="space-y-4">
            {requests.map((req) => (
              <div
                key={req.id}
                className="bg-white rounded-2xl border border-stone-100 shadow-sm p-5"
              >
                <div className="flex items-start justify-between gap-4 mb-3">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs text-slate-400">Order #{req.orderId}</span>
                      <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${STATUS_STYLES[req.status] || "bg-stone-100 text-stone-600 border-stone-200"}`}>
                        {req.status}
                      </span>
                    </div>
                    <p className="font-semibold text-slate-900">{req.productName}</p>
                    <p className="text-sm text-slate-500">₹{req.lineTotal}</p>
                    <p className="text-xs text-slate-400 mt-1">
                      Requested {new Date(req.requestedAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="bg-stone-50 rounded-xl p-3 text-sm text-slate-700 mb-3">
                  <span className="font-semibold text-slate-500 text-xs uppercase tracking-wide block mb-1">
                    Customer's reason
                  </span>
                  {req.reason}
                </div>

                {req.resolutionNote && (
                  <div className="text-xs text-slate-500 mb-3">
                    <span className="font-semibold">Your note:</span> {req.resolutionNote}
                  </div>
                )}

                {req.refund && (
                  <div className="flex items-center gap-2 mb-3">
                    <span className={`text-xs font-semibold px-2 py-1 rounded-full ${REFUND_STYLES[req.refund.status] || "bg-stone-100 text-stone-600"}`}>
                      Refund: {req.refund.status}
                    </span>
                    {req.refund.gatewayRefundId && (
                      <span className="text-xs text-slate-400 font-mono">
                        {req.refund.gatewayRefundId}
                      </span>
                    )}
                    {req.refund.failureReason && (
                      <span className="text-xs text-rose-500">{req.refund.failureReason}</span>
                    )}
                  </div>
                )}

                {req.status === "REQUESTED" && (
                  <div className="flex gap-2 pt-2 border-t border-stone-100">
                    <button
                      onClick={() => handleApprove(req.id)}
                      disabled={busyId === req.id}
                      className="px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-700 hover:bg-emerald-800 text-white transition disabled:opacity-60"
                    >
                      {busyId === req.id ? "Processing…" : "Approve & Refund"}
                    </button>
                    <button
                      onClick={() => handleReject(req.id)}
                      disabled={busyId === req.id}
                      className="px-4 py-2 rounded-lg text-sm font-medium border border-rose-200 text-rose-600 hover:bg-rose-50 transition disabled:opacity-60"
                    >
                      Reject
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default VendorReturns;