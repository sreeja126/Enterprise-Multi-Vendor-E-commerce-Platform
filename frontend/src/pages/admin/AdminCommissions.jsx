import React, { useEffect, useMemo, useState } from 'react';
import { getAdminCommissions } from '../../services/adminService';

const AdminCommissions = () => {
  const [commissions, setCommissions] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadCommissions = async () => {
    try {
      setLoading(true);
      const response = await getAdminCommissions();
      setCommissions(Array.isArray(response) ? response : []);
    } catch (err) {
      console.error('Failed to fetch admin commissions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCommissions();
  }, []);

  const totalCommission = useMemo(() => {
    return commissions.reduce(
      (total, item) => total + Number(item?.totalCommissionEarned || 0),
      0
    );
  }, [commissions]);

  const formatCurrency = (value) =>
    `₹${Number(value || 0).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`;

  if (loading) {
    return <LoadingSkeleton />;
  }

  return (
    <div className="min-h-[calc(100vh-64px)] bg-stone-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">
        
        {/* PAGE HEADER */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 font-serif">
              Commission Management
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Monitor vendor sales performance and earned marketplace commissions.
            </p>
          </div>

          {/* TOTAL COMMISSION METRIC BADGE */}
          <div className="inline-flex items-center gap-3 bg-emerald-50 border border-emerald-200/80 px-4 py-3 rounded-xl shadow-xs">
            <div className="p-2 bg-emerald-100 rounded-lg text-emerald-700">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <span className="text-xs font-medium text-emerald-800 uppercase tracking-wider block">
                Total Revenue Earned
              </span>
              <span className="text-lg font-bold text-emerald-900">
                {formatCurrency(totalCommission)}
              </span>
            </div>
          </div>
        </div>

        {/* DATA TABLE CONTAINER */}
        <div className="bg-white border border-stone-200 rounded-2xl shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-stone-50/80 border-b border-stone-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th scope="col" className="px-6 py-4">Vendor ID</th>
                  <th scope="col" className="px-6 py-4">Vendor</th>
                  <th scope="col" className="px-6 py-4">Vendor Sales</th>
                  <th scope="col" className="px-6 py-4">Commission %</th>
                  <th scope="col" className="px-6 py-4 text-right">Commission Earned</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-stone-100 text-sm text-slate-700">
                {commissions.length > 0 ? (
                  commissions.map((commission) => (
                    <tr 
                      key={commission.vendorId} 
                      className="hover:bg-stone-50/60 transition-colors"
                    >
                      {/* Vendor ID */}
                      <td className="px-6 py-4 font-mono text-xs text-slate-500">
                        #{commission.vendorId}
                      </td>

                      {/* Vendor Name */}
                      <td className="px-6 py-4 font-semibold text-slate-900">
                        {commission.vendorName || 'N/A'}
                      </td>

                      {/* Vendor Sales */}
                      <td className="px-6 py-4 text-slate-600 font-medium">
                        {formatCurrency(commission.totalVendorSales)}
                      </td>

                      {/* Commission % Badge */}
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-stone-100 text-slate-700 border border-stone-200">
                          {Number(commission.commissionPercentage || 0).toFixed(2)}%
                        </span>
                      </td>

                      {/* Commission Earned */}
                      <td className="px-6 py-4 font-bold text-emerald-700 text-right">
                        {formatCurrency(commission.totalCommissionEarned)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="px-6 py-12 text-center text-slate-500">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z" />
                        </svg>
                        <span>No commission records available.</span>
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

/* LOADING SKELETON COMPONENT */
const LoadingSkeleton = () => (
  <div className="min-h-[calc(100vh-64px)] bg-stone-50/50 py-8 px-4 sm:px-6 lg:px-8">
    <div className="max-w-7xl mx-auto space-y-6 animate-pulse">
      
      {/* Header Skeleton */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-2">
          <div className="h-7 w-64 bg-stone-200 rounded-md"></div>
          <div className="h-4 w-80 bg-stone-200 rounded-md"></div>
        </div>
        <div className="h-16 w-48 bg-stone-200 rounded-xl"></div>
      </div>

      {/* Table Card Skeleton */}
      <div className="bg-white border border-stone-200 rounded-2xl p-6 space-y-4">
        <div className="h-8 bg-stone-100 rounded-md w-full"></div>
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-12 bg-stone-50 rounded-md w-full"></div>
        ))}
      </div>

    </div>
  </div>
);

export default AdminCommissions;