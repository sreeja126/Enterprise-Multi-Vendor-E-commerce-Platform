import { Link } from "react-router-dom";
import { useState } from "react";
import AuthLayout from "../components/AuthLayout";
import InputField from "../components/InputField";
import Button from "../components/Button";

function Login() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    console.log(formData);

    // TODO:
    // Call login API
    // Store JWT
    // Redirect to dashboard
  };

  return (
    <AuthLayout
      title="Welcome Back 👋"
      subtitle="Login to continue shopping with ShopStack."
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <InputField
          label="Email"
          type="email"
          name="email"
          value={formData.email}
          onChange={handleChange}
          placeholder="Enter your email"
          required
        />

        <InputField
          label="Password"
          type="password"
          name="password"
          value={formData.password}
          onChange={handleChange}
          placeholder="Enter your password"
          required
        />

        <div className="flex items-center justify-between text-sm">
          <label className="flex items-center gap-2 text-slate-600">
            <input
              type="checkbox"
              className="rounded border-slate-300"
            />
            Remember me
          </label>

          <button
            type="button"
            className="font-medium text-blue-600 hover:text-blue-700"
          >
            Forgot Password?
          </button>
        </div>

        <Button type="submit">
          Login
        </Button>

        <p className="text-center text-sm text-slate-600">
          Don't have an account?{" "}
          <Link
            to="/register"
            className="font-semibold text-blue-600 hover:text-blue-700"
          >
            Register
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}

export default Login;