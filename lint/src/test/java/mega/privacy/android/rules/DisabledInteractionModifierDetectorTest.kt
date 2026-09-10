package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import mega.privacy.android.rules.compose.DisabledInteractionModifierDetector
import org.intellij.lang.annotations.Language
import org.junit.Test

class DisabledInteractionModifierDetectorTest : BaseLintTest() {

    override fun getDetector(): Detector = DisabledInteractionModifierDetector()

    override fun getIssues(): List<Issue> = listOf(DisabledInteractionModifierDetector.ISSUE)

    // An import alias renames the modifier call itself, which the detector matches by name.
    override val skipTestModes: Array<TestMode> = arrayOf(TestMode.IMPORT_ALIAS)

    @Test
    fun `test that clickable reports a warning when enabled comes from a parameter`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            @Composable
            fun Row(enabled: Boolean, onClick: () -> Unit) {
                Modifier.clickable(enabled = enabled, onClick = onClick)
            }
            """.trimIndent()

        runLint(code)
            .expectWarningCount(1)
            .expectContains("Modifier.conditional(enabled)")
    }

    @Test
    fun `test that combinedClickable reports a warning when enabled comes from a parameter`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.combinedClickable

            @Composable
            fun GenericListItem(enableClick: Boolean, onClick: () -> Unit) {
                Modifier.combinedClickable(enabled = enableClick, onClick = onClick)
            }
            """.trimIndent()

        runLint(code).expectWarningCount(1)
    }

    @Test
    fun `test that toggleable reports a warning when enabled is always false`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.selection.toggleable

            @Composable
            fun Checkbox(checked: Boolean) {
                Modifier.toggleable(value = checked, enabled = false, onValueChange = {})
            }
            """.trimIndent()

        runLint(code)
            .expectWarningCount(1)
            .expectContains("can never react to input")
    }

    @Test
    fun `test that selectable and triStateToggleable report a warning when enabled is a property`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.selection.selectable
            import androidx.compose.foundation.selection.triStateToggleable

            class State(val interactive: Boolean)

            @Composable
            fun Options(state: State) {
                Modifier.selectable(selected = true, enabled = state.interactive, onClick = {})
                Modifier.triStateToggleable(state = true, enabled = state.interactive, onClick = {})
            }
            """.trimIndent()

        runLint(code).expectWarningCount(2)
    }

    @Test
    fun `test that a modifier extension function reports a warning when enabled comes from a parameter`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            fun Modifier.rowClick(enabled: Boolean, onClick: () -> Unit): Modifier =
                clickable(enabled = enabled, onClick = onClick)
            """.trimIndent()

        runLint(code).expectWarningCount(1)
    }

    @Test
    fun `test that clickable reports a warning when a branch applies it with enabled false`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable
            import androidx.compose.foundation.combinedClickable

            @Composable
            fun NodeListViewItem(enabled: Boolean, onClick: () -> Unit) {
                val modifier = if (enabled) {
                    Modifier.combinedClickable(onClick = onClick)
                } else {
                    Modifier.clickable(enabled = false) { }
                }
            }
            """.trimIndent()

        runLint(code).expectWarningCount(1)
    }

    @Test
    fun `test that clickable reports a warning when enabled is a parameter defaulting to true`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            @Composable
            fun MegaButtonWithIcon(
                modifier: Modifier = Modifier,
                onClick: () -> Unit,
                enabled: Boolean = true,
            ) {
                modifier.clickable(enabled = enabled) { onClick() }
            }
            """.trimIndent()

        runLint(code).expectWarningCount(1)
    }

    @Test
    fun `test that clickable reports a warning when the guard tests an unrelated condition`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            @Composable
            fun VideoItem(showMenuButton: Boolean, isSelected: Boolean, onMenuClick: () -> Unit) {
                if (showMenuButton) {
                    Modifier.clickable(enabled = !isSelected, onClick = onMenuClick)
                }
            }
            """.trimIndent()

        runLint(code).expectWarningCount(1)
    }

    @Test
    fun `test that clickable reports nothing when enabled is omitted`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            @Composable
            fun Row(onClick: () -> Unit) {
                Modifier.clickable(onClick = onClick)
            }
            """.trimIndent()

        runLint(code).expectClean()
    }

    @Test
    fun `test that clickable reports nothing when enabled is always true`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable

            const val ALWAYS_CLICKABLE = true

            @Composable
            fun Row(onClick: () -> Unit) {
                Modifier.clickable(enabled = true, onClick = onClick)
                Modifier.clickable(enabled = ALWAYS_CLICKABLE, onClick = onClick)
            }
            """.trimIndent()

        runLint(code).expectClean()
    }

    @Test
    fun `test that clickable reports nothing when the modifier is applied inside a conditional`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.semantics.semantics
            import androidx.compose.foundation.clickable
            import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional

            @Composable
            fun Row(enabled: Boolean, onClick: () -> Unit) {
                val guarded = if (enabled) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier.semantics(mergeDescendants = true)
                }
                val helper = Modifier.conditional(enabled) {
                    clickable(enabled = enabled, onClick = onClick)
                }
                val branched = when {
                    enabled -> Modifier.clickable(enabled = enabled, onClick = onClick)
                    else -> Modifier
                }
            }
            """.trimIndent()

        runLint(code).expectClean()
    }

    @Test
    fun `test that clickable reports nothing when it is not applied to a modifier`() {
        @Language("kotlin")
        val code =
            """
            package mega.privacy.android.test

            class LegacyView {
                fun clickable(enabled: Boolean) = enabled
            }

            fun setUp(view: LegacyView, enabled: Boolean) {
                view.clickable(enabled = enabled)
            }
            """.trimIndent()

        runLint(code).expectClean()
    }

    private fun runLint(@Language("kotlin") code: String) =
        lint()
            .files(*Stubs.modifierStubs, kotlin(code))
            .issues(DisabledInteractionModifierDetector.ISSUE)
            .run()
}
