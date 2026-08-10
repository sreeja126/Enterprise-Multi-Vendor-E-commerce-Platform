import { useNavigate, Link } from "react-router-dom";

function Navbar() {
  const navigate = useNavigate();

  const token = localStorage.getItem("token");
  // Normalize role check (handles "vendor", "VENDOR", "Vendor", etc.)
  const rawRole = localStorage.getItem("role") || "";
  const isVendor = rawRole.toUpperCase() === "VENDOR";

  const handleBrandClick = () => {
    if (!token) {
      navigate("/login");
    } else if (isVendor) {
      navigate("/vendor-dashboard");
    } else {
      navigate("/customer-dashboard");
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-40 bg-white text-slate-900 border-b border-stone-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">

        {/* ShopStack Brand Button */}
        <button
          type="button"
          onClick={handleBrandClick}
          className="flex items-center gap-2 group text-left text-xl font-serif font-bold tracking-tight text-slate-900 focus:outline-none cursor-pointer"
        >
          <div className="h-8 w-8 rounded-lg bg-slate-900 text-white flex items-center justify-center font-sans text-sm font-bold transition group-hover:bg-slate-800">
            S
          </div>
          <span>ShopStack</span>
        </button>

        {token && (
          <nav className="hidden md:flex items-center gap-1 text-sm">
            <Link
              to="/products"
              className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
            >
              Products
            </Link>

            {isVendor ? (
              <>
                <Link
                  to="/myproducts"
                  className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
                >
                  My Products
                </Link>
                <Link
                  to="/addproduct"
                  className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
                >
                  Add Product
                </Link>
              </>
            ) : (
              <>
                <Link
                  to="/wishlist"
                  className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
                >
                  Wishlist
                </Link>
                <Link
                  to="/cart"
                  className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
                >
                  Cart
                </Link>
                <Link
                  to="/orders"
                  className="px-3 py-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
                >
                  Orders
                </Link>
              </>
            )}
          </nav>
        )}

        <div className="flex items-center gap-3">
          {token ? (
            <>
              <Link
                to="/profile"
                className="hidden sm:inline-flex px-3 py-2 rounded-lg text-sm text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
              >
                Profile
              </Link>
              <button
                onClick={handleLogout}
                className="px-4 py-2 rounded-lg text-sm font-semibold border border-stone-300 text-slate-700 hover:bg-stone-100 transition"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-stone-100 transition"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 rounded-lg text-sm font-semibold bg-slate-900 hover:bg-slate-800 text-white transition"
              >
                Sign Up
              </Link>
            </>
          )}
        </div>

      </div>
    </header>
  );
}

export default Navbar;