import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAllProducts,
  deleteProduct,
  searchProducts,
  getProductsByCategory,
} from "../services/productService";
import { getAllCategories } from "../services/categoryService";

function ProductList() {
  const navigate = useNavigate();

  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [categoryId, setCategoryId] = useState("");

  useEffect(() => {
    fetchProducts();
    fetchCategories();
  }, []);

  const fetchProducts = async () => {
    try {
      const response = await getAllProducts();
      setProducts(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error(error);
      setProducts([]);
    }
  };

  const fetchCategories = async () => {
    try {
      const response = await getAllCategories();
      setCategories(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      // Backend unreachable, category endpoint down, etc. — the category
      // filter just won't have options; it should never crash the page.
      console.error(error);
      setCategories([]);
    }
  };

  const handleSearch = async () => {
    try {
      if (keyword.trim() === "") {
        fetchProducts();
        return;
      }

      const response = await searchProducts(keyword);
      setProducts(response.data);
    } catch (error) {
      console.error(error);
      alert("Search failed.");
    }
  };

  const handleCategoryFilter = async () => {
    try {

      if (categoryId === "") {
        fetchProducts();
        return;
      }

      // The backend expects a category id, not a name.
      const response = await getProductsByCategory(categoryId);

      setProducts(response.data);

    } catch (error) {

      console.error(error);
      alert("Failed to filter products.");

    }
  };

  const handleDelete = async (id) => {
    const confirmDelete = window.confirm(
      "Are you sure you want to delete this product?"
    );

    if (!confirmDelete) return;

    try {
      await deleteProduct(id);
      fetchProducts();
    } catch (error) {
      console.error(error);
      alert("Delete failed.");
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 p-6 md:p-8">
      <div className="max-w-7xl mx-auto">

        {/* Header */}
        <div className="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-serif font-bold text-slate-900">
              Shop All Products
            </h1>
            <p className="text-slate-500 mt-1">
              {products.length} item{products.length !== 1 ? "s" : ""} available across all vendors
            </p>
          </div>

          <button
            onClick={() => navigate("/addproduct")}
            className="bg-emerald-700 hover:bg-emerald-800 text-white px-5 py-3 rounded-lg font-semibold transition shadow-sm whitespace-nowrap"
          >
            + Add Product
          </button>
        </div>

        {/* Search & Filter Bar */}
        <div className="flex flex-col md:flex-row gap-4 mb-8 bg-white p-4 rounded-xl border border-stone-100 shadow-sm">

          <input
            type="text"
            placeholder="Search products…"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="flex-1 border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600"
          />

          <button
            onClick={handleSearch}
            className="bg-slate-800 hover:bg-slate-900 text-white px-6 py-3 rounded-lg font-medium transition"
          >
            Search
          </button>

          <select
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            className="border border-stone-300 rounded-lg px-4 py-3 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-600"
          >
            <option value="">All Categories</option>
            {Array.isArray(categories) && categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>

          <button
            onClick={handleCategoryFilter}
            className="bg-emerald-700 hover:bg-emerald-800 text-white px-6 py-3 rounded-lg font-medium transition"
          >
            Filter
          </button>

        </div>

        {!Array.isArray(products) || products.length === 0 ? (
          <div className="text-center text-slate-400 mt-20">
            <h2 className="text-2xl font-medium">No products available</h2>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {products.map((product) => {
              const imageSrc =
                product.images && product.images.length > 0
                  ? product.images[0]
                  : "https://via.placeholder.com/400x250?text=No+Image";

              return (
                <div
                  key={product.id}
                  className="bg-white rounded-2xl shadow-sm border border-stone-100 overflow-hidden hover:shadow-xl transition-shadow duration-300"
                >
                  <img
                    src={imageSrc}
                    alt={product.name}
                    className="w-full h-52 object-cover"
                  />

                  <div className="p-5">

                    {product.categoryName && (
                      <span className="inline-block bg-emerald-50 text-emerald-700 text-xs font-semibold uppercase tracking-wide px-2.5 py-1 rounded-full mb-2">
                        {product.categoryName}
                      </span>
                    )}

                    <h2 className="text-lg font-bold text-slate-900 leading-snug">
                      {product.name}
                    </h2>
                    <p className="text-sm text-slate-500 mb-2">{product.brand}</p>

                    <p className="text-slate-600 text-sm line-clamp-2 mb-3">
                      {product.description}
                    </p>

                    <div className="flex items-center justify-between mb-3">
                      <p className="text-xl font-bold text-amber-600">
                        ₹{product.price}
                      </p>
                      <p className="text-xs text-slate-400">
                        {product.stock > 0 ? `${product.stock} in stock` : "Out of stock"}
                      </p>
                    </div>

                    {product.vendor && (
                      <p className="text-xs text-slate-400 mb-4">
                        Sold by <span className="font-medium text-slate-600">{product.vendor.name}</span>
                      </p>
                    )}

                    <div className="flex gap-2">
                      <button
                        onClick={() => navigate(`/products/${product.id}`)}
                        className="flex-1 bg-slate-800 hover:bg-slate-900 text-white py-2 rounded-lg text-sm font-medium transition"
                      >
                        View
                      </button>

                      <button
                        onClick={() => navigate(`/editproduct/${product.id}`)}
                        className="flex-1 border border-stone-300 text-slate-700 hover:bg-stone-100 py-2 rounded-lg text-sm font-medium transition"
                      >
                        Edit
                      </button>

                      <button
                        onClick={() => handleDelete(product.id)}
                        className="flex-1 border border-rose-200 text-rose-600 hover:bg-rose-50 py-2 rounded-lg text-sm font-medium transition"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default ProductList;
