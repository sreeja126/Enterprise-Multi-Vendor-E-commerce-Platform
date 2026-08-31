import React, { useEffect, useMemo, useState } from 'react';
import { getAdminOrders, markOrderItemDelivered } from '../../services/adminService';
import { getAllocationsForOrder } from '../../services/warehouseService';

const AdminOrders = () => {
  const [orders, setOrders] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  // Fulfillment (warehouse pick/pack/ship) data, keyed by order id.
  // Fetched in bulk for confirmed orders after the order list loads, and
  // lazily on-demand if a row is expanded before the bulk fetch reaches it.
  const [allocationsByOrder, setAllocationsByOrder] = useState({});
  const [fulfillmentLoadingIds, setFulfillmentLoadingIds] = useState(new Set());
  const [expandedOrderId, setExpandedOrderId] = useState(null);
  const [deliveringItemId, setDeliveringItemId] = useState(null);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const response = await getAdminOrders();
      const list = Array.isArray(response) ? response : [];
      setOrders(list);
      loadFulfillmentSummaries(list);
    } catch (err) {
      console.error('Error fetching admin orders:', err);
    } finally {
      setLoading(false);
    }
  };

  const FULFILLMENT_EXCLUDED_STATUSES = ['PENDING', 'CANCELLED'];

  const hasFulfillmentTrail = (order) =>
    !FULFILLMENT_EXCLUDED_STATUSES.includes(
      String(getStatus(order)).toUpperCase()
    );

  const loadFulfillmentSummaries = async (orderList) => {
    const confirmedOrders = orderList.filter(hasFulfillmentTrail);

    const results = await Promise.allSettled(
      confirmedOrders.map((o) => getAllocationsForOrder(o.id))
    );

    setAllocationsByOrder((prev) => {
      const next = { ...prev };

      confirmedOrders.forEach((o, idx) => {
        const result = results[idx];

        next[o.id] =
          result.status === 'fulfilled' && Array.isArray(result.value)
            ? result.value
            : [];
      });

      return next;
    });
  };

  const loadAllocationsForOrder = async (orderId) => {
    setFulfillmentLoadingIds((prev) => new Set(prev).add(orderId));

    try {
      const data = await getAllocationsForOrder(orderId);

      setAllocationsByOrder((prev) => ({
        ...prev,
        [orderId]: Array.isArray(data) ? data : []
      }));
    } catch (err) {
      console.error(
        `Failed to load fulfillment for order ${orderId}:`,
        err
      );

      setAllocationsByOrder((prev) => ({
        ...prev,
        [orderId]: []
      }));
    } finally {
      setFulfillmentLoadingIds((prev) => {
        const next = new Set(prev);
        next.delete(orderId);
        return next;
      });
    }
  };

  const toggleExpandOrder = (orderId) => {
    const nowExpanding = expandedOrderId !== orderId;

    setExpandedOrderId(nowExpanding ? orderId : null);

    if (nowExpanding && !allocationsByOrder[orderId]) {
      loadAllocationsForOrder(orderId);
    }
  };

  const handleMarkDelivered = async (orderId, orderItemId) => {
    setDeliveringItemId(orderItemId);

    try {
      await markOrderItemDelivered(orderItemId);
      await loadAllocationsForOrder(orderId);
      await loadOrders();
    } catch (err) {
      alert(
        err.response?.data ||
        'Failed to mark this item as delivered.'
      );
    } finally {
      setDeliveringItemId(null);
    }
  };

  // Where an order sits, overall, in the pick -> pack -> ship pipeline.
  // Driven by the LEAST-progressed active allocation, since that's the
  // item holding the whole order back from shipping.
  const STAGE_ORDER = [
    'ALLOCATED',
    'PICKED',
    'PACKED',
    'READY_FOR_SHIPMENT',
    'DELIVERED'
  ];

  const STAGE_LABELS = {
    ALLOCATED: 'To Pick',
    PICKED: 'To Pack',
    PACKED: 'To Ship',
    READY_FOR_SHIPMENT: 'Ready to Ship',
    DELIVERED: 'Delivered'
  };

  const STAGE_STYLES = {
    ALLOCATED: 'bg-amber-50 text-amber-700 border-amber-200',
    PICKED: 'bg-blue-50 text-blue-700 border-blue-200',
    PACKED: 'bg-violet-50 text-violet-700 border-violet-200',
    READY_FOR_SHIPMENT:
      'bg-emerald-50 text-emerald-700 border-emerald-200',
    DELIVERED:
      'bg-emerald-50 text-emerald-700 border-emerald-200'
  };

  const getFulfillmentSummary = (orderId) => {
    const allocations = allocationsByOrder[orderId];

    if (allocations === undefined) {
      return {
        loading: fulfillmentLoadingIds.has(orderId),
        label: null
      };
    }

    const active = allocations.filter(
      (a) => a.status !== 'CANCELLED'
    );

    if (active.length === 0) {
      return allocations.length > 0
        ? {
            label: 'Cancelled',
            style: 'bg-rose-50 text-rose-700 border-rose-200',
            total: allocations.length
          }
        : {
            label: 'Awaiting Allocation',
            style: 'bg-stone-100 text-slate-600 border-stone-200',
            total: 0
          };
    }

    // Final state — all active items are delivered.
    if (active.every((a) => a.status === 'DELIVERED')) {
      return {
        label: 'Delivered',
        style: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        total: active.length
      };
    }

    const stageIndexes = active
      .map((a) => STAGE_ORDER.indexOf(a.status))
      .filter((index) => index >= 0);

    const minStageIndex =
      stageIndexes.length > 0
        ? Math.min(...stageIndexes)
        : 0;

    const stage =
      STAGE_ORDER[minStageIndex] || 'ALLOCATED';

    return {
      label: STAGE_LABELS[stage],
      style: STAGE_STYLES[stage],
      total: active.length
    };
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const getStatus = (order) =>
    order?.status ||
    order?.orderStatus ||
    order?.order_status ||
    'UNKNOWN';

  const getDate = (order) =>
    order?.createdAt ||
    order?.orderDate ||
    order?.date;

  const statuses = useMemo(() => {
    return [
      ...new Set(
        orders.map((order) =>
          String(getStatus(order)).toUpperCase()
        )
      )
    ];
  }, [orders]);

  const filteredOrders = useMemo(() => {
    const searchValue = search.toLowerCase().trim();

    return orders.filter((order) => {
      const status = String(getStatus(order)).toUpperCase();

      const matchesStatus =
        statusFilter === 'ALL' ||
        status === statusFilter;

      const matchesSearch =
        !searchValue ||
        String(order?.id || '')
          .toLowerCase()
          .includes(searchValue) ||
        String(
          order?.customerName ||
          order?.user?.fullName ||
          ''
        )
          .toLowerCase()
          .includes(searchValue) ||
        String(order?.customerEmail || '')
          .toLowerCase()
          .includes(searchValue);

      return matchesStatus && matchesSearch;
    });
  }, [orders, search, statusFilter]);

  const formatCurrency = (value) =>
    `₹${Number(value || 0).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`;

  const formatDate = (value) => {
    if (!value) return 'N/A';

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return 'N/A';

    return date.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  };

  const formatDateTime = (value) => {
    if (!value) return '—';

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return '—';

    return date.toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return <OrdersSkeleton />;
  }

  return (
    <div className="min-h-[calc(100vh-64px)] bg-stone-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">

        {/* HEADER */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 font-serif">
              Order Monitoring
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              Monitor marketplace orders and track fulfillment statuses.
            </p>
          </div>

          <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-stone-100/80 border border-stone-200 rounded-full text-xs font-semibold text-slate-700 self-start sm:self-auto">
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>

            {filteredOrders.length}{' '}
            {filteredOrders.length === 1
              ? 'Order'
              : 'Orders'} Found
          </div>
        </div>

        {/* MAIN CARD CONTAINER */}
        <div className="bg-white border border-stone-200 rounded-2xl shadow-xs overflow-hidden">

          {/* SEARCH & FILTERS BAR */}
          <div className="p-4 sm:p-5 border-b border-stone-100 bg-white">
            <div className="flex flex-col sm:flex-row items-center gap-3">

              {/* Search Field */}
              <div className="relative flex-1 w-full">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                  </svg>
                </div>

                <input
                  type="text"
                  placeholder="Search by Order ID, customer name, or email..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 bg-stone-50/50 border border-stone-200 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-900/10 focus:border-slate-400 transition"
                />

                {search && (
                  <button
                    onClick={() => setSearch('')}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
                  >
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="2"
                        d="M6 18L18 6M6 6l12 12"
                      />
                    </svg>
                  </button>
                )}
              </div>

              {/* Dropdown Filter */}
              <div className="w-full sm:w-56">
                <select
                  value={statusFilter}
                  onChange={(e) =>
                    setStatusFilter(e.target.value)
                  }
                  className="w-full px-3.5 py-2.5 bg-stone-50/50 border border-stone-200 rounded-xl text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-slate-900/10 focus:border-slate-400 transition cursor-pointer"
                >
                  <option value="ALL">All Statuses</option>

                  {statuses.map((status) => (
                    <option key={status} value={status}>
                      {status.replace(/_/g, ' ')}
                    </option>
                  ))}
                </select>
              </div>

            </div>
          </div>

          {/* TABLE SECTION */}
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[900px]">

              <thead>
                <tr className="bg-stone-50/70 border-b border-stone-200 text-xs uppercase tracking-wider font-semibold text-slate-500">
                  <th className="py-3.5 px-5">Order ID</th>
                  <th className="py-3.5 px-5">Customer</th>
                  <th className="py-3.5 px-5">Date</th>
                  <th className="py-3.5 px-5">Total</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5">Fulfillment</th>
                  <th className="py-3.5 px-5"></th>
                </tr>
              </thead>

              <tbody className="divide-y divide-stone-100 text-sm">

                {filteredOrders.length > 0 ? (
                  filteredOrders.map((order) => {

                    const isConfirmed =
                      hasFulfillmentTrail(order);

                    const summary =
                      isConfirmed
                        ? getFulfillmentSummary(order.id)
                        : null;
                    const isExpanded =
                      expandedOrderId === order.id;
                    return (
                      <React.Fragment key={order.id}>
                        <tr className="hover:bg-stone-50/50 transition-colors duration-150">
                          <td className="py-4 px-5 font-mono text-xs font-semibold text-slate-900">
                            #{order.id}
                          </td>
                          <td className="py-4 px-5">
                            <div className="font-semibold text-slate-900">
                              {order.customerName ||
                                order.user?.fullName ||
                                'Customer'}
                            </div>
                            {order.customerEmail && (
                              <div className="text-xs text-slate-400 mt-0.5">
                                {order.customerEmail}
                              </div>
                            )}
                          </td>
                          <td className="py-4 px-5 text-slate-600 font-medium whitespace-nowrap">
                            {formatDate(getDate(order))}
                          </td>
                          <td className="py-4 px-5 font-bold text-slate-900 whitespace-nowrap">
                            {formatCurrency(order.totalAmount)}
                          </td>
                          <td className="py-4 px-5 whitespace-nowrap">
                            <StatusBadge
                              status={getStatus(order)}
                            />
                          </td>
                          <td className="py-4 px-5 whitespace-nowrap">
                            {!isConfirmed ? (
                              <span className="text-xs text-slate-400">
                                —
                              </span>
                            ) : summary?.loading ||
                              summary?.label === null ? (
                              <span className="text-xs text-slate-400">
                                Loading...
                              </span>
                            ) : (
                              <span
                                className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${summary.style}`}
                              >
                                {summary.label}
                                {summary.total
                                  ? ` • ${summary.total} item${
                                      summary.total === 1
                                        ? ''
                                        : 's'
                                    }`
                                  : ''}
                              </span>
                            )}
                          </td>
                          <td className="py-4 px-5 whitespace-nowrap">
                            {isConfirmed && (
                              <button
                                type="button"
                                onClick={() =>
                                  toggleExpandOrder(order.id)
                                }
                                className="text-xs font-semibold text-slate-500 hover:text-slate-900 cursor-pointer"
                              >
                                {isExpanded
                                  ? 'Hide'
                                  : 'Details'}
                              </button>
                            )}
                          </td>
                        </tr>
                        {isExpanded && (
                          <tr>
                            <td
                              colSpan="7"
                              className="bg-stone-50/70 px-5 py-4"
                            >
                              {fulfillmentLoadingIds.has(
                                order.id
                              ) ? (
                                <p className="text-xs text-slate-500">
                                  Loading fulfillment details...
                                </p>
                              ) : (
                                (allocationsByOrder[
                                  order.id
                                ] || []).length === 0 ? (
                                  <p className="text-xs text-slate-500">
                                    No warehouse allocation yet
                                    for this order — it needs
                                    stock received into a
                                    warehouse before it can be
                                    picked.
                                  </p>
                                ) : (
                                  <table className="w-full text-left text-xs">
                                    <thead>
                                      <tr className="text-2xs uppercase tracking-wider font-semibold text-slate-500 border-b border-stone-200">
                                        <th className="py-2 pr-4">
                                          Product
                                        </th>
                                        <th className="py-2 pr-4">
                                          Qty
                                        </th>
                                        <th className="py-2 pr-4">
                                          Warehouse
                                        </th>
                                        <th className="py-2 pr-4">
                                          Status
                                        </th>
                                        <th className="py-2 pr-4">
                                          Last Updated
                                        </th>
                                        <th className="py-2 pr-4"></th>
                                      </tr>
                                    </thead>
                                    <tbody className="divide-y divide-stone-200">
                                      {allocationsByOrder[
                                        order.id
                                      ].map((allocation) => (
                                        <tr
                                          key={
                                            allocation.id
                                          }
                                        >
                                          <td className="py-2 pr-4 font-medium text-slate-800">
                                            {
                                              allocation.productName
                                            }
                                          </td>
                                          <td className="py-2 pr-4 text-slate-600">
                                            {
                                              allocation.quantity
                                            }
                                          </td>
                                          <td className="py-2 pr-4 text-slate-600">
                                            {
                                              allocation.warehouseName
                                            }
                                          </td>
                                          <td className="py-2 pr-4">
                                            <span
                                              className={`inline-flex items-center px-2 py-0.5 rounded-md text-2xs font-semibold border ${
                                                STAGE_STYLES[
                                                  allocation.status
                                                ] ||
                                                'bg-rose-50 text-rose-700 border-rose-200'
                                              }`}
                                            >
                                              {allocation.status ===
                                              'CANCELLED'
                                                ? 'Cancelled'
                                                : STAGE_LABELS[
                                                    allocation.status
                                                  ] ||
                                                  allocation.status}
                                            </span>

                                          </td>

                                          <td className="py-2 pr-4 text-slate-500">
                                            {formatDateTime(
                                              allocation.deliveredAt ||
                                                allocation.readyAt ||
                                                allocation.packedAt ||
                                                allocation.pickedAt ||
                                                allocation.allocatedAt
                                            )}
                                          </td>

                                          <td className="py-2 pr-4">

                                            {allocation.status ===
                                              'READY_FOR_SHIPMENT' && (
                                              <button
                                                type="button"
                                                onClick={() =>
                                                  handleMarkDelivered(
                                                    order.id,
                                                    allocation.orderItemId
                                                  )
                                                }
                                                disabled={
                                                  deliveringItemId ===
                                                  allocation.orderItemId
                                                }
                                                className="px-2.5 py-1 rounded-md text-2xs font-bold bg-slate-900 hover:bg-slate-800 text-white transition disabled:opacity-50 cursor-pointer whitespace-nowrap"
                                              >
                                                {deliveringItemId ===
                                                allocation.orderItemId
                                                  ? 'Updating...'
                                                  : 'Mark Delivered'}
                                              </button>
                                            )}

                                          </td>

                                        </tr>
                                      ))}

                                    </tbody>

                                  </table>
                                )
                              )}

                            </td>
                          </tr>
                        )}

                      </React.Fragment>
                    );
                  })
                ) : (
                  <tr>
                    <td
                      colSpan="7"
                      className="py-12 px-5 text-center"
                    >
                      <div className="max-w-xs mx-auto space-y-2">
                        <div className="w-10 h-10 bg-stone-100 text-slate-400 rounded-full flex items-center justify-center mx-auto mb-3">
                          <svg
                            className="w-5 h-5"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth="2"
                              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
                            />
                          </svg>
                        </div>
                        <p className="text-sm font-semibold text-slate-800">
                          No orders matched your criteria
                        </p>
                        <p className="text-xs text-slate-500">
                          Try searching with different terms or adjusting your status filters.
                        </p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
const StatusBadge = ({ status }) => {
  const value = String(
    status || 'UNKNOWN'
  ).toUpperCase();
  let badgeStyles =
    'bg-slate-100 text-slate-700 border-slate-200';
  if (
    ['COMPLETED', 'DELIVERED', 'PAID'].includes(value)
  ) {
    badgeStyles =
      'bg-emerald-50 text-emerald-700 border-emerald-200';
  } else if (
    ['PENDING', 'PROCESSING', 'SHIPPED'].includes(value)
  ) {
    badgeStyles =
      'bg-amber-50 text-amber-700 border-amber-200';
  } else if (
    ['CANCELLED', 'CANCELED', 'FAILED'].includes(value)
  ) {
    badgeStyles =
      'bg-rose-50 text-rose-700 border-rose-200';
  }
  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeStyles}`}
    >
      {value.replace(/_/g, ' ')}
    </span>
  );
};
const OrdersSkeleton = () => (
  <div className="min-h-[calc(100vh-64px)] bg-stone-50/50 py-8 px-4 sm:px-6 lg:px-8">
    <div className="max-w-7xl mx-auto space-y-6 animate-pulse">

      {/* Header Skeleton */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-2">
          <div className="h-7 w-48 bg-stone-200 rounded-md"></div>
          <div className="h-4 w-72 bg-stone-200 rounded-md"></div>
        </div>

        <div className="h-7 w-24 bg-stone-200 rounded-full"></div>
      </div>

      {/* Card Skeleton */}
      <div className="bg-white border border-stone-200 rounded-2xl overflow-hidden space-y-4 p-5">

        {/* Filter Bar Skeleton */}
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="h-10 bg-stone-100 rounded-xl flex-1 w-full"></div>
          <div className="h-10 bg-stone-100 rounded-xl w-full sm:w-56"></div>
        </div>

        {/* Rows Skeleton */}
        <div className="space-y-3 pt-2">
          {[...Array(6)].map((_, i) => (
            <div
              key={i}
              className="h-12 bg-stone-50 rounded-xl w-full"
            ></div>
          ))}
        </div>

      </div>

    </div>
  </div>
);

export default AdminOrders;