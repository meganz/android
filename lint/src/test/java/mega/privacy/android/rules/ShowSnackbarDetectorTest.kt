package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ShowSnackbarDetectorTest {
    @Test
    fun `test that when showSnackbar method of SnackbarHostState is used without duration then it shows the correct warning`() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.material.SnackbarHostState

                        suspend fun someViewModelMethod(snackbarHostState: SnackbarHostState) {
                            snackbarHostState.showSnackbar("some message")
                        }
                    """
            ).indented()
        ).issues(ShowSnackbarDetector.ISSUE)
            .run()
            .expectWarningCount(1)
            .expectContains(ShowSnackbarDetector.EXPLANATION.replace("`", ""))
    }

    @Test
    fun `test that when showSnackbar method of SnackbarHostState is used with duration then it shows no warnings`() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.material.SnackbarHostState
                        import androidx.compose.material.SnackbarDuration

                        suspend fun someViewModelMethod(snackbarHostState: SnackbarHostState) {
                            snackbarHostState.showSnackbar("some message", duration = SnackbarDuration.Short)
                        }
                    """
            ).indented()
        ).issues(ShowSnackbarDetector.ISSUE)
            .run()
            .expectWarningCount(0)
    }
}