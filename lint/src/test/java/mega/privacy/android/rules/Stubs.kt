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

    private val modifier: TestFile = kotlin(
        "androidx/compose/ui/Modifier.kt",
        """
            package androidx.compose.ui
            interface Modifier {
                fun then(other: Modifier): Modifier = other
                companion object : Modifier
            }
        """.trimIndent()
    ).indented().within("src")

    private val clickableModifiers: TestFile = kotlin(
        "androidx/compose/foundation/Clickable.kt",
        """
            package androidx.compose.foundation
            import androidx.compose.ui.Modifier
            fun Modifier.clickable(
                enabled: Boolean = true,
                onClick: () -> Unit,
            ): Modifier = this
            fun Modifier.combinedClickable(
                enabled: Boolean = true,
                onLongClick: (() -> Unit)? = null,
                onClick: () -> Unit,
            ): Modifier = this
        """.trimIndent()
    ).indented().within("src")

    private val selectionModifiers: TestFile = kotlin(
        "androidx/compose/foundation/selection/Selection.kt",
        """
            package androidx.compose.foundation.selection
            import androidx.compose.ui.Modifier
            fun Modifier.toggleable(
                value: Boolean,
                enabled: Boolean = true,
                onValueChange: (Boolean) -> Unit,
            ): Modifier = this
            fun Modifier.triStateToggleable(
                state: Boolean,
                enabled: Boolean = true,
                onClick: () -> Unit,
            ): Modifier = this
            fun Modifier.selectable(
                selected: Boolean,
                enabled: Boolean = true,
                onClick: () -> Unit,
            ): Modifier = this
        """.trimIndent()
    ).indented().within("src")

    private val semanticsModifier: TestFile = kotlin(
        "androidx/compose/ui/semantics/Semantics.kt",
        """
            package androidx.compose.ui.semantics
            import androidx.compose.ui.Modifier
            fun Modifier.semantics(
                mergeDescendants: Boolean = false,
                properties: () -> Unit = {},
            ): Modifier = this
        """.trimIndent()
    ).indented().within("src")

    private val conditionalModifier: TestFile = kotlin(
        "mega/privacy/android/shared/original/core/ui/theme/extensions/ModifierExt.kt",
        """
            package mega.privacy.android.shared.original.core.ui.theme.extensions
            import androidx.compose.ui.Modifier
            fun Modifier.conditional(
                condition: Boolean,
                modifier: Modifier.() -> Modifier,
            ): Modifier = this
        """.trimIndent()
    ).indented().within("src")

    val modifierStubs = arrayOf(
        composeAnnotation,
        modifier,
        clickableModifiers,
        selectionModifiers,
        semanticsModifier,
        conditionalModifier,
    )

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