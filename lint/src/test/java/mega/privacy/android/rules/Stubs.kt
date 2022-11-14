package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

@Suppress("UnstableApiUsage")
object Stubs {
    private val composeAnnotation: TestFile = kotlin(
        "androidx/compose/runtime/Composable.kt",
        """
            package androidx.compose.runtime
            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPE_PARAMETER,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.PROPERTY_GETTER,
            )
            annotation class Composable
        """.trimIndent()
    ).indented().within("src")

    private val previewAnnotation: TestFile = kotlin(
        "androidx/compose/ui/tooling/preview/Preview.kt",
        """
            package androidx.compose.ui.tooling.preview
            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.FUNCTION
            )
            @Repeatable
            annotation class Preview
        """.trimIndent()
    ).indented().within("src")

    private val isSystemInDarkTheme: TestFile = kotlin(
        "androidx/compose/foundation/Foudation.kt",
        """
            package androidx.compose.foundation
            fun isSystemInDarkTheme() = false
        """.trimIndent()
    ).indented().within("src")

    private val androidTheme: TestFile = kotlin(
        "mega/privacy/android/presentation/theme/AndroidTheme.kt",
        """
            package mega.privacy.android.presentation.theme
            import androidx.compose.runtime.Composable
            @Composable
            fun AndroidTheme(
                isDark: Boolean,
                content: @Composable () -> Unit,
            ) {
                content()
            }
        """.trimIndent()
    ).indented().within("src")

    val stubs = arrayOf(previewAnnotation, composeAnnotation, androidTheme, isSystemInDarkTheme)
}