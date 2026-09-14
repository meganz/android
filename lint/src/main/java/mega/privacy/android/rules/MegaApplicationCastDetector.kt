package mega.privacy.android.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastBinaryExpressionWithTypeKind

/**
 * Lint detector that flags any cast of an Application/Context to the concrete
 * [mega.privacy.android.app.MegaApplication] — both Kotlin `x as MegaApplication` and Java
 * `(MegaApplication) x`.
 *
 * This is the guardrail for the MegaApplication statics burn-down: reaching for the concrete
 * application to pull dependencies off it couples callers to the app module and hides real
 * dependencies. Every access has been migrated to a Hilt `@EntryPoint`
 * (e.g. `MegaApiEntryPoint`), a gateway, or a use case, so this is a hard (fatal) error: any
 * reintroduction of the cast fails the build.
 */
@Suppress("UnstableApiUsage")
internal class MegaApplicationCastDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UBinaryExpressionWithType::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) {
                if (node.operationKind !is UastBinaryExpressionWithTypeKind.TypeCast) return
                val typeFqn = (node.type as? PsiClassType)?.resolve()?.qualifiedName
                    ?: node.type.canonicalText
                if (typeFqn == MEGA_APPLICATION_FQN) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = BRIEF_DESCRIPTION,
                    )
                }
            }
        }

    companion object {
        private const val MEGA_APPLICATION_FQN = "mega.privacy.android.app.MegaApplication"

        private const val BRIEF_DESCRIPTION =
            "Avoid casting to the concrete MegaApplication. Obtain dependencies through a Hilt " +
                "@EntryPoint (e.g. MegaApiEntryPoint), a gateway, or a use case instead of casting " +
                "the Application/Context to MegaApplication."

        /**
         * Issue reported by this detector.
         */
        val ISSUE = Issue.create(
            id = "MegaApplicationCast",
            briefDescription = BRIEF_DESCRIPTION,
            explanation = """
                Casting an Application or Context to the concrete MegaApplication (`x as \
                MegaApplication` in Kotlin, `(MegaApplication) x` in Java) reaches into the \
                application object to pull global state and dependencies off it. This couples the \
                caller to the app module, hides real dependencies, and makes the class harder to \
                test. Every such cast has been removed. Inject what you need via Hilt, use a Hilt \
                @EntryPoint (for example MegaApiEntryPoint) for classes that cannot be \
                constructor-injected, or go through a gateway or use case.
            """,
            category = CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(MegaApplicationCastDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
