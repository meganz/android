package mega.privacy.android.rules.compose

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import java.util.EnumSet

internal val ModifierNames by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        "Modifier",
        "GlanceModifier"
    )
}

internal fun isKotlin(language: Language?): Boolean {
    return language == KotlinLanguage.INSTANCE
}

internal val KtAnnotated.isComposable: Boolean
    get() = annotationEntries.any { it.calleeExpression?.text == "Composable" }

internal val KtCallableDeclaration.isModifier: Boolean
    get() = ModifierNames.contains(typeReference?.text)

internal fun <T> T.runIf(value: Boolean, block: T.() -> T): T = if (value) block() else this

@Suppress("SpreadOperator")
internal inline fun <reified T> sourceImplementation(
    shouldRunOnTestSources: Boolean = true,
): Implementation where T : Detector, T : SourceCodeScanner {
    // We use the overloaded constructor that takes a varargs of `Scope` as the last param.
    // This is to enable on-the-fly IDE checks. We are telling lint to run on both
    // JAVA and TEST_SOURCES in the `scope` parameter but by providing the `analysisScopes`
    // params, we're indicating that this check can run on either JAVA or TEST_SOURCES and
    // doesn't require both of them together.
    // From discussion on lint-dev https://groups.google.com/d/msg/lint-dev/ULQMzW1ZlP0/1dG4Vj3-AQAJ
    // This was supposed to be fixed in AS 3.4 but still required as recently as 3.6-alpha10.
    return if (shouldRunOnTestSources) {
        Implementation(
            T::class.java,
            EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
            EnumSet.of(Scope.JAVA_FILE),
            EnumSet.of(Scope.TEST_SOURCES)
        )
    } else {
        Implementation(T::class.java, EnumSet.of(Scope.JAVA_FILE))
    }
}
