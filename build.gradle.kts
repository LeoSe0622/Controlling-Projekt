plugins {
    application          // gibt uns die Aufgabe "gradle run"
}

repositories {
    mavenCentral()       // woher Bibliotheken geladen werden
}

dependencies {
    // Excel-Ausgabe
    implementation("org.apache.poi:poi-ooxml:5.4.1")

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
