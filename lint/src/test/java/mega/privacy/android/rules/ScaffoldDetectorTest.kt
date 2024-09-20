package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ScaffoldDetectorTest {
    @Test
    fun `test that when Scaffold is used then it shows the correct warning`() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.runtime.Composable
                        import androidx.compose.material.Scaffold

                        @Composable
                        fun SomeScreen() {
                            Scaffold() {
                                
                            }
                        }
                    """
            ).indented()
        ).issues(ScaffoldDetector.ISSUE)
            .run()
            .expectWarningCount(1)
            .expectContains(ScaffoldDetector().explanation().replace("`", ""))
    }
}