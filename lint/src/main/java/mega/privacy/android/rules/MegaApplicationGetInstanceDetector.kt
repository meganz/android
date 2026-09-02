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
 * Lint detector that flags any reference to [MegaApplication.getInstance].
 *
 * This is the guardrail for the MegaApplication statics burn-down: the goal is to stop the app
 * reaching into the singleton for global state and instead obtain dependencies through Hilt
 * injection or a Hilt `@EntryPoint`. The burn-down is complete and `MegaApplication.getInstance()`
 * has been removed, so this is a hard (fatal) error: any reintroduction fails the build.
 */
@Suppress("UnstableApiUsage")
internal class MegaApplicationGetInstanceDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("getInstance")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        val containingClassFqn = method.containingClass?.qualifiedName ?: return
        if (containingClassFqn == MEGA_APPLICATION_FQN ||
            containingClassFqn == MEGA_APPLICATION_COMPANION_FQN
        ) {
            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getNameLocation(node),
                message = BRIEF_DESCRIPTION,
            )
        }
    }

    companion object {
        private const val MEGA_APPLICATION_FQN = "mega.privacy.android.app.MegaApplication"
        private const val MEGA_APPLICATION_COMPANION_FQN =
            "mega.privacy.android.app.MegaApplication.Companion"

        private const val BRIEF_DESCRIPTION =
            "Avoid MegaApplication.getInstance(). Obtain dependencies through Hilt injection " +
                "or a Hilt @EntryPoint instead of reaching into the application singleton."

        /**
         * Issue reported by this detector.
         */
        val ISSUE = Issue.create(
            id = "MegaApplicationGetInstance",
            briefDescription = BRIEF_DESCRIPTION,
            explanation = """
                MegaApplication.getInstance() exposes the application singleton so that global \
                state and dependencies can be reached from anywhere. It has been fully burned down \
                and removed: every access makes the class harder to test and hides \
                real dependencies. Inject what you need via Hilt, or use a Hilt @EntryPoint for \
                classes that cannot be constructor-injected.
            """,
            category = CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(MegaApplicationGetInstanceDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
