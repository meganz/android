plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation(lib.json)
    implementation(lib.eclipse.jgit.ssh.jsch)
    implementation(lib.eclipse.jgit)

    testImplementation(testlib.system.stubs.jupiter)
    testImplementation(testlib.truth)
    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
