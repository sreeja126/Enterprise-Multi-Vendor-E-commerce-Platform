import { useState } from "react";
import { Link } from "react-router-dom";

function ForgotPassword() {
  const [email, setEmail] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();

    // Backend API will be added later
    alert("Password reset link sent");
  };

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center px-4">

      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">

        <div className="text-center mb-8">

          <div className="text-5xl mb-4">🔒</div>

          <h1 className="text-3xl font-bold text-blue-600">
            Forgot Password?
          </h1>

          <p className="text-gray-500 mt-3">
            Enter your registered email address and we'll send you a password
            reset link.
          </p>

        </div>

        <form onSubmit={handleSubmit} className="space-y-6">

          <div>

            <label className="block text-sm font-medium mb-2">
              Email Address
            </label>

            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-600"
            />

          </div>

          <button
            type="submit"
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition"
          >
            Send Reset Link
          </button>

        </form>

        <div className="mt-6 text-center">

          <Link
            to="/login"
            className="text-blue-600 hover:underline font-medium"
          >
            ← Back to Login
          </Link>

        </div>

      </div>

    </div>
  );
}

export default ForgotPassword;