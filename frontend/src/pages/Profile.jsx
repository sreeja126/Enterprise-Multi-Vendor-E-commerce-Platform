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
      <div className="min-h-screen flex items-center justify-center">
        <h2 className="text-xl font-semibold">Loading Profile...</h2>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="w-full max-w-lg bg-white shadow-lg rounded-xl p-8">

        <h1 className="text-3xl font-bold text-center text-blue-600 mb-6">
          My Profile
        </h1>

        <div className="space-y-4">

          <div className="border rounded-lg p-4">
            <p className="text-gray-500 text-sm">Full Name</p>
            <p className="text-lg font-semibold">{user.fullName}</p>
          </div>

          <div className="border rounded-lg p-4">
            <p className="text-gray-500 text-sm">Email</p>
            <p className="text-lg font-semibold">{user.email}</p>
          </div>

          <div className="border rounded-lg p-4">
            <p className="text-gray-500 text-sm">Role</p>
            <p className="text-lg font-semibold">{user.role}</p>
          </div>

        </div>

        <div className="flex gap-3 mt-8">

          <button
            onClick={() => navigate("/dashboard")}
            className="flex-1 bg-gray-500 hover:bg-gray-600 text-white py-2 rounded-lg"
          >
            Dashboard
          </button>

          <button
            onClick={() => navigate("/editprofile")}
            className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg"
          >
            Edit Profile
          </button>

          <button
            onClick={handleLogout}
            className="flex-1 bg-red-500 hover:bg-red-600 text-white py-2 rounded-lg"
          >
            Logout
          </button>

        </div>
      </div>
    </div>
  );
}

export default Profile;