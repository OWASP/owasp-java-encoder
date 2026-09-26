package org.owasp.encoder.testing.jakarta_test.dto;

/** Immutable values exposed as bean properties to JSP EL. */
public final class Item {
    private final int id;
    private final String name;
    private final String description;

    public Item(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
