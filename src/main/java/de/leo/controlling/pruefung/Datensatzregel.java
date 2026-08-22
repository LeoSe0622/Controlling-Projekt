package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;

import java.util.List;

/**
 * Eine Regel, die alle Zeilen auf einmal braucht.
 *
 * <p>Der Unterschied zu {@link Zeilenregel} ist nicht technischer, sondern fachlicher Natur:
 * Manche Fehler existieren nicht IN einer Zeile, sondern ZWISCHEN Zeilen. Eine Dublette
 * kann man einer einzelnen Zeile nicht ansehen — erst der Vergleich mit den uebrigen 48
 * macht sie zur Dublette.
 *
 * <p>Wie bei {@link Zeilenregel}: leere Liste heisst "alles in Ordnung", niemals {@code null}.
 */
public interface Datensatzregel {

    List<Befund> pruefe(List<Rohzeile> alle);
}
