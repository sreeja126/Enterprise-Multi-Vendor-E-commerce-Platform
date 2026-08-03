import { useNavigate } from "react-router-dom";

function Dashboard() {

  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center py-12 px-4">

      <div className="w-full max-w-lg bg-white rounded-2xl shadow-md border border-stone-100 p-8">

        <div className="text-center">

          <span className="inline-block bg-emerald-50 text-emerald-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-4">
            ShopStack
          </span>

          <h1 className="text-3xl font-serif font-bold text-slate-900">
            Welcome back
          </h1>

          <p className="text-slate-500 mt-2">
            You're logged in and ready to shop.
          </p>

        </div>

        <div className="mt-8 rounded-xl bg-stone-50 border border-stone-100 p-5">

          <div className="flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
            <span className="text-emerald-700 font-medium text-sm">
              Account active
            </span>
          </div>

          <p className="text-slate-600 mt-2 text-sm">
            Manage your account, browse products, and track your orders below.
          </p>

        </div>

        <div className="grid grid-cols-1 gap-3 mt-8">

          <button
            onClick={() => navigate("/products")}
            className="w-full bg-emerald-700 hover:bg-emerald-800 text-white py-3 rounded-lg font-semibold transition"
          >
            Browse Products
          </button>

          <button
            onClick={() => navigate("/myproducts")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            My Products
          </button>

          <button
            onClick={() => navigate("/profile")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            View Profile
          </button>

          <button
            onClick={() => navigate("/editprofile")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            Edit Profile
          </button>

          <button
            onClick={handleLogout}
            className="w-full border border-rose-200 text-rose-600 hover:bg-rose-50 py-3 rounded-lg font-medium transition"
          >
            Logout
          </button>

        </div>

        <div className="mt-8 border-t border-stone-100 pt-4 text-center text-xs text-slate-400">
          ShopStack · Enterprise Multi-Vendor E-Commerce Platform
        </div>

      </div>

    </div>
  );
}

export default Dashboard;
