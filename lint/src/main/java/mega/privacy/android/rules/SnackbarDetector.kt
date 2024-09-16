package mega.privacy.android.rules

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * Snackbar detector
 */
class SnackbarDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf(METHOD_NAME)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.name.startsWith(METHOD_NAME) && node.resolve()?.containingClass?.qualifiedName == "androidx.compose.material.SnackbarKt") {
            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = EXPLANATION,
                quickfixData = fix,
            )
        }
    }

    companion object {
        private const val METHOD_NAME = "Snackbar"
        private const val ID = "MegaSnackbar"
        internal const val EXPLANATION =
            "The use of `androidx.compose.material.Snackbar` is discouraged. Please use `MegaSnackbar` instead to follow our design system."
        private const val DESCRIPTION =
            "`Snackbar` instead of `MegaSnackbar`"

        private val fix = LintFix.create()
            .name("Replace with MegaSnackbar")
            .replace()
            .text("Snackbar")
            .with("MegaSnackbar")
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
            Implementation(SnackbarDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}