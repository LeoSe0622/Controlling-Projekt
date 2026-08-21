package de.leo.controlling.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZahlenTest {

    @Test
    void parstNormaleZahl() {
        // Sollwert in DERSELBEN Schreibweise wie der Istwert: BigDecimal.equals()
        // vergleicht auch die Nachkommastellen. new BigDecimal("2233") waere hier
        // NICHT gleich zu new BigDecimal("2233.0").
        assertEquals(new BigDecimal("2233.0"), Zahlen.parse("2233.0"));
        assertEquals(new BigDecimal("26.54"), Zahlen.parse("26.54"));
        assertEquals(new BigDecimal("2000"), Zahlen.parse("2000"));
    }

    @Test
    void parstNegativeZahl() {
        // Negativ ist eine gueltige ZAHL. Ob sie fachlich erlaubt ist, entscheidet
        // V05 - nicht der Parser. Trennung von Syntax und Fachlichkeit.
        assertEquals(new BigDecimal("-77.05"), Zahlen.parse("-77.05"));
    }

    @Test
    void liefertNull_beiLeer() {
        assertNull(Zahlen.parse(""), "leerer String ist keine Zahl");
        assertNull(Zahlen.parse("   "), "nur Leerzeichen ist keine Zahl");
        assertNull(Zahlen.parse(null), "null ist keine Zahl - und darf nicht knallen");
    }

    @Test
    void liefertNull_beiText() {
        assertNull(Zahlen.parse("abc"));
        assertNull(Zahlen.parse("zweitausend"));
        assertNull(Zahlen.parse("26,54"), "deutsches Komma kann BigDecimal nicht");
    }

    @Test
    void ignoriertLeerzeichenRundherum() {
        assertEquals(new BigDecimal("2000"), Zahlen.parse(" 2000 "));
    }
}
