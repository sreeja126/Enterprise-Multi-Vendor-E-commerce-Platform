import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import AuthLayout from "../components/AuthLayout";
import InputField from "../components/InputField";
import Button from "../components/Button";
import { loginUser } from "../services/authService";

function Login() {

  const navigate = useNavigate();

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

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const response = await loginUser({
        email: formData.email,
        password: formData.password,
      });

      localStorage.setItem("token", response.data.token);
      localStorage.setItem("role", response.data.role);

      if (response.data.role === "VENDOR") {
        navigate("/vendor-dashboard");
      } else if (response.data.role === "CUSTOMER") {
        navigate("/customer-dashboard");
      } else {
        // Covers ADMINISTRATOR / WAREHOUSE_STAFF or any future role that
        // doesn't have a dedicated dashboard yet — send them somewhere
        // safe instead of silently doing nothing.
        navigate("/login");
      }

    } catch (error) {

      console.error(error);

      if (error.response) {
        alert(error.response.data.message || "Login Failed");
      } else {
        alert("Unable to connect to server.");
      }
    }
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

          <div className="text-right mb-4">
    <Link
    to="/forgot-password"
    className="text-blue-600 hover:underline text-sm" >
    Forgot Password?
  </Link>
</div>

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
