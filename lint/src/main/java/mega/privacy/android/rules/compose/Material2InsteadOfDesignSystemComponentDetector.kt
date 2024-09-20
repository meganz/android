package mega.privacy.android.rules.compose

import com.android.tools.lint.detector.api.Category
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

internal abstract class Material2InsteadOfDesignSystemComponentDetector(
    private val discouragedComponentName: String,
    private val discouragedComponentPackage: String = "androidx.compose.material",
    private val encouragedComponentName: String,
) : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(discouragedComponentName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.name.startsWith(discouragedComponentName) && node.resolve()?.containingClass?.qualifiedName == "$discouragedComponentPackage.${discouragedComponentName}Kt") {
            context.report(
                issue = createIssue(),
                scope = node,
                location = context.getLocation(node),
                message = explanation(),
                quickfixData = createFix(),
            )
        }
    }

    fun createIssue() = Issue.create(
        id = encouragedComponentName,
        briefDescription = "`$discouragedComponentName` instead of `$encouragedComponentName`",
        explanation = explanation(),
        category = Category.CORRECTNESS,
        priority = 7,
        severity = Severity.WARNING,
        Implementation(this::class.java, Scope.JAVA_FILE_SCOPE)
    )

    fun explanation() =
        "The use of `$discouragedComponentPackage.$discouragedComponentPackage` is discouraged. Please use `$encouragedComponentName` instead to follow our design system."

    private fun createFix() = LintFix.create()
        .name("Replace with $encouragedComponentName")
        .replace()
        .text(discouragedComponentName)
        .with(encouragedComponentName)
        .robot(true)
        .independent(true)
        .build()
}