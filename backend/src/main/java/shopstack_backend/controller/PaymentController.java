package shopstack_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.OrderResponseDTO;
import shopstack_backend.dto.RazorpayOrderResponseDTO;
import shopstack_backend.dto.VerifyPaymentRequest;
import shopstack_backend.service.RazorpayPaymentService;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    @Autowired
    private RazorpayPaymentService razorpayPaymentService;

    // Step 1: create a Razorpay order for the customer's current cart total.
    // Frontend uses the returned razorpayOrderId + keyId to open the
    // Razorpay Checkout modal.
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(Authentication authentication) {
        try {
            RazorpayOrderResponseDTO response =
                    razorpayPaymentService.createRazorpayOrder(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to initiate payment: " + e.getMessage());
        }
    }

    // Step 2: after the Razorpay modal reports success, the frontend sends
    // the three values Razorpay gave it here. We verify the signature
    // ourselves before creating the real order — this is the only step
    // that actually matters for trust; nothing before this point should be
    // treated as "paid."
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(Authentication authentication,
                                            @RequestBody VerifyPaymentRequest request) {
        try {
            OrderResponseDTO order =
                    razorpayPaymentService.verifyAndCompleteOrder(authentication.getName(), request);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to verify payment: " + e.getMessage());
        }
    }
}