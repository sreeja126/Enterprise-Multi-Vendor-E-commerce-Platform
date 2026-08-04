import { useNavigate } from "react-router-dom";

function CustomerDashboard() {

  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center py-12 px-4">

      <div className="w-full max-w-lg bg-white rounded-2xl shadow-md border border-stone-100 p-8">

        <div className="text-center">

          <span className="inline-block bg-blue-50 text-blue-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-4">
            ShopStack Customer
          </span>

          <h1 className="text-3xl font-serif font-bold text-slate-900">
            Customer Dashboard
          </h1>

          <p className="text-slate-500 mt-2">
            Browse products, manage your cart and track your orders.
          </p>

        </div>

        <div className="mt-8 rounded-xl bg-stone-50 border border-stone-100 p-5">

          <div className="flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-blue-500"></span>
            <span className="text-blue-700 font-medium text-sm">
              Customer Account Active
            </span>
          </div>

          <p className="text-slate-600 mt-2 text-sm">
            Discover products, save your favorites, manage your shopping cart,
            and view your previous orders.
          </p>

        </div>

        <div className="grid grid-cols-1 gap-3 mt-8">

          <button
           onClick={() => navigate("/shop")}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition"
          >
            Browse Products
          </button>

          <button
            onClick={() => navigate("/wishlist")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            Wishlist
          </button>

          <button
            onClick={() => navigate("/cart")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            My Cart
          </button>

          <button
            onClick={() => navigate("/orders")}
            className="w-full border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            Order History
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
            className="w-full border border-red-300 text-red-600 hover:bg-red-50 py-3 rounded-lg font-medium transition"
          >
            Logout
          </button>

        </div>

        <div className="mt-8 border-t border-stone-100 pt-4 text-center text-xs text-slate-400">
          ShopStack · Customer Portal
        </div>

      </div>

    </div>
  );
}

export default CustomerDashboard;