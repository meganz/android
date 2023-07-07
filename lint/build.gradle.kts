plugins {
    id("kotlin")
    id("com.android.lint")
}

lint {
    htmlReport = true
    htmlOutput = file("lint-report.html")
    textReport = true
    absolutePaths = false
    ignoreTestSources = true
}

dependencies {
    compileOnly(lib.kotlin.stdlib.jdk7)
    // For a description of the below dependencies, see the main project README
    compileOnly(tools.lint.api)
    // You typically don't need this one:
    compileOnly(tools.lint.checks)

    testImplementation(testlib.junit)
    testImplementation(tools.lint)
    testImplementation(tools.lint.tests)
}

/**
 * Service to set jvmToolchain
 */
val service = project.extensions.getByType<JavaToolchainService>()

/**
 * Custom Launcher to set jvmToolchain
 */
val customLauncher = service.launcherFor {
    val jdk: String by rootProject.extra
    languageVersion.set(JavaLanguageVersion.of(jdk.toInt()))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
    kotlinJavaToolchain.toolchain.use(customLauncher)
}
