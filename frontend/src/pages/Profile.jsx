import { useEffect, useState } from "react";
import { getProfile } from "../services/UserService";
import { useNavigate } from "react-router-dom";

function Profile() {
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getProfile();
        setUser(response.data);
      } catch (error) {
        console.error("Failed to fetch profile", error);
      }
    };

    fetchProfile();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50">
        <h2 className="text-lg font-medium text-slate-500">Loading profile…</h2>
      </div>
    );
  }

  const initials = user.fullName
    ? user.fullName.split(" ").map((n) => n[0]).slice(0, 2).join("").toUpperCase()
    : "?";

  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-lg bg-white shadow-md border border-stone-100 rounded-2xl p-8">

        <div className="flex flex-col items-center mb-8">
          <div className="h-20 w-20 rounded-full bg-emerald-700 text-white flex items-center justify-center text-2xl font-bold mb-4">
            {initials}
          </div>
          <h1 className="text-2xl font-serif font-bold text-slate-900">
            {user.fullName}
          </h1>
          <span className="inline-block bg-emerald-50 text-emerald-700 text-xs font-semibold uppercase tracking-wide px-3 py-1 rounded-full mt-2">
            {user.role}
          </span>
        </div>

        <div className="space-y-4">

          <div className="border border-stone-100 rounded-xl p-4 bg-stone-50">
            <p className="text-slate-400 text-xs uppercase tracking-wide">Full Name</p>
            <p className="text-lg font-semibold text-slate-900">{user.fullName}</p>
          </div>

          <div className="border border-stone-100 rounded-xl p-4 bg-stone-50">
            <p className="text-slate-400 text-xs uppercase tracking-wide">Email</p>
            <p className="text-lg font-semibold text-slate-900">{user.email}</p>
          </div>

          <div className="border border-stone-100 rounded-xl p-4 bg-stone-50">
            <p className="text-slate-400 text-xs uppercase tracking-wide">Role</p>
            <p className="text-lg font-semibold text-slate-900">{user.role}</p>
          </div>

        </div>

        <div className="flex gap-3 mt-8">

          <button
            onClick={() => navigate("/dashboard")}
            className="flex-1 border border-stone-300 text-slate-700 hover:bg-stone-100 py-3 rounded-lg font-medium transition"
          >
            Dashboard
          </button>

          <button
            onClick={() => navigate("/editprofile")}
            className="flex-1 bg-emerald-700 hover:bg-emerald-800 text-white py-3 rounded-lg font-semibold transition"
          >
            Edit Profile
          </button>

          <button
            onClick={handleLogout}
            className="flex-1 border border-rose-200 text-rose-600 hover:bg-rose-50 py-3 rounded-lg font-medium transition"
          >
            Logout
          </button>

        </div>
      </div>
    </div>
  );
}

export default Profile;
