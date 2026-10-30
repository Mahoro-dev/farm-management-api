package rw.farm.breeding;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AncestryTest {

    @Test
    void rejectsMatingWithOffspringWhenDamHasNoRecordedParents() {
        UUID cow = UUID.randomUUID();
        UUID calf = UUID.randomUUID();
        Map<UUID, Ancestry.Parents> links = new HashMap<>();
        links.put(cow, new Ancestry.Parents(null, null));
        links.put(calf, new Ancestry.Parents(cow, null));

        assertTrue(Ancestry.sharesAncestor(cow, calf, links, 5));
    }

    @Test
    void rejectsMatingWithSireWhenSireHasNoRecordedParents() {
        UUID cow = UUID.randomUUID();
        UUID sire = UUID.randomUUID();
        Map<UUID, Ancestry.Parents> links = new HashMap<>();
        links.put(cow, new Ancestry.Parents(null, sire));
        links.put(sire, new Ancestry.Parents(null, null));

        assertTrue(Ancestry.sharesAncestor(cow, sire, links, 5));
    }

    @Test
    void rejectsSharedGrandparentWithinDepth() {
        UUID grandDam = UUID.randomUUID();
        UUID cow = UUID.randomUUID();
        UUID sire = UUID.randomUUID();
        UUID cowDam = UUID.randomUUID();
        UUID sireDam = UUID.randomUUID();
        Map<UUID, Ancestry.Parents> links = new HashMap<>();
        links.put(grandDam, new Ancestry.Parents(null, null));
        links.put(cowDam, new Ancestry.Parents(grandDam, null));
        links.put(sireDam, new Ancestry.Parents(grandDam, null));
        links.put(cow, new Ancestry.Parents(cowDam, null));
        links.put(sire, new Ancestry.Parents(sireDam, null));

        assertFalse(Ancestry.sharesAncestor(cow, sire, links, 1));
        assertTrue(Ancestry.sharesAncestor(cow, sire, links, 2));
    }

    @Test
    void allowsUnrelatedAnimals() {
        UUID cow = UUID.randomUUID();
        UUID sire = UUID.randomUUID();
        Map<UUID, Ancestry.Parents> links = new HashMap<>();
        links.put(cow, new Ancestry.Parents(UUID.randomUUID(), UUID.randomUUID()));
        links.put(sire, new Ancestry.Parents(UUID.randomUUID(), UUID.randomUUID()));

        assertFalse(Ancestry.sharesAncestor(cow, sire, links, 5));
    }
}
