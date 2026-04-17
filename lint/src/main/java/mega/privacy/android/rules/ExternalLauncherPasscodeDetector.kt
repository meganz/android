package mega.privacy.android.rules

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * Lint detector that flags usage of [rememberLauncherForActivityResult] with contracts
 * known to launch external activities (file pickers, document scanner, etc.).
 *
 * These should use [rememberPasscodeAwareLauncher] instead, which automatically suppresses
 * the passcode prompt when the user returns from the external activity.
 */
@Suppress("UnstableApiUsage")
internal class ExternalLauncherPasscodeDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf("rememberLauncherForActivityResult")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        val argMapping = context.evaluator.computeArgumentMapping(node, method)
        val contractArg = argMapping.entries
            .firstOrNull { (_, param) -> param.name == "contract" }?.key ?: return
        val contractType = contractArg.getExpressionType()?.canonicalText ?: return

        if (EXTERNAL_CONTRACT_TYPES.any { contractType.contains(it) }) {
            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getNameLocation(node),
                message = BRIEF_DESCRIPTION,
            )
        }
    }

    companion object {
        private val EXTERNAL_CONTRACT_TYPES = setOf(
            "OpenDocument",
            "OpenMultipleDocuments",
            "OpenDocumentTree",
            "GetContent",
            "GetMultipleContents",
            "CreateDocument",
            "PickVisualMedia",
            "PickMultipleVisualMedia",
            "StartIntentSenderForResult",
            "OpenMultipleDocumentsPersistable",
        )

        private const val BRIEF_DESCRIPTION =
            "Use rememberPasscodeAwareLauncher() for contracts that launch external activities, " +
                "to automatically suppress the passcode prompt on return."

        /**
         * Issue reported by this detector.
         */
        val ISSUE = Issue.create(
            id = "ExternalLauncherPasscode",
            briefDescription = BRIEF_DESCRIPTION,
            explanation = """
                Contracts like OpenDocument, OpenDocumentTree, StartIntentSenderForResult, etc. \
                launch external activities. When the user returns, the app's passcode check \
                triggers unnecessarily. Use rememberPasscodeAwareLauncher() which wraps the \
                contract to automatically skip the passcode check for external launches.
            """,
            category = CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            Implementation(ExternalLauncherPasscodeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
