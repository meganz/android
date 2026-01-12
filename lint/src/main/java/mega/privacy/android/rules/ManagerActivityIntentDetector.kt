package mega.privacy.android.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CUSTOM_LINT_CHECKS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement

/**
 * Detector that flags the creation of Intent for ManagerActivity
 */
internal class ManagerActivityIntentDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement?>> =
        listOf(UClassLiteralExpression::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClassLiteralExpression(node: UClassLiteralExpression) {
                val type = node.type
                val typeName = type?.canonicalText
                if (typeName?.endsWith(".ManagerActivity") == true ||
                    typeName == "ManagerActivity"
                ) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = EXPLANATION,
                    )
                }
            }

            override fun visitCallExpression(node: UCallExpression) {}
        }
    }

    companion object {
        private const val ID = "ManagerActivityIntent"
        private const val DESCRIPTION =
            "Direct access to ManagerActivity::class.java is discouraged"
        private const val EXPLANATION =
            "The creation of an Intent for ManagerActivity using `ManagerActivity::class.java` is discouraged as this Activity is deprecated. Please use an alternative approach in AppNavigator: openManagerActivity with NavKey fallback to be sure that it's only used when Single activity feature flag is false"

        /**
         * Issue
         */
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = CUSTOM_LINT_CHECKS,
            priority = 7,
            severity = Severity.WARNING,
            Implementation(ManagerActivityIntentDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
