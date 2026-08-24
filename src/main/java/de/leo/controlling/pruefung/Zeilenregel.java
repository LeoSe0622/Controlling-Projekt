package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;

import java.util.List;

/**
 * Eine Regel, die eine einzelne Zeile für sich allein beurteilen kann.
 *
 * <p>Rückgabe ist eine <b>Liste</b>, nicht ein einzelner Befund: Eine Zeile kann mehrere
 * Probleme derselben Art haben (eine Zeile kann bei istMenge <i>und</i> istPreis leer
 * sein). Eine leere Liste bedeutet "alles in Ordnung" — niemals {@code null}.
 */
public interface Zeilenregel {

    List<Befund> pruefe(Rohzeile zeile);
}
