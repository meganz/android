import mega.privacy.android.gradle.google
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Plugin that applies the Firebase Android application convention.
 */
class AndroidApplicationFirebaseConvention : Plugin<Project> {
    /**
     * Apply the Firebase Android application convention.
     * @param target the project to apply the convention to.
     */
    override fun apply(target: Project) = target.run {
        with(pluginManager) {
            apply("com.google.gms.google-services")
            apply("com.google.firebase.crashlytics")
            apply("com.google.firebase.firebase-perf")
        }

        dependencies {
            val bom = google.findLibrary("firebase-bom").get()
            add("implementation", platform(bom))
            add("implementation", google.findLibrary("firebase.analytics").get())
            add("implementation", google.findBundle("firebase.bom").get())
        }
    }
}