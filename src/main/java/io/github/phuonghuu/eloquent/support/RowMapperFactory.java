package io.github.phuonghuu.eloquent.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class RowMapperFactory {

    private static final DefaultConversionService CONVERSION_SERVICE = new DefaultConversionService();

    static {
        CONVERSION_SERVICE.addConverter(new TimestampToZonedDateTimeConverter());
    }

    private RowMapperFactory() {
    }

    public static <T> RowMapper<T> create(Class<T> type) {
        BeanPropertyRowMapper<T> rowMapper = new BeanPropertyRowMapper<>(type);
        rowMapper.setPrimitivesDefaultedForNullValue(true);
        rowMapper.setCheckFullyPopulated(false);
        rowMapper.setConversionService(CONVERSION_SERVICE);
        return rowMapper;
    }

    private static final class TimestampToZonedDateTimeConverter implements Converter<Timestamp, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(Timestamp source) {
            if (source == null) {
                return null;
            }
            return source.toInstant().atZone(ZoneId.systemDefault());
        }
    }
}

