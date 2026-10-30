package rw.farm.domain;

public final class Enums {

    private Enums() {
    }

    public enum Sex {
        MALE, FEMALE, UNKNOWN
    }

    public enum AcquisitionType {
        BORN_ON_FARM, PURCHASED, TRANSFERRED_IN, OTHER
    }

    public enum AnimalStatus {
        ACTIVE, SOLD, DECEASED, LOST, CULLED, TRANSFERRED_OUT
    }

    public enum IdentifierType {
        VISUAL_EAR_TAG, RFID_EAR_TAG, INJECTABLE_RFID, TATTOO, OTHER
    }

    public enum TransferReason {
        BIRTH, PURCHASE, TRANSFER_IN, TRANSFER_OUT, SALE, OTHER
    }

    public enum VisitType {
        EXAMINATION, ILLNESS, INJURY, VACCINATION, FOLLOW_UP, OTHER
    }

    public enum TreatmentOutcome {
        IN_PROGRESS, RECOVERED, FOLLOW_UP_REQUIRED, REFERRED, OTHER
    }

    public enum AdministrationRoute {
        ORAL, INTRAMUSCULAR, SUBCUTANEOUS, INTRAVENOUS, TOPICAL, OTHER
    }

    public enum MedicineForm {
        LIQUID, TABLET, INJECTION, CAPSULE, BOLUS, POWDER, OINTMENT,
        SUSPENSION, POUR_ON, INTRAMAMMARY, SPRAY, OTHER
    }

    public enum MedicineUnit {
        ML, LITER, TABLET, CAPSULE, BOLUS, DOSE, VIAL, PIECE, GRAM, SACHET, TUBE
    }

    public enum SupplierKind {
        SUPPLIER, PHARMACY
    }

    public enum StockMovementType {
        PURCHASE, TREATMENT_USE, VACCINATION_USE, EXPIRED, RETURN, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT
    }

    public enum BreedingMethod {
        NATURAL, ARTIFICIAL_INSEMINATION, EMBRYO_TRANSFER, OTHER
    }

    public enum BreedingOutcome {
        PENDING, PREGNANT, NOT_PREGNANT, CANCELLED, UNKNOWN
    }

    public enum PregnancyStatus {
        SUSPECTED, CONFIRMED, LOST, CALVED, FALSE_PREGNANCY, UNKNOWN
    }

    public enum CalvingOutcome {
        LIVE, STILLBORN, ABORTION, DYSTOCIA, OTHER
    }

    public enum AnimalEventType {
        PURCHASE, BIRTH, TRANSFER, SALE, DEATH, LOSS, CALVING, OTHER
    }

    public enum RoleCode {
        ORG_ADMIN, FARM_MANAGER, VETERINARIAN, FARM_WORKER, INVENTORY_MANAGER, READ_ONLY
    }
}
