

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
}