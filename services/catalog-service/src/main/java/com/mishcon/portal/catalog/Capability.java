package com.mishcon.portal.catalog;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** A card in a catalogue. It is a definition of what can be done. The options are what the person picks. */
@Entity
@Table(name = "capabilities")
public class Capability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String risk;
    @Column(length = 1000)
    String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capability_options", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "option_name")
    @OrderColumn(name = "position")
    List<String> options = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "catalogue_id")
    Catalogue catalogue;

    protected Capability() {}

    Capability(Catalogue catalogue, String name, String risk, String description, List<String> options) {
        this.catalogue = catalogue;
        this.name = name;
        this.risk = risk;
        this.description = description;
        this.options = new ArrayList<>(options);
    }
}
