package com.example.moneytransfer.db

import spock.lang.Specification

import java.sql.Timestamp

import static java.time.Clock.systemUTC
import static java.time.LocalDateTime.now

class LocalDateTimeConverterSpec extends Specification {
    def underSpec = new LocalDateTimeConverter()

    def "convert - converts timestamp to local date time"() {
        given:
        def timestamp = new Timestamp(100, 1, 1, 10, 1, 2, 3)

        when:
        def result = underSpec.convert(timestamp)

        then:
        result == timestamp.toLocalDateTime()
    }

    def "convert - handles null"() {
        when:
        def result = underSpec.convert(null)

        then:
        result == null
    }

    def "toDatabaseParam - converts local date time to timestamp"() {
        given:
        def now = now(systemUTC())

        when:
        def result = underSpec.toDatabaseParam(now)

        then:
        result == Timestamp.valueOf(now)
    }

    def "toDatabaseParam - handles null"() {
        when:
        def result = underSpec.toDatabaseParam(null)

        then:
        result == null
    }
}
