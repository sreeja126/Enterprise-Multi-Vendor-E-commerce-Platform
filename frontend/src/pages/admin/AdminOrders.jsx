import React, { useEffect, useMemo, useState } from 'react';
import { getAdminOrders } from '../../services/adminService';

const AdminOrders = () => {
  const [orders, setOrders] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const response = await getAdminOrders();
      setOrders(Array.isArray(response) ? response : []);
    } catch (err) {
      console.error('Error fetching admin orders:', err);
    } finally {
      setLoading(false);
    }
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
        statusFilter === 'ALL' || status === statusFilter;

      const matchesSearch =
        !searchValue ||
        String(order?.id || '').toLowerCase().includes(searchValue) ||
        String(
          order?.customerName || order?.user?.fullName || ''
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
            {filteredOrders.length} {filteredOrders.length === 1 ? 'Order' : 'Orders'} Found
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
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
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
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                )}
              </div>

              {/* Dropdown Filter */}
              <div className="w-full sm:w-56">
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
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
            <table className="w-full text-left border-collapse min-w-[700px]">
              <thead>
                <tr className="bg-stone-50/70 border-b border-stone-200 text-xs uppercase tracking-wider font-semibold text-slate-500">
                  <th className="py-3.5 px-5">Order ID</th>
                  <th className="py-3.5 px-5">Customer</th>
                  <th className="py-3.5 px-5">Date</th>
                  <th className="py-3.5 px-5">Total</th>
                  <th className="py-3.5 px-5">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100 text-sm">
                {filteredOrders.length > 0 ? (
                  filteredOrders.map((order) => (
                    <tr
                      key={order.id}
                      className="hover:bg-stone-50/50 transition-colors duration-150"
                    >
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
                        <StatusBadge status={getStatus(order)} />
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="py-12 px-5 text-center">
                      <div className="max-w-xs mx-auto space-y-2">
                        <div className="w-10 h-10 bg-stone-100 text-slate-400 rounded-full flex items-center justify-center mx-auto mb-3">
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
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

/* STATUS BADGE COMPONENT */
const StatusBadge = ({ status }) => {
  const value = String(status || 'UNKNOWN').toUpperCase();

  let badgeStyles = 'bg-slate-100 text-slate-700 border-slate-200';

  if (['COMPLETED', 'DELIVERED', 'PAID'].includes(value)) {
    badgeStyles = 'bg-emerald-50 text-emerald-700 border-emerald-200';
  } else if (['PENDING', 'PROCESSING', 'SHIPPED'].includes(value)) {
    badgeStyles = 'bg-amber-50 text-amber-700 border-amber-200';
  } else if (['CANCELLED', 'CANCELED', 'FAILED'].includes(value)) {
    badgeStyles = 'bg-rose-50 text-rose-700 border-rose-200';
  }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeStyles}`}
    >
      {value.replace(/_/g, ' ')}
    </span>
  );
};

/* LOADING SKELETON */
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
            <div key={i} className="h-12 bg-stone-50 rounded-xl w-full"></div>
          ))}
        </div>

      </div>

    </div>
  </div>
);

export default AdminOrders;