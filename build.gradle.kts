plugins {
    application          // gibt uns die Aufgabe "gradle run"
}

repositories {
    mavenCentral()       // woher Bibliotheken geladen werden
}

dependencies {
    // Tests. Apache POI (Excel) kommt erst in Phase 5 dazu.
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
