package com.voicestock.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InventoryUnitConverter implements AttributeConverter<InventoryUnit, String> {
    public String convertToDatabaseColumn(InventoryUnit unit) { return unit == null ? null : unit.getValue(); }
    public InventoryUnit convertToEntityAttribute(String value) { return value == null ? null : InventoryUnit.fromValue(value); }
}
