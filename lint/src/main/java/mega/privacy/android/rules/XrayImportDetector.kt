package mega.privacy.android.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UImportStatement

/**
 * Xray import issue
 */
@Suppress("UnstableApiUsage")
internal class XrayImportDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        createUElementHandler(context)

    private fun createUElementHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitImportStatement(node: UImportStatement) {
            val importReference = node.importReference ?: return
            val import = importReference.asRenderString()

            if (import.contains(XRAY_PATH)) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getNameLocation(importReference),
                    message = DESCRIPTION,
                )
            }
        }
    }

    companion object {
        private const val ID = "Xray"
        private const val DESCRIPTION =
            "Reminder to remove this import once your development is done before commit your changes"
        private const val EXPLANATION =
            "Since Xray is imported with debugImplementation, commit it will cause pipeline for qa/release build failed"
        private const val XRAY_PATH = "mega.privacy.android.xray"

        /**
         * Issue
         */
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            Implementation(XrayImportDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
