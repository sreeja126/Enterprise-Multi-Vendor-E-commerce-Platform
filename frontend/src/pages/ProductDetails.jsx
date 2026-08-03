import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getProductById } from "../services/productService";

function ProductDetails() {

  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState(null);

  useEffect(() => {

    const fetchProduct = async () => {

      try {

        const response = await getProductById(id);

        setProduct(response.data);

      } catch (error) {

        console.error(error);
        alert("Unable to fetch product.");

      }

    };

    fetchProduct();

  }, [id]);

  if (!product) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50">
        <h2 className="text-xl font-medium text-slate-500">Loading product…</h2>
      </div>
    );
  }

  const imageSrc =
    product.images && product.images.length > 0
      ? product.images[0]
      : "https://via.placeholder.com/500x500?text=No+Image";

  return (

    <div className="min-h-screen bg-stone-50 py-10 px-4">

      <div className="max-w-5xl mx-auto bg-white rounded-2xl shadow-md border border-stone-100 overflow-hidden">

        <div className="grid md:grid-cols-2">

          <div className="bg-stone-100">
            <img
              src={imageSrc}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          </div>

          <div className="p-8 md:p-10">

            {product.categoryName && (
              <span className="inline-block bg-emerald-50 text-emerald-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-4">
                {product.categoryName}
              </span>
            )}

            <h1 className="text-3xl md:text-4xl font-serif font-bold text-slate-900 mb-2">
              {product.name}
            </h1>

            <p className="text-slate-500 mb-6">{product.brand}</p>

            <p className="text-3xl font-bold text-amber-600 mb-6">
              ₹{product.price}
            </p>

            <p className="text-slate-600 leading-relaxed mb-6">
              {product.description}
            </p>

            <div className="space-y-2 text-sm border-t border-stone-100 pt-6">

              <p className="text-slate-600">
                <span className="font-semibold text-slate-800">Stock:</span>{" "}
                {product.stock > 0 ? `${product.stock} available` : "Out of stock"}
              </p>

              {product.vendor && (
                <>
                  <p className="text-slate-600">
                    <span className="font-semibold text-slate-800">Sold by:</span>{" "}
                    {product.vendor.name}
                  </p>
                  <p className="text-slate-600">
                    <span className="font-semibold text-slate-800">Vendor email:</span>{" "}
                    {product.vendor.email}
                  </p>
                </>
              )}

            </div>

            <div className="flex gap-4 mt-10">

              <button
                onClick={() => navigate("/products")}
                className="flex-1 border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
              >
                Back to Products
              </button>

              <button
                onClick={() => navigate(`/editproduct/${product.id}`)}
                className="flex-1 bg-emerald-700 hover:bg-emerald-800 text-white py-3 rounded-lg font-semibold transition shadow-sm"
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
