package com.proyecto.proyecto_techlab_springboot.sale.service;

import com.proyecto.proyecto_techlab_springboot.sale.util.SaleInternalNoteFormatter;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleInternalNoteFormatterTest {

    private static final Pattern NOTE_PATTERN =
            Pattern.compile("^\\[OPERADOR - Pago APROBADO \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}] nueva nota");

    @Test
    void prependsFormattedEntryAndPreservesExistingHistory() {
        String result = SaleInternalNoteFormatter.prepend(
                "nota anterior",
                "OPERADOR - Pago APROBADO",
                "nueva nota"
        );

        assertTrue(NOTE_PATTERN.matcher(result).find());
        assertTrue(result.endsWith("\nnota anterior"));
    }

    @Test
    void doesNotAddBlankLineWhenThereIsNoExistingHistory() {
        String result = SaleInternalNoteFormatter.prepend("  ", "SISTEMA - Posventa", "evento");

        assertTrue(result.matches("^\\[SISTEMA - Posventa \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}] evento$"));
        assertEquals(1, result.lines().count());
    }
}
