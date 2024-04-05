plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.6.0.202305301015-r")
    implementation("org.json:json:20230227")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.1.6")
    testImplementation("com.google.truth:truth:1.1.3")
}

tasks.withType<Test> {
    // If using JUnit 5
    useJUnitPlatform()
}