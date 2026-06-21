package it.unibs.ingsw24_25.model.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;
import java.util.Optional;

@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return Optional.ofNullable(attribute)
                .map(YearMonth::toString)
                .orElse(null);
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return Optional.ofNullable(dbData)
                .map(YearMonth::parse)
                .orElse(null);
    }
}
