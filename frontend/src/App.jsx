import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Profile from "./pages/Profile";
import EditProfile from "./pages/EditProfile";
import AddProduct from "./pages/AddProduct";
import ProductList from "./pages/ProductList";
import ProductDetails from "./pages/ProductDetails";
import EditProduct from "./pages/EditProduct";
import MyProducts from "./pages/MyProducts";
import VendorDashboard from "./pages/VendorDashboard";
import CustomerDashboard from "./pages/CustomerDashboard";
import ForgotPassword from "./pages/ForgotPassword";
import CustomerProducts from "./pages/CustomerProducts";
import ViewProduct from "./pages/ViewProduct";
function App() {
    return (
        <BrowserRouter>
            <Routes>
               <Route path="/" element={<Navigate to="/login" replace />} />
               <Route path="/vendor-dashboard" element={<VendorDashboard />} />
               <Route path="/customer-dashboard" element={<CustomerDashboard />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route   path="/profile"   element={<Profile />} />
               <Route  path="/editprofile" element={<EditProfile />} />
               <Route path="/addproduct" element={<AddProduct />} />
               <Route path="/products" element={<ProductList />} />
               <Route   path="/products/:id" element={<ProductDetails />}/>
              <Route path="/products/edit/:id" element={<EditProduct />} />
               <Route path="/products/:id" element={<ViewProduct />} />
               <Route path="/myproducts" element={<MyProducts />}/>
               <Route path="/forgot-password" element={<ForgotPassword />} />
               <Route  path="/shop"  element={<CustomerProducts />}/>
            </Routes>
        </BrowserRouter>
    );
}

export default App;