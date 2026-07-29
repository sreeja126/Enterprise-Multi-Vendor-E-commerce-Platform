import React from "react";

const AuthLayout = ({ title, subtitle, children }) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-100 via-slate-100 to-indigo-100 flex items-center justify-center px-4">
      <div className="grid w-full max-w-6xl overflow-hidden rounded-2xl bg-white shadow-2xl md:grid-cols-2">
        
        {/* Left Section */}
        <div className="hidden md:flex flex-col justify-center bg-blue-600 p-12 text-white">
          <h1 className="text-4xl font-bold">ShopStack</h1>

          <p className="mt-4 text-lg text-blue-100">
            Enterprise Multi-Vendor E-Commerce Platform
          </p>

          <div className="mt-10 space-y-4">
            <div className="flex items-center gap-3">
              <span className="text-2xl">🛍️</span>
              <p>Shop from multiple vendors in one place.</p>
            </div>

            <div className="flex items-center gap-3">
              <span className="text-2xl">🔒</span>
              <p>Secure authentication powered by JWT.</p>
            </div>

            <div className="flex items-center gap-3">
              <span className="text-2xl">⚡</span>
              <p>Fast, modern and responsive shopping experience.</p>
            </div>
          </div>
        </div>

        {/* Right Section */}
        <div className="flex items-center justify-center p-8 md:p-12">
          <div className="w-full max-w-md">
            <h2 className="text-3xl font-bold text-slate-800">
              {title}
            </h2>

            <p className="mt-2 mb-8 text-slate-500">
              {subtitle}
            </p>

            {children}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;