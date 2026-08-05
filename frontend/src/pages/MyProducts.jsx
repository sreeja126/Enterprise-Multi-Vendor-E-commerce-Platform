import React, { useState, useEffect } from 'react';
import productService from '../services/productService';

const MyProducts = () => {
  const [products, setProducts] = useState([]);
  const [updatingId, setUpdatingId] = useState(null);
  const [stockInputs, setStockInputs] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const data = await productService.getAllProducts();
      setProducts(data);
      const initialInputs = {};
      data.forEach((p) => {
        initialInputs[p.id] = p.stockQuantity ?? p.stock ?? 0;
      });
      setStockInputs(initialInputs);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (id, value) => {
    setStockInputs((prev) => ({
      ...prev,
      [id]: value
    }));
  };

  const handleUpdateStock = async (id) => {
    const newQuantity = parseInt(stockInputs[id], 10);
    if (isNaN(newQuantity) || newQuantity < 0) {
      alert('Please enter a valid non-negative integer for quantity.');
      return;
    }

    setUpdatingId(id);
    try {
      await productService.updateStockQuantity(id, newQuantity);
      await fetchProducts();
    } catch (err) {
      alert('Failed to update stock quantity.');
    } finally {
      setUpdatingId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500 font-medium">Loading inventory…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-5xl mx-auto">

        <div className="mb-8">
          <h1 className="text-4xl font-serif font-bold text-slate-900">
            Vendor Inventory
          </h1>
          <p className="text-slate-500 mt-1">
            Manage stock levels for {products.length} listing{products.length !== 1 ? "s" : ""}
          </p>
        </div>

        {products.length === 0 ? (
          <div className="text-center mt-20 bg-white border border-stone-100 rounded-2xl py-16 px-6">
            <h2 className="text-2xl font-semibold text-slate-700">
              You haven't listed any products yet
            </h2>
          </div>
        ) : (
          <div className="bg-white rounded-2xl border border-stone-100 shadow-sm overflow-hidden">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-stone-50 border-b border-stone-100 text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-semibold">Product</th>
                  <th className="px-5 py-3 font-semibold">Price</th>
                  <th className="px-5 py-3 font-semibold">Current Stock</th>
                  <th className="px-5 py-3 font-semibold">Update Stock</th>
                  <th className="px-5 py-3 font-semibold"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {products.map((p) => {
                  const currentStock = p.stockQuantity ?? p.stock ?? 0;

                  return (
                    <tr key={p.id} className="hover:bg-stone-50/60 transition">
                      <td className="px-5 py-4 font-medium text-slate-900">{p.name}</td>
                      <td className="px-5 py-4 text-amber-600 font-semibold">
                        ₹{p.price?.toFixed ? p.price.toFixed(2) : p.price}
                      </td>
                      <td className="px-5 py-4">
                        {currentStock <= 0 ? (
                          <span className="text-rose-600 font-semibold text-sm">0 · Out of Stock</span>
                        ) : (
                          <span className="text-slate-700 text-sm">{currentStock}</span>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        <input
                          type="number"
                          min="0"
                          value={stockInputs[p.id] !== undefined ? stockInputs[p.id] : ''}
                          onChange={(e) => handleInputChange(p.id, e.target.value)}
                          className="w-24 border border-stone-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
                        />
                      </td>
                      <td className="px-5 py-4">
                        <button
                          onClick={() => handleUpdateStock(p.id)}
                          disabled={updatingId === p.id}
                          className="px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-700 hover:bg-emerald-800 text-white transition disabled:opacity-60"
                        >
                          {updatingId === p.id ? 'Saving…' : 'Save'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default MyProducts;
