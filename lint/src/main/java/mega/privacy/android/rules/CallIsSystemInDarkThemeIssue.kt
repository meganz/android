package mega.privacy.android.rules

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.tryResolveNamed
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Call is system in dark theme issue
 *
 */
@Suppress("UnstableApiUsage", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
object CallIsSystemInDarkThemeIssue {

    private const val ID = "isSystemInDarkTheme"

    private const val PRIORITY = 8

    private const val DESCRIPTION =
        "isSystemInDarkTheme does not allow, you should use GetThemeMode UseCase"

    private const val EXPLANATION = """
        We have a setting that overrides the default dark theme of the system. 
        We should be checking our setting by GetThemeMode UseCase instead of using the provided isSystemInDarkTheme method.
    """

    private val SEVERITY = Severity.WARNING

    private const val THEME_METHOD = "AndroidTheme"
    private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
    private const val PREVIEW_COMPOSABLE_ANNOTATION =
        "androidx.compose.ui.tooling.preview.Preview"

    /**
     * Issue
     */
    val ISSUE = Issue.create(
        id = ID,
        briefDescription = DESCRIPTION,
        explanation = EXPLANATION,
        category = CORRECTNESS,
        priority = PRIORITY,
        severity = SEVERITY,
        Implementation(IssueDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )

    internal class IssueDetector : Detector(), Detector.UastScanner {
        override fun getApplicableMethodNames(): List<String> = listOf(THEME_METHOD)

        override fun visitMethodCall(
            context: JavaContext,
            themeNode: UCallExpression,
            method: PsiMethod,
        ) {
            var parent: UElement? = themeNode
            while (parent != null) {
                if (parent is UMethod) {
                    // reach the root of method
                    break
                }
                parent = parent.uastParent
            }
            // Check if method is not @Compose or contains @Preview
            val root = (parent as? UMethod) ?: return
            if (root.hasAnnotation(COMPOSABLE_ANNOTATION).not()
                || root.hasAnnotation(PREVIEW_COMPOSABLE_ANNOTATION)
            ) return

            val firstParam =
                themeNode.getArgumentForParameter(0)?.skipParenthesizedExprDown() ?: return
            if (firstParam is UCallExpression) {
                // in case pass isSystemInDarkTheme as method
                isSystemDarkThemeComposeMethod(context, firstParam, themeNode)
            } else {
                // in cass pass isSystemInDarkTheme as variable
                val paramName = firstParam.tryResolveNamed()?.name
                root.accept(visitor = object : UastVisitor {
                    override fun visitElement(node: UElement): Boolean {
                        // find the statement assign value to variable
                        // example val isDark = isSystemInDarkTheme()
                        if (node is ULocalVariable && node.name == paramName) {
                            node.accept(object : UastVisitor {
                                override fun visitElement(node: UElement): Boolean {
                                    // find the method call isSystemInDarkTheme()
                                    if (node is UCallExpression) {
                                        return isSystemDarkThemeComposeMethod(context,
                                            node,
                                            themeNode)
                                    }
                                    return false
                                }
                            })
                            return false
                        }
                        return false
                    }
                })
            }
        }

        private fun isSystemDarkThemeComposeMethod(
            context: JavaContext,
            node: UCallExpression,
            themeNode: UElement,
        ): Boolean {
            val resolved = node.tryResolveNamed()
            println((resolved as? PsiMember)?.containingClass?.qualifiedName)
            if (resolved?.name == "isSystemInDarkTheme"
                && (resolved as? PsiMember)?.containingClass?.qualifiedName?.contains("androidx.compose.foundation") == true
            ) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getNameLocation(themeNode),
                    message = DESCRIPTION
                )
                return true
            }
            return false
        }
    }
}