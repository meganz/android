package mega.privacy.android.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * Show snack bar detector
 */
internal class ShowSnackbarDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement?>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve() ?: return
                if (method.name == "showSnackbar" && method.containingClass?.qualifiedName?.contains(
                        "androidx.compose.material.SnackbarHost"
                    ) == true
                ) {
                    if (node.valueArguments.none { arg ->
                            arg.getExpressionType()?.canonicalText == "androidx.compose.material.SnackbarDuration"
                        }) {
                        context.report(
                            issue = ISSUE,
                            scope = node,
                            location = context.getLocation(node),
                            message = EXPLANATION,
                            quickfixData = fix,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val ID = "ShowSnackbar"
        internal const val EXPLANATION =
            "The use of `showSnackbar` without an explicit duration is discouraged. Please use `mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar` instead to follow our design system or made duration explicit if design system duration needs to be override."
        private const val DESCRIPTION =
            "`showAutoDurationSnackbar` instead of `showSnackbar`"

        private val fix = LintFix.create()
            .name("Replace with showAutoDurationSnackbar")
            .replace()
            .text("showSnackbar")
            .with("showAutoDurationSnackbar")
            .robot(true)
            .independent(true)
            .build()

        /**
         * Issue
         */
        val ISSUE = Issue.create(
            id = ID,
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            Implementation(ShowSnackbarDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}