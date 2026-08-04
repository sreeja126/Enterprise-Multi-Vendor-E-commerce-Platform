import React, { useState, useEffect } from 'react';
import productService from '../services/productService';

const MyProducts = () => {
  const [products, setProducts] = useState([]);
  const [updatingId, setUpdatingId] = useState(null);
  const [stockInputs, setStockInputs] = useState({});

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const data = await productService.getAllProducts();
      setProducts(data);
      const initialInputs = {};
      data.forEach((p) => {
        initialInputs[p.id] = p.stockQuantity;
      });
      setStockInputs(initialInputs);
    } catch (err) {
      console.error('Failed to load products', err);
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
      alert('Stock quantity updated successfully.');
    } catch (err) {
      alert('Failed to update stock quantity.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
      <h2>Vendor Inventory Management</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
        <thead>
          <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left', borderBottom: '2px solid #e2e8f0' }}>
            <th style={{ padding: '12px' }}>Product</th>
            <th style={{ padding: '12px' }}>Price</th>
            <th style={{ padding: '12px' }}>Current Stock</th>
            <th style={{ padding: '12px' }}>Update Stock</th>
            <th style={{ padding: '12px' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id} style={{ borderBottom: '1px solid #e2e8f0' }}>
              <td style={{ padding: '12px' }}>{p.name}</td>
             <td style={{ padding: '12px' }}>₹{p.price?.toFixed(2)}</td>
              <td style={{ padding: '12px' }}>
                {p.stockQuantity <= 0 ? (
                  <span style={{ color: '#ef4444', fontWeight: 'bold' }}>0 (Out of Stock)</span>
                ) : (
                  <span>{p.stockQuantity}</span>
                )}
              </td>
              <td style={{ padding: '12px' }}>
                <input
                  type="number"
                  min="0"
                  value={stockInputs[p.id] !== undefined ? stockInputs[p.id] : ''}
                  onChange={(e) => handleInputChange(p.id, e.target.value)}
                  style={{ width: '90px', padding: '6px', borderRadius: '4px', border: '1px solid #cbd5e1' }}
                />
              </td>
              <td style={{ padding: '12px' }}>
                <button
                  onClick={() => handleUpdateStock(p.id)}
                  disabled={updatingId === p.id}
                  style={{
                    padding: '6px 12px',
                    backgroundColor: '#10b981',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  {updatingId === p.id ? 'Saving...' : 'Save Stock'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default MyProducts;