package shopstack_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.*;
import shopstack_backend.entity.AllocationStatus;
import shopstack_backend.service.WarehouseService;

import java.util.List;

/**
 * Admin-only warehouse & fulfillment endpoints. Sits under /api/admin/**,
 * which SecurityConfig already restricts to hasRole('ADMINISTRATOR'),
 * so no per-method @PreAuthorize is needed here.
 */
@RestController
@RequestMapping("/api/admin/warehouses")
@CrossOrigin(origins = "http://localhost:5173")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    // ---- Warehouse management ----

    @PostMapping
    public ResponseEntity<?> createWarehouse(@RequestBody WarehouseRequestDTO request) {
        try {
            return ResponseEntity.ok(warehouseService.createWarehouse(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseRequestDTO request) {
        try {
            return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    // ---- Stock receiving ----

    @PostMapping("/{id}/stock")
    public ResponseEntity<?> receiveStock(@PathVariable Long id, @RequestBody WarehouseStockRequestDTO request) {
        try {
            return ResponseEntity.ok(warehouseService.receiveStock(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<List<WarehouseStockResponseDTO>> getWarehouseStock(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseStock(id));
    }

    // ---- Fulfillment queues: pick list, pack list, ready-to-ship list ----

    @GetMapping("/{id}/queue/{status}")
    public ResponseEntity<?> getQueue(@PathVariable Long id, @PathVariable String status) {
        try {
            AllocationStatus allocationStatus = AllocationStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(warehouseService.getWarehouseQueue(id, allocationStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    "\"" + status + "\" is not a valid stage. Use ALLOCATED, PICKED, PACKED, or READY_FOR_SHIPMENT.");
        }
    }

    @PostMapping("/orders/{orderId}/allocate")
public ResponseEntity<?> allocateExistingOrder(@PathVariable Long orderId) {
    try {
        warehouseService.allocateExistingOrder(orderId);
        return ResponseEntity.ok().build();
    } catch (IllegalArgumentException | IllegalStateException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

    @PatchMapping("/allocations/{allocationId}/pick")
    public ResponseEntity<?> pick(@PathVariable Long allocationId) {
        try {
            return ResponseEntity.ok(warehouseService.pick(allocationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/allocations/{allocationId}/pack")
    public ResponseEntity<?> pack(@PathVariable Long allocationId) {
        try {
            return ResponseEntity.ok(warehouseService.pack(allocationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/allocations/{allocationId}/ready")
    public ResponseEntity<?> markReady(@PathVariable Long allocationId) {
        try {
            return ResponseEntity.ok(warehouseService.markReadyForShipment(allocationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- Per-order fulfillment view + movement history ----

    @GetMapping("/orders/{orderId}/allocations")
    public ResponseEntity<List<StockAllocationResponseDTO>> getAllocationsForOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(warehouseService.getAllocationsForOrder(orderId));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<StockMovementResponseDTO>> getWarehouseMovements(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseMovements(id));
    }
}