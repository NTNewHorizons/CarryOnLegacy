
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// Keep the distributed license identical to the source tree's LGPL license.
tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    from(layout.projectDirectory.file("LICENSE"))
}

tasks.named<Jar>("sourcesJar") {
    from(layout.projectDirectory.file("LICENSE"))
}
