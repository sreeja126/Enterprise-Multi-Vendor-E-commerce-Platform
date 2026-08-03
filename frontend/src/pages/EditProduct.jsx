import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getProductById,
  updateProduct,
} from "../services/productService";
import { getAllCategories } from "../services/categoryService";

function EditProduct() {

  const { id } = useParams();
  const navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  const [formData, setFormData] = useState({
    name: "",
    brand: "",
    description: "",
    price: "",
    stock: "",
    categoryId: "",
    imageUrl: ""
  });

  useEffect(() => {

    const loadData = async () => {

      try {

        const [productResult, categoriesResult] = await Promise.allSettled([
          getProductById(id),
          getAllCategories()
        ]);

        const categoryList =
          categoriesResult.status === "fulfilled" &&
          Array.isArray(categoriesResult.value.data)
            ? categoriesResult.value.data
            : [];

        setCategories(categoryList);

        if (categoriesResult.status === "rejected") {
          console.error(categoriesResult.reason);
        }

        if (productResult.status === "rejected") {
          console.error(productResult.reason);
          alert("Unable to load this product.");
          return;
        }

        const product = productResult.value.data;

        // The product read model only returns the category's name
        // (categoryName), not its id, so match it back against the
        // category list to preselect the right option.
        const matchedCategory = categoryList.find(
          (cat) => cat.name === product.categoryName
        );

        setFormData({
          name: product.name || "",
          brand: product.brand || "",
          description: product.description || "",
          price: product.price ?? "",
          stock: product.stock ?? "",
          categoryId: matchedCategory ? matchedCategory.id : "",
          imageUrl: product.images && product.images.length > 0
            ? product.images[0]
            : ""
        });

      } catch (error) {

        console.error(error);

      } finally {
        setLoading(false);
      }

    };

    loadData();

  }, [id]);

  const handleChange = (e) => {

    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });

  };

  const handleSubmit = async (e) => {

    e.preventDefault();

    try {

      await updateProduct(id, {
        name: formData.name,
        brand: formData.brand,
        description: formData.description,
        price: Number(formData.price),
        stock: Number(formData.stock),
        category: formData.categoryId ? { id: Number(formData.categoryId) } : null,
        images: formData.imageUrl ? [formData.imageUrl] : []
      });

      navigate("/myproducts");

    } catch (error) {

      console.error(error);
      alert("Update failed.");

    }

  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <p className="text-slate-500">Loading product…</p>
      </div>
    );
  }

  return (

    <div className="min-h-screen bg-stone-50 flex justify-center items-center py-12 px-4">

      <div className="bg-white shadow-md border border-stone-100 rounded-2xl p-8 md:p-10 w-full max-w-2xl">

        <div className="mb-8 text-center">
          <span className="inline-block bg-amber-50 text-amber-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-3">
            Vendor · Edit Listing
          </span>
          <h1 className="text-3xl font-serif font-bold text-slate-900">
            Edit Product
          </h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">

          <div>
            <label className="block mb-2 text-sm font-medium text-slate-700">
              Product Name
            </label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
            />
          </div>

          <div>
            <label className="block mb-2 text-sm font-medium text-slate-700">
              Brand
            </label>
            <input
              type="text"
              name="brand"
              value={formData.brand}
              onChange={handleChange}
              required
              className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
            />
          </div>

          <div>
            <label className="block mb-2 text-sm font-medium text-slate-700">
              Description
            </label>
            <textarea
              name="description"
              rows="4"
              value={formData.description}
              onChange={handleChange}
              required
              className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
            />
          </div>

          <div className="grid grid-cols-2 gap-5">

            <div>
              <label className="block mb-2 text-sm font-medium text-slate-700">
                Price (₹)
              </label>
              <input
                type="number"
                name="price"
                min="0"
                step="0.01"
                value={formData.price}
                onChange={handleChange}
                required
                className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
              />
            </div>

            <div>
              <label className="block mb-2 text-sm font-medium text-slate-700">
                Stock
              </label>
              <input
                type="number"
                name="stock"
                min="0"
                value={formData.stock}
                onChange={handleChange}
                required
                className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
              />
            </div>

          </div>

          <div>
            <label className="block mb-2 text-sm font-medium text-slate-700">
              Category
            </label>
            <select
              name="categoryId"
              value={formData.categoryId}
              onChange={handleChange}
              required
              className="w-full border border-stone-300 rounded-lg px-4 py-3 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
            >
              <option value="" disabled>Select a category</option>
              {Array.isArray(categories) && categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block mb-2 text-sm font-medium text-slate-700">
              Image URL
            </label>
            <input
              type="text"
              name="imageUrl"
              value={formData.imageUrl}
              onChange={handleChange}
              placeholder="https://..."
              className="w-full border border-stone-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
            />
          </div>

          <div className="flex gap-4 pt-2">

            <button
              type="button"
              onClick={() => navigate("/myproducts")}
              className="flex-1 border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
            >
              Cancel
            </button>

            <button
              type="submit"
              className="flex-1 bg-emerald-700 hover:bg-emerald-800 text-white py-3 rounded-lg font-semibold transition shadow-sm"
            >
              Save Changes
            </button>

          </div>

        </form>

      </div>

    </div>

  );

}

export default EditProduct;
