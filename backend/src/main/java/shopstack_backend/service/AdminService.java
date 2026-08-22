package shopstack_backend.service;

import shopstack_backend.dto.*;
import shopstack_backend.entity.Order;
import shopstack_backend.entity.Vendor;
import shopstack_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ProductRepository productRepository;

    public AdminDashboardSummaryDTO getDashboardSummary() {
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orderRepository.count();
        long totalVendors = vendorRepository.count();
        long totalProducts = productRepository.count();

        long lowStockCount = productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 5)
                .count();

        return new AdminDashboardSummaryDTO(
                totalSales,
                totalOrders,
                totalVendors,
                totalProducts,
                lowStockCount
        );
    }

    public List<AdminVendorDTO> getAllVendors() {
        List<Vendor> vendors = vendorRepository.findAll();
        List<AdminVendorDTO> dtos = new ArrayList<>();
        for (Vendor v : vendors) {
            long productCount = v.getProducts() == null ? 0 : v.getProducts().size();
            AdminVendorDTO dto = new AdminVendorDTO(
                    v.getId(),
                    v.getName(),
                    v.getEmail(),
                    v.getPhone(),
                    v.getDescription(),
                    v.getStatus(),
                    productCount
            );

            dtos.add(dto);
        }

        return dtos;
    }

    public List<AdminVendorDTO> getAllVendors1() {
    List<Vendor> vendors = vendorRepository.findAll();
    List<AdminVendorDTO> dtos = new ArrayList<>();

    for (Vendor v : vendors) {
        long productCount = v.getProducts() == null ? 0 : v.getProducts().size();

        dtos.add(new AdminVendorDTO(
                v.getId(),
                v.getName(),
                v.getEmail(),
                v.getPhone(),
                v.getDescription(),
                v.getStatus(),
                productCount
        ));
    }

    return dtos;
}

public List<AdminOrderDTO> getAllOrders() {
    List<Order> orders = orderRepository.findAll();
    List<AdminOrderDTO> result = new ArrayList<>();

    for (Order order : orders) {
        String customerName = order.getUser() != null
                ? order.getUser().getFullName()
                : "Unknown";

        String customerEmail = order.getUser() != null
                ? order.getUser().getEmail()
                : "Unknown";

        int itemCount = order.getItems() != null
                ? order.getItems().size()
                : 0;

        result.add(new AdminOrderDTO(
                order.getId(),
                customerName,
                customerEmail,
                order.getTotalAmount(),
                order.getStatus() != null
                        ? order.getStatus().name()
                        : "UNKNOWN",
                order.getCreatedAt(),
                itemCount
        ));
    }

    return result;
}
   public List<CommissionDTO> getCommissionInfo() {
    List<Vendor> vendors = vendorRepository.findAll();
    List<CommissionDTO> commissions = new ArrayList<>();
    double defaultCommissionRate = 10.0;
    for (Vendor vendor : vendors) {
        BigDecimal vendorSales = orderItemRepository.calculateVendorSales(vendor.getId());
        if (vendorSales == null) {
            vendorSales = BigDecimal.ZERO;
        }
        BigDecimal commission = vendorSales
                .multiply(BigDecimal.valueOf(defaultCommissionRate))
                .divide(BigDecimal.valueOf(100));
        commissions.add(new CommissionDTO(
                vendor.getId(),
                vendor.getName(),
                vendorSales,
                defaultCommissionRate,
                commission
        ));
    }
    return commissions;
}
    private String checkDatabaseStatus() {
        try (var connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return "CONNECTED";
            }
            return "DISCONNECTED";
        } catch (Exception e) {
            return "DISCONNECTED";
        }
    }
    public SystemStatusDTO getSystemStatus() {
        String databaseStatus = checkDatabaseStatus();
        String razorpayStatus = "CONFIGURED";
        return new SystemStatusDTO(
                "UP",
                databaseStatus,
                razorpayStatus,
                LocalDateTime.now().toString()
        );
    }
}