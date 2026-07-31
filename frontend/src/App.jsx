import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Profile from "./pages/Profile";
import EditProfile from "./pages/EditProfile";
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
            </Routes>
        </BrowserRouter>
    );
}

export default App;