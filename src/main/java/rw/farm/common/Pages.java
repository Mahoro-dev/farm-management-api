package rw.farm.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Pages {

    private Pages() {
    }

    public static Pageable of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }
}
