plugins {
    application          // gibt uns die Aufgabe "gradle run"
}

repositories {
    mavenCentral()       // woher Bibliotheken geladen werden
}

dependencies {
    // Excel-Ausgabe
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    // POI benutzt die Log4j-API. Ohne Implementierung meldet es beim Start
    // "could not find a logging provider" - harmlos, aber es verschmutzt die
    // Ausgabe. runtimeOnly: nur zur Laufzeit noetig, nicht beim Uebersetzen.
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.24.3")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Umlaute in Quelltext und Kommentaren zuverlaessig uebersetzen (Windows-Standard ist nicht UTF-8)
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("de.leo.controlling.App")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

tasks.named<JavaExec>("run") {
    // Das Programm endet absichtlich mit Exit-Code 2, wenn die Daten Fehlerzeilen
    // enthalten. Dieser Code ist ein Vertrag mit dem AUFRUFER - spaeter der
    // .bat-Datei oder einem CI-Job -, nicht mit Gradle.
    //
    // Gradle wertet jeden Code != 0 als fehlgeschlagenen Build und faerbt die
    // Ausgabe rot. Bei Daten, die planmaessig Befunde enthalten, waere also JEDER
    // Lauf rot - und man gewoehnt sich an, Fehlermeldungen zu ueberlesen.
    isIgnoreExitValue = true
}
