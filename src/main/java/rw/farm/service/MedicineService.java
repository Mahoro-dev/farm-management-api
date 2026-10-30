package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Enums.StockMovementType;
import rw.farm.domain.Enums.SupplierKind;
import rw.farm.domain.Farm;
import rw.farm.domain.MedicineBatch;
import rw.farm.domain.MedicineProduct;
import rw.farm.domain.MedicineStockMovement;
import rw.farm.domain.Supplier;
import rw.farm.dto.OperationDtos.BatchResponse;
import rw.farm.dto.OperationDtos.CreateBatchRequest;
import rw.farm.dto.OperationDtos.MovementResponse;
import rw.farm.dto.OperationDtos.PostMovementRequest;
import rw.farm.dto.OperationDtos.ProductResponse;
import rw.farm.dto.OperationDtos.SaveProductRequest;
import rw.farm.dto.OperationDtos.SaveSupplierRequest;
import rw.farm.dto.OperationDtos.SupplierResponse;
import rw.farm.dto.OperationDtos.UpdateBatchRequest;
import rw.farm.repo.MedicineBatchRepository;
import rw.farm.repo.MedicineProductRepository;
import rw.farm.repo.MedicineStockMovementRepository;
import rw.farm.repo.SupplierRepository;
import rw.farm.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static rw.farm.service.AccessService.INVENTORY_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineService {

    private final MedicineProductRepository products;
    private final MedicineBatchRepository batches;
    private final MedicineStockMovementRepository movements;
    private final SupplierRepository suppliers;
    private final AccessService access;
    private final DomainSupport support;

    public PageResponse<ProductResponse> products(int page, int size) {
        access.requireMember();
        return PageResponse.of(products
                .findByOrganizationIdOrderByNameAsc(SecurityUtils.organizationId(), Pages.of(page, size))
                .map(this::toResponse));
    }

    @Transactional
    public ProductResponse createProduct(SaveProductRequest request) {
        access.requireAnyRole(INVENTORY_WRITE);
        MedicineProduct product = new MedicineProduct();
        product.setOrganization(access.currentOrganization());
        product.setCreatedBy(support.actor());
        product.setUpdatedBy(support.actor());
        apply(product, request, true);
        return toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, SaveProductRequest request) {
        access.requireAnyRole(INVENTORY_WRITE);
        MedicineProduct product = loadProduct(id);
        product.setUpdatedBy(support.actor());
        apply(product, request, false);
        return toResponse(product);
    }

    public PageResponse<BatchResponse> batches(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(batches.findByFarmIdOrderByExpiryDateAsc(farmId, Pages.of(page, size)).map(this::toResponse));
    }

    @Transactional
    public BatchResponse createBatch(UUID farmId, CreateBatchRequest request) {
        Farm farm = access.requireFarmWrite(farmId, INVENTORY_WRITE);
        MedicineProduct product = loadProduct(request.medicineProductId());
        if (!product.isActive()) {
            throw ApiException.unprocessable("Medicine product is not active");
        }
        LocalReceived received = dates(request.receivedOn(), request.expiryDate());
        Supplier supplier = loadSupplier(request.supplierId());
        if (!supplier.isActive()) {
            throw ApiException.unprocessable("This supplier is not active");
        }
        String requestedBatch = request.batchNumber() == null ? "" : request.batchNumber().trim();
        if (supplier.getKind() == SupplierKind.PHARMACY && requestedBatch.isEmpty()
                && (request.initialQuantity() == null || request.initialQuantity().signum() <= 0)) {
            throw ApiException.unprocessable("Enter how many doses or pieces you bought");
        }
        MedicineBatch batch = new MedicineBatch();
        batch.setFarm(farm);
        batch.setMedicineProduct(product);
        batch.setBatchNumber(batchNumber(farmId, product.getId(), request.batchNumber(), supplier.getKind()));
        batch.setExpiryDate(request.expiryDate());
        batch.setReceivedOn(received.receivedOn());
        batch.setSupplier(supplier);
        batch.setSupplierName(supplier.getName());
        batch.setUnitCost(request.unitCost());
        batch.setQuantityOnHand(BigDecimal.ZERO);
        batch.setCreatedBy(support.actor());
        batch.setUpdatedBy(support.actor());
        batches.saveAndFlush(batch);
        if (request.initialQuantity() != null && request.initialQuantity().signum() > 0) {
            postMovement(batch, StockMovementType.PURCHASE, request.initialQuantity(), "Initial receipt", null);
        }
        support.refresh(batch);
        return toResponse(batch);
    }

    @Transactional
    public BatchResponse updateBatch(UUID id, UpdateBatchRequest request) {
        MedicineBatch batch = loadBatch(id);
        access.requireFarmWrite(batch.getFarm().getId(), INVENTORY_WRITE);
        if (request.expiryDate().isBefore(request.receivedOn())) {
            throw ApiException.unprocessable("Expiry date cannot be before the received date");
        }
        batch.setExpiryDate(request.expiryDate());
        batch.setReceivedOn(request.receivedOn());
        if (request.supplierId() != null) {
            Supplier supplier = loadSupplier(request.supplierId());
            if (!supplier.isActive() && (batch.getSupplier() == null || !batch.getSupplier().getId().equals(supplier.getId()))) {
                throw ApiException.unprocessable("This supplier is not active");
            }
            batch.setSupplier(supplier);
            batch.setSupplierName(supplier.getName());
        }
        batch.setUnitCost(request.unitCost());
        batch.setUpdatedBy(support.actor());
        return toResponse(batch);
    }

    public PageResponse<MovementResponse> movements(UUID batchId, int page, int size) {
        MedicineBatch batch = loadBatch(batchId);
        access.requireFarmRead(batch.getFarm().getId());
        return PageResponse.of(movements.findByBatchIdOrderByMovementAtDesc(batchId, Pages.of(page, size))
                .map(movement -> toResponse(movement, batch.getQuantityOnHand())));
    }

    @Transactional
    public MovementResponse postMovement(UUID batchId, PostMovementRequest request) {
        MedicineBatch batch = loadBatch(batchId);
        access.requireFarmWrite(batch.getFarm().getId(), INVENTORY_WRITE);
        if (request.movementType() == StockMovementType.TREATMENT_USE || request.movementType() == StockMovementType.VACCINATION_USE) {
            throw ApiException.badRequest("Treatment and vaccination use are recorded when medicine is given to an animal");
        }
        MedicineStockMovement movement = postMovement(
                batch,
                request.movementType(),
                request.quantity(),
                request.referenceText(),
                request.notes());
        return toResponse(movement, batch.getQuantityOnHand());
    }

    private MedicineStockMovement postMovement(
            MedicineBatch batch,
            StockMovementType type,
            BigDecimal quantity,
            String referenceText,
            String notes) {
        BigDecimal signed = signedQuantity(type, quantity);
        if (signed.signum() < 0 && batch.getQuantityOnHand().compareTo(signed.abs()) < 0) {
            throw ApiException.unprocessable("Insufficient medicine stock");
        }
        MedicineStockMovement movement = new MedicineStockMovement();
        movement.setBatch(batch);
        movement.setMovementType(type);
        movement.setQuantityChange(signed);
        movement.setReferenceText(DomainSupport.trimToNull(referenceText));
        movement.setNotes(DomainSupport.trimToNull(notes));
        movement.setRecordedBy(support.actor());
        movements.saveAndFlush(movement);
        support.refresh(batch);
        return movement;
    }

    private BigDecimal signedQuantity(StockMovementType type, BigDecimal quantity) {
        if (quantity == null || quantity.signum() == 0) {
            throw ApiException.badRequest("Quantity must not be zero");
        }
        return switch (type) {
            case PURCHASE, RETURN, TRANSFER_IN -> positive(quantity);
            case EXPIRED, TRANSFER_OUT -> positive(quantity).negate();
            case ADJUSTMENT -> quantity;
            case TREATMENT_USE, VACCINATION_USE -> throw ApiException.badRequest("Treatment and vaccination use are recorded when medicine is given to an animal");
        };
    }

    private BigDecimal positive(BigDecimal quantity) {
        if (quantity.signum() < 0) {
            throw ApiException.badRequest("Quantity must be positive for this movement type");
        }
        return quantity;
    }

    private void apply(MedicineProduct product, SaveProductRequest request, boolean creating) {
        product.setName(request.name().trim());
        product.setActiveIngredient(DomainSupport.trimToNull(request.activeIngredient()));
        product.setForm(request.form());
        product.setFormulation(DomainSupport.trimToNull(request.formulation()));
        product.setBaseUnit(request.baseUnit());
        product.setDescription(DomainSupport.trimToNull(request.description()));
        product.setRequiresPrescription(request.requiresPrescription());
        if (request.active() != null) {
            product.setActive(request.active());
        } else if (creating) {
            product.setActive(true);
        }
    }

    private LocalReceived dates(java.time.LocalDate receivedOn, java.time.LocalDate expiry) {
        java.time.LocalDate received = receivedOn == null ? java.time.LocalDate.now() : receivedOn;
        if (expiry.isBefore(received)) {
            throw ApiException.unprocessable("Expiry date cannot be before the received date");
        }
        return new LocalReceived(received);
    }

    private MedicineProduct loadProduct(UUID id) {
        MedicineProduct product = products.findById(id)
                .orElseThrow(() -> ApiException.notFound("Medicine product not found"));
        if (!product.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Medicine product not found");
        }
        return product;
    }

    private MedicineBatch loadBatch(UUID id) {
        MedicineBatch batch = batches.findById(id).orElseThrow(() -> ApiException.notFound("Medicine batch not found"));
        if (!batch.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Medicine batch not found");
        }
        return batch;
    }

    private ProductResponse toResponse(MedicineProduct product) {
        return new ProductResponse(
                product.getId(), product.getOrganization().getId(), product.getName(), product.getActiveIngredient(),
                product.getForm(), product.getFormulation(), product.getBaseUnit(), product.getDescription(),
                product.isRequiresPrescription(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
    }

    public PageResponse<SupplierResponse> suppliers(String q, int page, int size) {
        access.requireMember();
        UUID organizationId = SecurityUtils.organizationId();
        var result = (q == null || q.isBlank())
                ? suppliers.findByOrganizationIdOrderByNameAsc(organizationId, Pages.of(page, size))
                : suppliers.findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(organizationId, q.trim(), Pages.of(page, size));
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional
    public SupplierResponse createSupplier(SaveSupplierRequest request) {
        access.requireAnyRole(INVENTORY_WRITE);
        Supplier supplier = new Supplier();
        supplier.setOrganization(access.currentOrganization());
        apply(supplier, request);
        return toResponse(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplier(UUID id, SaveSupplierRequest request) {
        access.requireAnyRole(INVENTORY_WRITE);
        Supplier supplier = loadSupplier(id);
        apply(supplier, request);
        return toResponse(supplier);
    }

    private void apply(Supplier supplier, SaveSupplierRequest request) {
        String name = request.name().trim();
        suppliers.findByOrganizationIdAndNameIgnoreCase(supplier.getOrganization().getId(), name)
                .filter(existing -> supplier.getId() == null || !existing.getId().equals(supplier.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("A supplier with this name already exists");
                });
        supplier.setName(name);
        supplier.setKind(request.kind());
        supplier.setPhone(DomainSupport.trimToNull(request.phone()));
        supplier.setNotes(DomainSupport.trimToNull(request.notes()));
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
    }

    private String batchNumber(UUID farmId, UUID productId, String requested, SupplierKind kind) {
        String number = requested == null ? "" : requested.trim();
        if (!number.isEmpty()) {
            return number;
        }
        if (kind != SupplierKind.PHARMACY) {
            throw ApiException.unprocessable("Enter the batch number from the supplier. A pharmacy purchase can leave it blank");
        }
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "PHARM-" + day + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            if (!batches.existsByFarmIdAndMedicineProductIdAndBatchNumber(farmId, productId, code)) {
                return code;
            }
        }
        return "PHARM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Supplier loadSupplier(UUID id) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> ApiException.notFound("Supplier not found"));
        if (!supplier.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Supplier not found");
        }
        return supplier;
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(), supplier.getName(), supplier.getKind(), supplier.getPhone(), supplier.getNotes(), supplier.isActive());
    }

    private BatchResponse toResponse(MedicineBatch batch) {
        return new BatchResponse(
                batch.getId(),
                batch.getFarm().getId(),
                batch.getMedicineProduct().getId(),
                batch.getMedicineProduct().getName(),
                batch.getMedicineProduct().getForm(),
                batch.getMedicineProduct().getBaseUnit(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                batch.getReceivedOn(),
                batch.getSupplier() == null ? null : batch.getSupplier().getId(),
                batch.getSupplierName(),
                batch.getUnitCost(),
                batch.getQuantityOnHand(),
                batch.getCreatedAt(),
                batch.getUpdatedAt());
    }

    private MovementResponse toResponse(MedicineStockMovement movement, BigDecimal quantityOnHand) {
        return new MovementResponse(
                movement.getId(),
                movement.getBatch().getId(),
                movement.getMovementType(),
                movement.getQuantityChange(),
                movement.getMovementAt(),
                movement.getTreatmentMedication() == null ? null : movement.getTreatmentMedication().getId(),
                movement.getReferenceText(),
                movement.getNotes(),
                quantityOnHand,
                movement.getCreatedAt());
    }

    private record LocalReceived(java.time.LocalDate receivedOn) {
    }
}
