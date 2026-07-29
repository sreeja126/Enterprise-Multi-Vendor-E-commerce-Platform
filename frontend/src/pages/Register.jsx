import { Link } from "react-router-dom";
import { useState } from "react";
import AuthLayout from "../components/AuthLayout";
import InputField from "../components/InputField";
import Button from "../components/Button";

function Register() {
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    role: "CUSTOMER",
  });

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      alert("Passwords do not match!");
      return;
    }

    console.log(formData);

    // TODO:
    // Call Register API
    // Redirect to Login
  };

  return (
    <AuthLayout
      title="Create Account 🚀"
      subtitle="Join ShopStack as a Customer or Vendor."
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <InputField
          label="Full Name"
          name="fullName"
          value={formData.fullName}
          onChange={handleChange}
          placeholder="Enter your full name"
          required
        />

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
          placeholder="Create a password"
          required
        />

        <InputField
          label="Confirm Password"
          type="password"
          name="confirmPassword"
          value={formData.confirmPassword}
          onChange={handleChange}
          placeholder="Confirm your password"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-slate-700">
            Register As
          </label>

          <select
            name="role"
            value={formData.role}
            onChange={handleChange}
            className="
              w-full
              rounded-lg
              border
              border-slate-300
              px-4
              py-3
              focus:outline-none
              focus:ring-2
              focus:ring-blue-500
              focus:border-blue-500
            "
          >
            <option value="CUSTOMER">Customer</option>
            <option value="VENDOR">Vendor</option>
          </select>
        </div>

        <Button type="submit">
          Create Account
        </Button>

        <p className="text-center text-sm text-slate-600">
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-semibold text-blue-600 hover:text-blue-700"
          >
            Login
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}

export default Register;