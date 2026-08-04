import React, { useState, useEffect } from 'react';
import productService from '../services/productService';

const CustomerProducts = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [purchasingId, setPurchasingId] = useState(null);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const data = await productService.getAllProducts();
      setProducts(data);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePurchase = async (productId) => {
    setPurchasingId(productId);
    try {
      await productService.reduceStockOnOrder(productId, 1);
      await fetchProducts();
      alert('Order placed successfully!');
    } catch (error) {
      alert(error.response?.data || 'Failed to complete purchase. Product may be out of stock.');
    } finally {
      setPurchasingId(null);
    }
  };

  if (loading) return <div style={{ padding: '20px' }}>Loading catalog...</div>;

  // Standalone SVG fallback (no external network dependencies)
  const FALLBACK_IMAGE =
    "data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22400%22%20height%3D%22250%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20400%20250%22%3E%3Crect%20fill%3D%22%23e2e8f0%22%20width%3D%22400%22%20height%3D%22250%22%2F%3E%3Ctext%20fill%3D%22%2364748b%22%20font-family%3D%22sans-serif%22%20font-size%3D%2218%22%20text-anchor%3D%22middle%22%20x%3D%22200%22%20y%3D%22130%22%3ENo%20Image%20Available%3C%2Ftext%3E%3C%2Fsvg%3E";

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <h2>Marketplace Products</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '20px', marginTop: '20px' }}>
        {products.map((product) => {
          // Safe stock detection matching backend ResponseDTO fields
          const stockCount = product.stock ?? product.stockQuantity ?? 0;
          const isOutOfStock = stockCount <= 0 || product.isOutOfStock;

          // Safe image detection (checks images array first, then fallback properties)
          const imgSrc =
            (Array.isArray(product.images) && product.images.length > 0 && product.images[0])
              ? product.images[0]
              : product.imageUrl || product.image || FALLBACK_IMAGE;

          return (
            <div
              key={product.id}
              style={{
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                padding: '16px',
                backgroundColor: '#ffffff',
                boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
                position: 'relative'
              }}
            >
              <img
                src={imgSrc}
                alt={product.name || 'Product'}
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = FALLBACK_IMAGE;
                }}
                style={{ width: '100%', height: '180px', objectFit: 'cover', borderRadius: '4px' }}
              />

              <h3 style={{ margin: '12px 0 8px 0' }}>{product.name}</h3>
              <p style={{ color: '#64748b', fontSize: '14px', height: '40px', overflow: 'hidden' }}>
                {product.description || 'No description provided.'}
              </p>
              
              <div style={{ margin: '12px 0', fontWeight: 'bold', fontSize: '18px' }}>
                ₹{product.price ? Number(product.price).toFixed(2) : '0.00'}
              </div>

              <div style={{ marginBottom: '12px' }}>
                {isOutOfStock ? (
                  <span style={{ color: '#ef4444', fontWeight: 'bold', padding: '4px 8px', backgroundColor: '#fef2f2', borderRadius: '4px' }}>
                    Out of Stock
                  </span>
                ) : (
                  <span style={{ color: '#10b981', fontSize: '14px' }}>
                    In Stock: <strong>{stockCount}</strong>
                  </span>
                )}
              </div>

              <button
                disabled={isOutOfStock || purchasingId === product.id}
                onClick={() => handlePurchase(product.id)}
                style={{
                  width: '100%',
                  padding: '10px',
                  backgroundColor: isOutOfStock ? '#cbd5e1' : '#3b82f6',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: isOutOfStock ? 'not-allowed' : 'pointer',
                  fontWeight: '600'
                }}
              >
                {purchasingId === product.id ? 'Processing...' : isOutOfStock ? 'Out of Stock' : 'Buy Now'}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default CustomerProducts;