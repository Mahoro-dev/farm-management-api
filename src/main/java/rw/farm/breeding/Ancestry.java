package rw.farm.breeding;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared-ancestor check used before a breeding record is saved.
 * The starting animal is included, so a direct parent/offspring mating is rejected
 * even when the older animal has no recorded parents.
 */
public final class Ancestry {

    private Ancestry() {
    }

    public record Parents(UUID damId, UUID sireId) {
    }

    public static boolean sharesAncestor(UUID cowId, UUID sireId, Map<UUID, Parents> links, int maxGenerations) {
        Set<UUID> cowLine = lineage(cowId, links, maxGenerations);
        Set<UUID> sireLine = lineage(sireId, links, maxGenerations);
        for (UUID id : cowLine) {
            if (sireLine.contains(id)) {
                return true;
            }
        }
        return false;
    }

    public static Set<UUID> lineage(UUID start, Map<UUID, Parents> links, int maxGenerations) {
        Set<UUID> found = new LinkedHashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(start, 0));
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (!found.add(node.id())) {
                continue;
            }
            if (node.depth() >= maxGenerations) {
                continue;
            }
            Parents parents = links.get(node.id());
            if (parents == null) {
                continue;
            }
            if (parents.damId() != null) {
                queue.add(new Node(parents.damId(), node.depth() + 1));
            }
            if (parents.sireId() != null) {
                queue.add(new Node(parents.sireId(), node.depth() + 1));
            }
        }
        return found;
    }

    public static Map<UUID, Parents> linksFrom(Iterable<Object[]> rows) {
        Map<UUID, Parents> links = new LinkedHashMap<>();
        for (Object[] row : rows) {
            links.put((UUID) row[0], new Parents((UUID) row[1], (UUID) row[2]));
        }
        return links;
    }

    private record Node(UUID id, int depth) {
    }
}
