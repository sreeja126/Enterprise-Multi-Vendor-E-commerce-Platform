package shopstack_backend.controller;


import shopstack_backend.dto.*;
import shopstack_backend.entity.CommissionStatus;
import shopstack_backend.service.AdminService;
import shopstack_backend.service.CommissionService;
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
    private CommissionService commissionService;

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

    // Aggregated per-vendor summary (total sales / total commission earned),
    // built from stored Commission records rather than a live recalculation.
    @GetMapping("/commissions")
    public ResponseEntity<List<CommissionDTO>> getCommissions() {
        return ResponseEntity.ok(commissionService.getVendorCommissionSummary());
    }

    // Detailed, per-order commission records: Order ID, Vendor, Sale Amount,
    // Commission Rate, Commission Amount, Vendor Amount, Status, Date.
    @GetMapping("/commissions/details")
    public ResponseEntity<List<CommissionResponseDTO>> getCommissionDetails() {
        return ResponseEntity.ok(commissionService.getAllCommissionRecords());
    }

    // Update the status of a single commission record (e.g. mark it PAID
    // once the vendor payout has actually gone out).
    @PatchMapping("/commissions/{commissionId}/status")
    public ResponseEntity<CommissionResponseDTO> updateCommissionStatus(
            @PathVariable Long commissionId,
            @RequestBody UpdateCommissionStatusRequest request) {
        CommissionStatus newStatus = CommissionStatus.valueOf(request.getStatus().toUpperCase());
        return ResponseEntity.ok(commissionService.updateCommissionStatus(commissionId, newStatus));
    }

    @GetMapping("/system/status")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        return ResponseEntity.ok(adminService.getSystemStatus());
    }
}