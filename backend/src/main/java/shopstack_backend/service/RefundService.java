package shopstack_backend.service;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import shopstack_backend.entity.*;
import shopstack_backend.repository.PaymentRepository;
import shopstack_backend.repository.RefundRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RefundService {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RazorpayClient razorpayClient;

   
    public Refund processRefund(OrderItem item) {

        Payment payment = paymentRepository
                .findByOrderId(item.getOrder().getId())
                .orElse(null);

        Refund refund = new Refund();

        refund.setOrderItem(item);

        // Refund amount stays as BigDecimal
        BigDecimal refundAmount = item.getLineTotal() != null
                ? item.getLineTotal()
                : BigDecimal.ZERO;

        refund.setAmount(refundAmount);

     
        if (payment != null
                && "RAZORPAY".equalsIgnoreCase(payment.getMethod())) {

            refund.setMethod("RAZORPAY");

            try {

             
                long amountInPaise = refundAmount
                        .setScale(2)
                        .movePointRight(2)
                        .longValueExact();

                JSONObject options = new JSONObject();

                options.put("amount", amountInPaise);

                com.razorpay.Refund razorpayRefund =
                        razorpayClient.payments.refund(
                                payment.getTransactionId(),
                                options
                        );

                // Razorpay refund ID
                refund.setGatewayRefundId(
                        razorpayRefund.get("id")
                );

                refund.setStatus(
                        RefundStatus.PROCESSED
                );

                refund.setProcessedAt(
                        LocalDateTime.now()
                );

            } catch (Exception e) {

               e.printStackTrace();

    refund.setStatus(RefundStatus.FAILED);

    refund.setFailureReason(
            e.getMessage() != null
                    ? e.getMessage()
                    : e.getClass().getName()
    );
            }

        } else {

            refund.setMethod(
                    payment != null
                            ? payment.getMethod()
                            : "MANUAL"
            );

            refund.setStatus(
                    RefundStatus.PROCESSED
            );

            refund.setProcessedAt(
                    LocalDateTime.now()
            );
        }

        return refundRepository.save(refund);
    }
}