package com.mishcon.portal.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeedDataTest {
    @Test
    @SuppressWarnings("unchecked")
    void seedingStoresThreeCataloguesWithCards() {
        List<Catalogue> saved = new ArrayList<>();
        CatalogueRepository repo = (CatalogueRepository) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{CatalogueRepository.class}, (p, m, a) -> {
                    if (m.getName().equals("count")) return 0L;
                    if (m.getName().equals("saveAll")) { saved.addAll((List<Catalogue>) a[0]); return saved; }
                    return null;
                });
        new SeedData(repo).run();
        assertEquals(3, saved.size());
        assertFalse(saved.get(0).capabilities.isEmpty());
    }
}
