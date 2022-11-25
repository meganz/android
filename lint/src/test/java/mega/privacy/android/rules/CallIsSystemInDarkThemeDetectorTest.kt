package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class CallIsSystemInDarkThemeDetectorTest {
    @Test
    fun test_when_no_preview_annotation_and_pass_reference_of_isSystemInDarkTheme_as_parameter_then_show_error() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.foundation.isSystemInDarkTheme
                        import androidx.compose.runtime.Composable
                        import mega.privacy.android.presentation.theme.AndroidTheme

                        @Composable
                        fun AskForDisplayOverDialog() {
                            val isDark = isSystemInDarkTheme()
                            AndroidTheme(isDark = isDark) {

                            }
                        }
                    """
            ).indented()
        ).issues(CallIsSystemInDarkThemeDetector.ISSUE)
            .run()
            .expect(
                """
                        src/mega/privacy/android/presentation/controls/test.kt:10: Warning: isSystemInDarkTheme does not allow, you should use GetThemeMode UseCase [IsSystemInDarkTheme]
                            AndroidTheme(isDark = isDark) {
                            ~~~~~~~~~~~~
                        0 errors, 1 warnings
                    """
            )
    }

    @Test
    fun test_when_no_preview_annotation_and_pass_isSystemInDarkTheme_as_parameter_then_show_error() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.foundation.isSystemInDarkTheme
                        import androidx.compose.runtime.Composable
                        import mega.privacy.android.presentation.theme.AndroidTheme

                        @Composable
                        fun AskForDisplayOverDialog() {
                            AndroidTheme(isDark = isSystemInDarkTheme()) {

                            }
                        }
                    """
            ).indented()
        ).issues(CallIsSystemInDarkThemeDetector.ISSUE)
            .run()
            .expect(
                """
                        src/mega/privacy/android/presentation/controls/test.kt:9: Warning: isSystemInDarkTheme does not allow, you should use GetThemeMode UseCase [IsSystemInDarkTheme]
                            AndroidTheme(isDark = isSystemInDarkTheme()) {
                            ~~~~~~~~~~~~
                        0 errors, 1 warnings
                    """
            )
    }

    @Test
    fun `test_when_function_has_preview_annotation_then_no_warning`() {
        lint().files(
            *Stubs.stubs,
            kotlin(
                """
                        package mega.privacy.android.presentation.controls

                        import androidx.compose.foundation.isSystemInDarkTheme
                        import androidx.compose.runtime.Composable
                        import androidx.compose.ui.tooling.preview.Preview
                        import mega.privacy.android.presentation.theme.AndroidTheme

                        @Preview
                        @Composable
                        fun PreviewAskForDisplayOverDialog() {
                            val isDark = isSystemInDarkTheme()
                            AndroidTheme(isDark = isDark) {

                            }
                        }
                    """
            ).indented()
        ).issues(CallIsSystemInDarkThemeDetector.ISSUE)
            .run()
            .expect(
                """
                        No warnings.
                    """
            )
    }
}