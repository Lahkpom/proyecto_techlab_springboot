package com.proyecto.proyecto_techlab_springboot.sale.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class SaleInternalNoteFormatter {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private SaleInternalNoteFormatter() {}

    public static String prepend(String existingNotes, String actor, String message) {
        String timestamp = ZonedDateTime.now(ZONE_ID).format(DATE_TIME_FORMATTER);
        String newEntry = "[%s %s] %s".formatted(actor, timestamp, message);
        return existingNotes == null || existingNotes.isBlank()
                ? newEntry
                : "%s\n%s".formatted(newEntry, existingNotes);
    }
}
