import { useNavigate } from "react-router-dom";

function VendorDashboard() {

  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  const tiles = [
    {
      icon: "📦",
      title: "My Products",
      description: "View and manage your listings",
      onClick: () => navigate("/myproducts"),
    },
    {
      icon: "➕",
      title: "Add Product",
      description: "List a new item in your storefront",
      onClick: () => navigate("/addproduct"),
    },
    {
      icon: "📑",
      title: "Orders",
      description: "Coming soon",
      onClick: null,
    },
    {
      icon: "👤",
      title: "My Profile",
      description: "View and edit your account",
      onClick: () => navigate("/profile"),
    },
  ];

  return (
    <div className="min-h-screen bg-stone-50">

      {/* Header */}
      <div className="bg-slate-900 text-white py-10 shadow-lg">
        <div className="max-w-6xl mx-auto px-6 text-center">
          <span className="inline-block bg-emerald-500/20 text-emerald-300 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mb-3">
            Vendor Portal
          </span>
          <h1 className="text-4xl font-serif font-bold">
            Vendor Dashboard
          </h1>
          <p className="text-slate-300 mt-2">
            Manage your products and grow your business on ShopStack.
          </p>
        </div>
      </div>

      <div className="max-w-6xl mx-auto p-8">

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">

          {tiles.map((tile) => (
            <div
              key={tile.title}
              onClick={tile.onClick ?? undefined}
              className={`bg-white rounded-2xl shadow-sm border border-stone-100 p-8 transition ${
                tile.onClick
                  ? "cursor-pointer hover:shadow-xl hover:-translate-y-0.5"
                  : "opacity-60"
              }`}
            >
              <div className="text-4xl text-center mb-4">{tile.icon}</div>

              <h2 className="text-xl font-bold text-center text-slate-900">
                {tile.title}
              </h2>

              <p className="text-center text-slate-500 mt-2 text-sm">
                {tile.description}
              </p>
            </div>
          ))}

          {/* Logout */}
          <div
            onClick={handleLogout}
            className="cursor-pointer bg-rose-600 hover:bg-rose-700 text-white rounded-2xl shadow-sm p-8 transition hover:-translate-y-0.5"
          >
            <div className="text-4xl text-center mb-4">🚪</div>

            <h2 className="text-xl font-bold text-center">
              Logout
            </h2>

            <p className="text-center mt-2 text-sm text-rose-100">
              Sign out safely
            </p>
          </div>

        </div>

      </div>

    </div>
  );
}

export default VendorDashboard;
