package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class SnackbarDetectorTest {
    @Test
    fun `test that when Snackbar is used then it shows the correct warning`() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.runtime.Composable
                        import androidx.compose.material.Snackbar

                        @Composable
                        fun SomeScreen() {
                            Snackbar(
                                snackbarData = object : SnackbarData {
                                    override val actionLabel = null
                                    override val duration = SnackbarDuration.Short
                                    override val message = "message"
                                    override fun dismiss() {}
                                    override fun performAction() {}
                                }
                            )
                        }
                    """
            ).indented()
        ).issues(SnackbarDetector.ISSUE)
            .run()
            .expectWarningCount(1)
            .expectContains(SnackbarDetector().explanation().replace("`", ""))
    }
}