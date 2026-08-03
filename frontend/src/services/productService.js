import API from "./api";

// Add Product
export const addProduct = (productData) =>
  API.post("/products", productData);

// Get All Products
export const getAllProducts = () =>
  API.get("/products");

// Get Product By Id
export const getProductById = (id) =>
  API.get(`/products/${id}`);

// Update Product
export const updateProduct = (id, productData) =>
  API.put(`/products/${id}`, productData);

// Delete Product
export const deleteProduct = (id) =>
  API.delete(`/products/${id}`);

// Get Logged-in Vendor Products
export const getVendorProducts = () =>
  API.get("/products/vendor");


// Filter By Category
export const getProductsByCategory = (category) =>
  API.get(`/products/category/${category}`);
export const searchProducts = (keyword) =>
    API.get(`/products/search?keyword=${keyword}`);
