package com.example.moneytransfer.db;

import org.sql2o.converters.Converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static java.util.Optional.ofNullable;

public class LocalDateTimeConverter implements Converter<LocalDateTime> {
    @Override
    public LocalDateTime convert(Object val) {
        if (val instanceof Timestamp) return ((Timestamp) val).toLocalDateTime();
        else return null;
    }

    @Override
    public Object toDatabaseParam(LocalDateTime val) {
        return ofNullable(val).map(Timestamp::valueOf).orElse(null);
    }
}
