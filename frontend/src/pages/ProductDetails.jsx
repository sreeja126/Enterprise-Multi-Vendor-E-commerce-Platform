import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getProductById } from "../services/productService";

function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        if (!id || id === "undefined") {
          setError("Invalid Product ID.");
          return;
        }

        const response = await getProductById(id);
        
        // Safe extraction handling both direct data & Axios responses
        const data = response?.data?.data || response?.data || response;

        if (data && typeof data === "object") {
          setProduct(data);
        } else {
          setError("Product not found.");
        }
      } catch (err) {
        console.error("Error fetching product details:", err);
        setError("Unable to fetch product details.");
      }
    };

    fetchProduct();
  }, [id]);

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-stone-50 gap-4">
        <h2 className="text-xl font-medium text-rose-600">{error}</h2>
        <button
          onClick={() => navigate(-1)}
          className="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm hover:bg-slate-900 transition"
        >
          Go Back
        </button>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50">
        <h2 className="text-xl font-medium text-slate-500">Loading product…</h2>
      </div>
    );
  }

  // Safe Fallback SVG (100% offline & instant)
  const FALLBACK_IMAGE =
    "data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22400%22%20height%3D%22250%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20400%20250%22%3E%3Crect%20fill%3D%22%23e2e8f0%22%20width%3D%22400%22%20height%3D%22250%22%2F%3E%3Ctext%20fill%3D%22%2364748b%22%20font-family%3D%22sans-serif%22%20font-size%3D%2218%22%20text-anchor%3D%22middle%22%20x%3D%22200%22%20y%3D%22130%22%3ENo%20Image%20Available%3C%2Ftext%3E%3C%2Fsvg%3E";

  // Handles imageUrl (string), image (string), or images (array)
  const imageSrc =
    product.imageUrl ||
    product.image ||
    (Array.isArray(product.images) && product.images.length > 0
      ? product.images[0]
      : FALLBACK_IMAGE);

  // Safe category and stock mapping
  const categoryName = product.categoryName || product.category?.name;
  const stockCount = product.stockQuantity ?? product.stock ?? 0;
  const productId = product.id || product._id || id;

  return (
    <div className="min-h-screen bg-stone-50 py-10 px-4">
      <div className="max-w-5xl mx-auto bg-white rounded-2xl shadow-md border border-stone-100 overflow-hidden">
        <div className="grid md:grid-cols-2">
          <div className="bg-stone-100 flex items-center justify-center min-h-[300px]">
            <img
              src={imageSrc}
              alt={product.name}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = FALLBACK_IMAGE;
              }}
            />
          </div>

          <div className="p-8 md:p-10 flex flex-col justify-between">
            <div>
              {categoryName && (
                <span className="inline-block bg-emerald-50 text-emerald-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-4">
                  {categoryName}
                </span>
              )}

              <h1 className="text-3xl md:text-4xl font-serif font-bold text-slate-900 mb-2">
                {product.name}
              </h1>

              <p className="text-slate-500 mb-6">{product.brand || "N/A"}</p>

              <p className="text-3xl font-bold text-amber-600 mb-6">
                ₹{product.price}
              </p>

              <p className="text-slate-600 leading-relaxed mb-6">
                {product.description || "No description available."}
              </p>

              <div className="space-y-2 text-sm border-t border-stone-100 pt-6">
                <p className="text-slate-700">
                  <span className="font-semibold">Stock:</span> {stockCount}
                </p>

                {stockCount === 0 ? (
                  <p className="text-red-600 font-semibold">❌ Out of Stock</p>
                ) : stockCount <= 5 ? (
                  <p className="text-orange-500 font-semibold">⚠️ Low Stock</p>
                ) : (
                  <p className="text-green-600 font-semibold">✅ In Stock</p>
                )}

                {product.vendor && (
                  <div className="pt-2 text-slate-600">
                    <p>
                      <span className="font-semibold text-slate-800">
                        Sold by:
                      </span>{" "}
                      {product.vendor.name || product.vendor.username}
                    </p>
                    {product.vendor.email && (
                      <p>
                        <span className="font-semibold text-slate-800">
                          Vendor email:
                        </span>{" "}
                        {product.vendor.email}
                      </p>
                    )}
                  </div>
                )}
              </div>
            </div>

            <div className="flex gap-4 mt-10">
              <button
                onClick={() => navigate(-1)}
                className="flex-1 border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
              >
                Back
              </button>

              <button
                onClick={() => navigate(`/products/edit/${productId}`)}
                className="flex-1 bg-emerald-700 hover:bg-emerald-800 text-white py-3 rounded-lg font-semibold transition shadow-sm text-center"
              >
                Edit Product
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ProductDetails;