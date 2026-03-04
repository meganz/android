package mega.privacy.android.rules.compose

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.uast.UMethod

internal class ComposableFunctionVisibilityDetector : ComposableFunctionDetector() {

    override fun visitComposable(context: JavaContext, function: KtFunction, method: UMethod) {
        if (function.isPublic && method.isReturningUnit() && !isValidPublic(context, function)) {
            context.report(
                issue = ISSUE,
                scope = function,
                location = context.getNameLocation(function),
                message = "This @Composable function should not be public",
                quickfixData = createFix(context, function)
            )
        }
    }

    private fun isValidPublic(context: Context, function: KtFunction): Boolean {
        val projectPath = context.mainProject.dir.path
        val filePath = function.containingFile.virtualFile.path
        val relativeFilePath = filePath.removePrefix(projectPath).removePrefix("/")

        val moduleName = context.project.name

        val isInSharedModule = relativeFilePath.startsWith("shared/")
        val isSnowflakeModule =
            moduleName.endsWith("snowflake-components")
                    || moduleName.endsWith("snowflakes")
                    || moduleName.endsWith("core-ui")

        return isInSharedModule || isSnowflakeModule
    }

    private fun createFix(context: JavaContext, function: KtFunction): LintFix? {
        val publicModifier = function.modifierList?.getModifier(KtTokens.PUBLIC_KEYWORD)

        return if (publicModifier != null) {
            // Explicit public, replace it
            fix().name("Make internal")
                .replace()
                .range(context.getLocation(publicModifier))
                .with("internal")
                .autoFix()
                .build()
        } else if (function is KtNamedFunction) {
            // Implicit public, add internal modifier before the fun keyword
            fix().name("Make internal")
                .replace()
                .range(context.getLocation(function.funKeyword))
                .with("internal fun")
                .autoFix()
                .build()
        } else null
    }

    companion object {
        val ISSUE = Issue.create(
            id = "ComposablePublicFunction",
            briefDescription = "A @Composable function should not be public",
            explanation = "Exposing @Composable functions to other modules is not recommended. Only core-ui library, shared modules or snowflake modules should expose @Composable functions.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.INFORMATIONAL,
            implementation = Implementation(
                ComposableFunctionVisibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
