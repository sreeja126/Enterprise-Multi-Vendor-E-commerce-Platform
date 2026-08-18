package shopstack_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.CreateReturnRequestDTO;
import shopstack_backend.dto.ResolveReturnRequestDTO;
import shopstack_backend.dto.ReturnRequestResponseDTO;
import shopstack_backend.service.ReturnService;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@CrossOrigin(origins = "http://localhost:5173")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    // Customer requests a return on a delivered item.
    @PostMapping("/items/{orderItemId}")
    public ResponseEntity<?> requestReturn(@PathVariable Long orderItemId,
                                            Authentication authentication,
                                            @RequestBody CreateReturnRequestDTO request) {
        try {
            ReturnRequestResponseDTO created =
                    returnService.requestReturn(authentication.getName(), orderItemId, request.getReason());
            return ResponseEntity.ok(created);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReturnRequestResponseDTO>> getMyReturnRequests(Authentication authentication) {
        return ResponseEntity.ok(returnService.getMyReturnRequests(authentication.getName()));
    }

    // Vendor's incoming return requests — only for their own products.
    @GetMapping("/vendor")
    public ResponseEntity<List<ReturnRequestResponseDTO>> getVendorReturnRequests(Authentication authentication) {
        return ResponseEntity.ok(returnService.getVendorReturnRequests(authentication.getName()));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReturn(@PathVariable Long id,
                                            Authentication authentication,
                                            @RequestBody(required = false) ResolveReturnRequestDTO request) {
        try {
            String note = request != null ? request.getResolutionNote() : null;
            ReturnRequestResponseDTO updated = returnService.approveReturn(authentication.getName(), id, note);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReturn(@PathVariable Long id,
                                           Authentication authentication,
                                           @RequestBody(required = false) ResolveReturnRequestDTO request) {
        try {
            String note = request != null ? request.getResolutionNote() : null;
            ReturnRequestResponseDTO updated = returnService.rejectReturn(authentication.getName(), id, note);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}