package shopstack_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.QualityCheckRequestDTO;
import shopstack_backend.dto.ResolveReturnRequestDTO;
import shopstack_backend.dto.ReturnRequestResponseDTO;
import shopstack_backend.service.ReturnService;

import java.util.List;

/**
 * Admin-only return handling: review/approve/reject a customer's return
 * request, then perform (or record) the warehouse's physical QC once the
 * item is routed back. Kept separate from the customer/vendor-facing
 * ReturnController, and locked down the same way AdminController is —
 * via a class-level @PreAuthorize rather than relying on a URL prefix.
 */
@RestController
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReturnController {

    @Autowired
    private ReturnService returnService;

    @GetMapping
    public ResponseEntity<List<ReturnRequestResponseDTO>> getAllReturnRequests() {
        return ResponseEntity.ok(returnService.getAllReturnRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReturn(@PathVariable Long id,
                                            @RequestBody(required = false) ResolveReturnRequestDTO request) {
        try {
            String note = request != null ? request.getResolutionNote() : null;
            return ResponseEntity.ok(returnService.approveReturn(id, note));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReturn(@PathVariable Long id,
                                           @RequestBody(required = false) ResolveReturnRequestDTO request) {
        try {
            String note = request != null ? request.getResolutionNote() : null;
            return ResponseEntity.ok(returnService.rejectReturn(id, note));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Warehouse's physical inspection result: ACCEPTED restocks it as
    // sellable, DAMAGED quarantines it. Either way the customer is refunded.
    @PutMapping("/{id}/qc")
    public ResponseEntity<?> performQualityCheck(@PathVariable Long id,
                                                  @RequestBody QualityCheckRequestDTO request) {
        try {
            return ResponseEntity.ok(
                    returnService.performQualityCheck(id, request.getResult(), request.getNote()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}