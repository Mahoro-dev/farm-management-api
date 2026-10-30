package rw.farm.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.common.PageResponse;
import rw.farm.dto.OperationDtos.BatchResponse;
import rw.farm.dto.OperationDtos.CreateBatchRequest;
import rw.farm.dto.OperationDtos.MovementResponse;
import rw.farm.dto.OperationDtos.PostMovementRequest;
import rw.farm.dto.OperationDtos.ProductResponse;
import rw.farm.dto.OperationDtos.SaveProductRequest;
import rw.farm.dto.OperationDtos.SaveSupplierRequest;
import rw.farm.dto.OperationDtos.SupplierResponse;
import rw.farm.dto.OperationDtos.UpdateBatchRequest;
import rw.farm.service.MedicineService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Medicine")
public class MedicineController {

    private final MedicineService medicine;

    @GetMapping("/medicine-products")
    public PageResponse<ProductResponse> products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return medicine.products(page, size);
    }

    @PostMapping("/medicine-products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody SaveProductRequest request) {
        ProductResponse body = medicine.createProduct(request);
        return ResponseEntity.created(URI.create("/api/medicine-products/" + body.id())).body(body);
    }

    @PutMapping("/medicine-products/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody SaveProductRequest request) {
        return medicine.updateProduct(id, request);
    }

    @GetMapping("/suppliers")
    public PageResponse<SupplierResponse> suppliers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return medicine.suppliers(q, page, size);
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SaveSupplierRequest request) {
        SupplierResponse body = medicine.createSupplier(request);
        return ResponseEntity.created(URI.create("/api/suppliers/" + body.id())).body(body);
    }

    @PutMapping("/suppliers/{id}")
    public SupplierResponse updateSupplier(@PathVariable UUID id, @Valid @RequestBody SaveSupplierRequest request) {
        return medicine.updateSupplier(id, request);
    }

    @GetMapping("/farms/{farmId}/medicine-batches")
    public PageResponse<BatchResponse> batches(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return medicine.batches(farmId, page, size);
    }

    @Operation(description = """
            unitCost is the price of one base unit, in RWF. initialQuantity posts a purchase movement.
            Choose a supplier or pharmacy already saved for the organization. A pharmacy purchase may omit the batch
            number and can be a quantity of 1 or 2 doses or pieces.
            """)
    @PostMapping("/farms/{farmId}/medicine-batches")
    public ResponseEntity<BatchResponse> createBatch(
            @PathVariable UUID farmId, @Valid @RequestBody CreateBatchRequest request) {
        BatchResponse body = medicine.createBatch(farmId, request);
        return ResponseEntity.created(URI.create("/api/medicine-batches/" + body.id())).body(body);
    }

    @Operation(description = "Updates batch details. Stock quantity changes only through movements. unitCost is RWF.")
    @PutMapping("/medicine-batches/{id}")
    public BatchResponse updateBatch(@PathVariable UUID id, @Valid @RequestBody UpdateBatchRequest request) {
        return medicine.updateBatch(id, request);
    }

    @GetMapping("/medicine-batches/{id}/movements")
    public PageResponse<MovementResponse> movements(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return medicine.movements(id, page, size);
    }

    @Operation(description = """
            Stock movements are append-only. Send a positive quantity for purchases, returns, transfers in,
            expired stock and transfers out. For ADJUSTMENT, send a signed quantity. Amounts are quantities, and unit cost on the batch is RWF.
            """)
    @PostMapping("/medicine-batches/{id}/movements")
    public ResponseEntity<MovementResponse> postMovement(
            @PathVariable UUID id, @Valid @RequestBody PostMovementRequest request) {
        MovementResponse body = medicine.postMovement(id, request);
        return ResponseEntity.created(URI.create("/api/medicine-movements/" + body.id())).body(body);
    }
}
