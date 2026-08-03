import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Profile from "./pages/Profile";
import EditProfile from "./pages/EditProfile";
import AddProduct from "./pages/AddProduct";
import ProductList from "./pages/ProductList";
import ProductDetails from "./pages/ProductDetails";
import EditProduct from "./pages/EditProduct";
import MyProducts from "./pages/MyProducts";
import VendorDashboard from "./pages/VendorDashboard";
function App() {
    return (
        <BrowserRouter>
            <Routes>
               <Route path="/" element={<Navigate to="/login" replace />} />
                 <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route   path="/profile"   element={<Profile />} />
               <Route  path="/editprofile" element={<EditProfile />} />
               <Route path="/addproduct" element={<AddProduct />} />
               <Route path="/products" element={<ProductList />} />
               <Route   path="/products/:id" element={<ProductDetails />}/>
               <Route path="/editproduct/:id" element={<EditProduct />}/>
               <Route path="/vendor/dashboard" element={<VendorDashboard />}/>
               <Route path="/myproducts" element={<MyProducts />}/>
            </Routes>
        </BrowserRouter>
    );
}

export default App;