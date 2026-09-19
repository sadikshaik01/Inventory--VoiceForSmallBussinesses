package com.voicestock.entity;

public enum InventoryUnit {
    PACKETS("packets"), PIECES("pieces"), KG("kg"), GRAMS("grams"), BAGS("bags"), CARTONS("cartons"),
    BOXES("boxes"), DOZENS("dozens"), LITRES("litres"), MILLILITRES("millilitres"), QUINTALS("quintals");

    private final String value;
    InventoryUnit(String value) { this.value = value; }
    public String getValue() { return value; }

    public static InventoryUnit fromValue(String value) {
        for (InventoryUnit unit : values()) if (unit.value.equals(value)) return unit;
        throw new IllegalArgumentException("Unsupported inventory unit");
    }
}

