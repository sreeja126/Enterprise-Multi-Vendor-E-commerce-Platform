import { useNavigate } from "react-router-dom";

function Dashboard() {

  const navigate = useNavigate();

  const token = localStorage.getItem("token");

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-slate-100 flex items-center justify-center">

      <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl p-8">

        <div className="text-center">

          <h1 className="text-4xl font-bold text-blue-600">
            Welcome to ShopStack 🚀
          </h1>

          <p className="text-slate-600 mt-3">
            You have successfully logged in.
          </p>

        </div>

        <div className="mt-8 rounded-xl bg-slate-100 p-5">

          <h2 className="text-xl font-semibold text-slate-800">
            Dashboard
          </h2>

          <p className="text-slate-600 mt-2">
            Manage your account, view your profile and update your information.
          </p>

          <div className="mt-4 flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-green-500"></span>
            <span className="text-green-700 font-medium">
              Logged In
            </span>
          </div>

        </div>

        <div className="grid grid-cols-1 gap-4 mt-8">

          <button
            onClick={() => navigate("/profile")}
            className="w-full rounded-lg bg-blue-600 py-3 text-white font-semibold hover:bg-blue-700 transition"
          >
            👤 View Profile
          </button>

          <button
            onClick={() => navigate("/editprofile")}
            className="w-full rounded-lg bg-emerald-600 py-3 text-white font-semibold hover:bg-emerald-700 transition"
          >
            ✏️ Edit Profile
          </button>

          <button
            onClick={handleLogout}
            className="w-full rounded-lg bg-red-500 py-3 text-white font-semibold hover:bg-red-600 transition"
          >
            🚪 Logout
          </button>

        </div>

        <div className="mt-8 border-t pt-4 text-center text-sm text-slate-500">
          ShopStack • Enterprise Multi-Vendor E-Commerce Platform
        </div>

      </div>

    </div>
  );
}

export default Dashboard;