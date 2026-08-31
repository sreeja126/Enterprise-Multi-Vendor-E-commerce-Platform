import { useEffect, useState } from "react";
import { getVendorReturnRequests } from "../services/returnService";

const STATUS_STYLES = {
  REQUESTED: "bg-amber-50 text-amber-700 border-amber-200",
  APPROVED: "bg-blue-50 text-blue-700 border-blue-200",
  REJECTED: "bg-rose-50 text-rose-700 border-rose-200",
  QC_PENDING: "bg-violet-50 text-violet-700 border-violet-200",
  COMPLETED: "bg-emerald-50 text-emerald-700 border-emerald-200",
};

const STATUS_LABELS = {
  REQUESTED: "Awaiting Admin Review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  QC_PENDING: "At Warehouse — Awaiting QC",
  COMPLETED: "Completed",
};

const REFUND_STYLES = {
  PENDING: "bg-amber-50 text-amber-700",
  PROCESSED: "bg-emerald-50 text-emerald-700",
  FAILED: "bg-rose-50 text-rose-700",
};

function VendorReturns() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

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
          <p className="text-xs text-slate-400 mt-2 bg-stone-100 inline-block px-3 py-1.5 rounded-lg">
            📦 Returns are reviewed by admin and inspected at the warehouse — this view is read-only.
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
                        {STATUS_LABELS[req.status] || req.status}
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
                  <div className="text-xs text-slate-500 mb-2">
                    <span className="font-semibold">Admin's note:</span> {req.resolutionNote}
                  </div>
                )}

                {req.assignedWarehouseName && (
                  <div className="text-xs text-slate-500 mb-2">
                    <span className="font-semibold">Routed to warehouse:</span> {req.assignedWarehouseName}
                  </div>
                )}

                {req.qcResult && (
                  <div className="text-xs text-slate-500 mb-3">
                    <span className="font-semibold">QC result:</span>{" "}
                    <span className={req.qcResult === "ACCEPTED" ? "text-emerald-700 font-semibold" : "text-rose-700 font-semibold"}>
                      {req.qcResult === "ACCEPTED" ? "Accepted — restocked" : "Damaged — quarantined"}
                    </span>
                    {req.qcNote && <span> — {req.qcNote}</span>}
                  </div>
                )}

                {req.refund && (
                  <div className="flex items-center gap-2">
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
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default VendorReturns;