import React from 'react';
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role');

  // Not logged in
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // Normalize role
  const normalizedRole = userRole
    ? userRole.replace('ROLE_', '')
    : '';

  // Check allowed roles
  if (allowedRoles && allowedRoles.length > 0) {
    const normalizedAllowedRoles = allowedRoles.map(
      (role) => role.replace('ROLE_', '')
    );

    if (!normalizedAllowedRoles.includes(normalizedRole)) {
      return <Navigate to="/" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;