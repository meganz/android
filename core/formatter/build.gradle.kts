plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.core.formatter"

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
}
