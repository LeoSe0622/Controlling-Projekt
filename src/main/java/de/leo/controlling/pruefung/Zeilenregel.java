package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;

import java.util.List;

/**
 * Eine Regel, die eine einzelne Zeile für sich allein beurteilen kann.
 *
 * <p>Rückgabe ist eine <b>Liste</b>, nicht ein einzelner Befund: Eine Zeile kann mehrere
 * Probleme derselben Art haben (eine Zeile kann bei istMenge <i>und</i> istPreis leer
 * sein). Eine leere Liste bedeutet "alles in Ordnung" — niemals {@code null}.
 *
 * <p>{@link #id()} und {@link #bezeichnung()} machen die Regel selbstbeschreibend. Damit
 * kann der Bericht eine Uebersicht "Befunde je Regel" bauen, in der auch die Regeln
 * auftauchen, die NICHTS gefunden haben — und nur so ist "keine Dubletten" von "nicht auf
 * Dubletten geprueft" zu unterscheiden. Stuende die Liste der Regelnamen stattdessen im
 * Writer, gaebe es sie zweimal, und beim Hinzufuegen der zehnten Regel faellt nur eine auf.
 */
public interface Zeilenregel {

    List<Befund> pruefe(Rohzeile zeile);

    /** Die Kennung, unter der die Befunde dieser Regel im Bericht erscheinen, z.B. "V02". */
    String id();

    /** Was sie prueft, in ein bis zwei Woertern - fuer die Uebersicht auf dem Deckblatt. */
    String bezeichnung();
}
