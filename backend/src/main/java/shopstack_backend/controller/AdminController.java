package shopstack_backend.controller;


import shopstack_backend.dto.*;
import shopstack_backend.entity.Order;
import shopstack_backend.repository.OrderRepository;
import shopstack_backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<AdminDashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<AdminVendorDTO>> getAllVendors() {
        return ResponseEntity.ok(adminService.getAllVendors());
    }

   @GetMapping("/orders")
public ResponseEntity<List<AdminOrderDTO>> getAllOrders() {
    return ResponseEntity.ok(adminService.getAllOrders());
}

    @GetMapping("/commissions")
    public ResponseEntity<List<CommissionDTO>> getCommissions() {
        return ResponseEntity.ok(adminService.getCommissionInfo());
    }

    @GetMapping("/system/status")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        return ResponseEntity.ok(adminService.getSystemStatus());
    }
}
