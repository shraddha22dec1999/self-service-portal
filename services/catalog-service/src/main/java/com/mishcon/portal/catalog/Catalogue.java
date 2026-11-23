package com.mishcon.portal.catalog;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "catalogues")
public class Catalogue {
    @Id
    String id;
    String name;
    String department;
    String businessUnit;
    String creator;
    String l1;
    String l2;
    @Column(length = 1000)
    String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "catalogue_visible_to", joinColumns = @JoinColumn(name = "catalogue_id"))
    @Column(name = "team")
    List<String> visibleTo = new ArrayList<>();

    @OneToMany(mappedBy = "catalogue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    List<Capability> capabilities = new ArrayList<>();

    @OneToMany(mappedBy = "catalogue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    List<ToolEntry> tools = new ArrayList<>();

    protected Catalogue() {}

    Catalogue(String id, String name, String department, String businessUnit, String creator, String l1, String l2,
              String description, List<String> visibleTo) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.businessUnit = businessUnit;
        this.creator = creator;
        this.l1 = l1;
        this.l2 = l2;
        this.description = description;
        this.visibleTo = new ArrayList<>(visibleTo);
    }
}
