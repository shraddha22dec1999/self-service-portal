package com.mishcon.portal.catalog;

import jakarta.persistence.*;

/** A row of the Tools and resources tab: for example Source code, GitHub, Code. */
@Entity
@Table(name = "tool_entries")
public class ToolEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String tool;
    String category;
    String url;
    @Column(length = 500)
    String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "catalogue_id")
    Catalogue catalogue;

    protected ToolEntry() {}

    ToolEntry(Catalogue catalogue, String name, String tool, String category, String url, String description) {
        this.catalogue = catalogue;
        this.name = name;
        this.tool = tool;
        this.category = category;
        this.url = url;
        this.description = description;
    }
}
