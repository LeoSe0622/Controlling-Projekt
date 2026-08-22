package de.leo.controlling.pruefung;

    public record Befund(int Zeilennummer, String feld, String regeId, Schweregrad.Grad grad, String originalwert, String meldung) {
    }
    

