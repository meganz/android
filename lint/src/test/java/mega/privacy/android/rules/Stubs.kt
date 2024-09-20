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
            fun OriginalTempTheme(
                isDark: Boolean,
                content: @Composable () -> Unit,
            ) {
                content()
            }
        """.trimIndent()
    ).indented().within("src")

    private val snackbarComponent: TestFile = kotlin(
        "androidx/compose/material/Snackbar.kt",
        """
            package androidx.compose.material
            @Composable
            fun Snackbar(
                snackbarData: SnackbarData?,
            )
        """.trimIndent()
    ).indented().within("src")

    private val scaffoldComponent: TestFile = kotlin(
        "androidx/compose/material/Scaffold.kt",
        """
            package androidx.compose.material
            @Composable
            fun Scaffold(content: @Composable (PaddingValues) -> Unit)
        """.trimIndent()
    ).indented().within("src")

    private val snackbarHostStateShow: TestFile = kotlin(
        "androidx/compose/material/SnackbarHost.kt",
        """
            package androidx.compose.material

            enum class SnackbarDuration {
                Short,
                Long,
                Indefinite
            }
            class SnackbarHostState {
                fun showSnackbar(
                    message: String,
                    actionLabel: String? = null,
                    duration: SnackbarDuration = SnackbarDuration.Short
                )
            }
        """.trimIndent()
    ).indented().within("src")

    val stubs = arrayOf(
        previewAnnotation,
        composeAnnotation,
        androidTheme,
        isSystemInDarkTheme,
        snackbarComponent,
        snackbarHostStateShow,
        scaffoldComponent,
    )
}