package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

@Suppress("UnstableApiUsage")
class MegaApplicationCastDetectorTest : BaseLintTest() {

    override fun getDetector(): Detector = MegaApplicationCastDetector()

    override fun getIssues(): List<Issue> = listOf(MegaApplicationCastDetector.ISSUE)

    private val megaApplicationStub: TestFile = kotlin(
        "mega/privacy/android/app/MegaApplication.kt",
        """
            package mega.privacy.android.app

            import android.app.Application

            class MegaApplication : Application()
        """.trimIndent()
    ).indented().within("src")

    @Test
    fun `test that detector flags cast to MegaApplication`() {
        lint().files(
            megaApplicationStub,
            kotlin(
                """
                    package test
                    import android.content.Context
                    import mega.privacy.android.app.MegaApplication

                    fun test(context: Context) {
                        val app = (context.applicationContext as MegaApplication)
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectErrorCount(1)
    }

    @Test
    fun `test that detector flags safe cast to MegaApplication`() {
        lint().files(
            megaApplicationStub,
            kotlin(
                """
                    package test
                    import android.content.Context
                    import mega.privacy.android.app.MegaApplication

                    fun test(context: Context) {
                        val app = context.applicationContext as? MegaApplication
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectErrorCount(1)
    }

    @Test
    fun `test that detector flags java cast to MegaApplication`() {
        lint().files(
            megaApplicationStub,
            java(
                """
                    package test;
                    import android.content.Context;
                    import mega.privacy.android.app.MegaApplication;

                    class JavaCaster {
                        void test(Context context) {
                            MegaApplication app = (MegaApplication) context.getApplicationContext();
                        }
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectErrorCount(1)
    }

    @Test
    fun `test that detector does not flag cast to another type`() {
        lint().files(
            kotlin(
                """
                    package test
                    import android.app.Application
                    import android.content.Context

                    fun test(context: Context) {
                        val app = context.applicationContext as Application
                    }
                """.trimIndent()
            ).indented().within("src")
        ).run().expectClean()
    }
}
