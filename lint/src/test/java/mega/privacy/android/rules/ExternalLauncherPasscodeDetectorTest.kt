package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

@Suppress("UnstableApiUsage")
class ExternalLauncherPasscodeDetectorTest : BaseLintTest() {

    override fun getDetector(): Detector = ExternalLauncherPasscodeDetector()

    override fun getIssues(): List<Issue> = listOf(ExternalLauncherPasscodeDetector.ISSUE)

    private val activityResultStub = kotlin(
        "androidx/activity/compose/ActivityResult.kt",
        """
            package androidx.activity.compose
            import androidx.activity.result.contract.ActivityResultContract

            fun <I, O> rememberLauncherForActivityResult(
                contract: ActivityResultContract<I, O>,
                onResult: (O) -> Unit,
            ): Unit = Unit

            fun <I, O> rememberPasscodeAwareLauncher(
                contract: ActivityResultContract<I, O>,
                onResult: (O) -> Unit,
            ): Unit = Unit
        """.trimIndent()
    ).indented().within("src")

    private val activityResultContractsStub = kotlin(
        "androidx/activity/result/contract/ActivityResultContracts.kt",
        """
            package androidx.activity.result.contract

            abstract class ActivityResultContract<I, O>

            class ActivityResultContracts {
                class OpenDocument : ActivityResultContract<Array<String>, Any?>()
                class OpenMultipleDocuments : ActivityResultContract<Array<String>, List<Any?>>()
                class OpenDocumentTree : ActivityResultContract<Any?, Any?>()
                class GetContent : ActivityResultContract<String, Any?>()
                class CreateDocument(mimeType: String) : ActivityResultContract<String, Any?>()
                class StartActivityForResult : ActivityResultContract<Any?, Any?>()
                class StartIntentSenderForResult : ActivityResultContract<Any?, Any?>()
                class PickVisualMedia : ActivityResultContract<Any?, Any?>()
                class RequestPermission : ActivityResultContract<String, Boolean>()
            }
        """.trimIndent()
    ).indented().within("src")

    @Test
    fun `flags rememberLauncherForActivityResult with OpenDocument`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectWarningCount(1)
    }

    @Test
    fun `flags rememberLauncherForActivityResult with OpenDocumentTree`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectWarningCount(1)
    }

    @Test
    fun `flags rememberLauncherForActivityResult with StartIntentSenderForResult`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectWarningCount(1)
    }

    @Test
    fun `flags rememberLauncherForActivityResult with PickVisualMedia`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectWarningCount(1)
    }

    @Test
    fun `does not flag rememberLauncherForActivityResult with StartActivityForResult`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectClean()
    }

    @Test
    fun `does not flag rememberLauncherForActivityResult with RequestPermission`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberLauncherForActivityResult
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectClean()
    }

    @Test
    fun `does not flag rememberPasscodeAwareLauncher`() {
        lint().files(
            activityResultStub,
            activityResultContractsStub,
            kotlin(
                """
                    package test
                    import androidx.activity.compose.rememberPasscodeAwareLauncher
                    import androidx.activity.result.contract.ActivityResultContracts

                    fun test() {
                        rememberPasscodeAwareLauncher(ActivityResultContracts.OpenDocument()) {}
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectClean()
    }
}
