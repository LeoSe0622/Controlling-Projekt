package de.leo.controlling.abweichung;

/**
 * Wie dringend muss jemand auf diese Abweichung schauen.
 *
 * <p>Die Ampel bewertet <b>Geschaeftsabweichungen</b>, nicht Datenqualitaet. Der Ausreisser
 * in den Daten ist bereits als V08-Warnung ausgewiesen; ihn hier ein zweites Mal rot zu
 * faerben, wuerde dieselbe Information doppelt transportieren.
 */
public enum Ampel {

    /** Keine Schwelle ueberschritten — laeuft im Rahmen. */
    GRUEN,

    /** Mindestens eine Schwelle ueberschritten — anschauen. */
    GELB,

    /** Beide Schwellen ueberschritten und negativ — Handlungsbedarf. */
    ROT
}
