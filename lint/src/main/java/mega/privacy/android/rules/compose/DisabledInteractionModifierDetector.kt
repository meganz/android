package mega.privacy.android.rules.compose

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.USwitchClauseExpression

/**
 * Reports interaction modifiers that are applied unconditionally and then turned off through their
 * `enabled` argument.
 *
 * Compose does not remove a disabled interaction modifier: the node, its bounds and its
 * pointer-input consumption all survive, so the element stops looking interactive while it keeps
 * swallowing taps and keeps reporting itself to accessibility services.
 */
internal class DisabledInteractionModifierDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (!isKotlin(context.uastFile?.lang)) return null
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val modifierName = node.methodName?.takeIf { it in InteractionModifiers } ?: return
                if (!node.hasModifierReceiver()) return

                val enabledArgument = node.enabledArgument(context) ?: return
                val enabled = constantValueOf(context, enabledArgument)
                if (enabled == true) return
                // A constant `false` is dead in every branch, so a surrounding guard does not
                // excuse it; a dynamic value may well be what the surrounding guard tests.
                if (enabled != false && node.isGuardedBy(enabledArgument)) return

                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(node),
                    message = message(modifierName, enabled == false),
                )
            }
        }
    }

    private fun message(modifierName: String, alwaysDisabled: Boolean): String {
        val cause = if (alwaysDisabled) {
            "`$modifierName` is applied with `enabled = false`, so it can never react to input."
        } else {
            "`$modifierName` is applied unconditionally and only disabled through `enabled`."
        }
        return "$cause A disabled interaction modifier is not removed by Compose: it keeps its " +
                "bounds, keeps consuming pointer input and keeps publishing a clickable " +
                "semantics node, so the element stops looking interactive while it still " +
                "swallows taps meant for whatever is behind it and still reports itself to " +
                "accessibility services. Apply the modifier only when it is enabled, for " +
                "example with the `Modifier.conditional(enabled) { ... }` helper or an `if` " +
                "around the modifier. Keep `semantics(mergeDescendants = true)` on the disabled " +
                "branch so the element still announces as a single node — dropping the modifier " +
                "without it loses semantics merging, which is its own regression. If the intent " +
                "really is to absorb input so it cannot reach a parent, suppress this check with " +
                "`@Suppress(\"$ISSUE_ID\")` and say why."
    }

    /**
     * The compile-time value of [argument], or null when it varies at runtime. A parameter is never
     * constant even when it declares a default: every caller can pass something else.
     */
    private fun constantValueOf(context: JavaContext, argument: UExpression): Boolean? {
        var expression = argument
        while (expression is UParenthesizedExpression) expression = expression.expression
        if ((expression as? UResolvable)?.resolve() is PsiParameter) return null
        return ConstantEvaluator.evaluate(context, expression) as? Boolean
    }

    private fun UCallExpression.enabledArgument(context: JavaContext): UExpression? {
        val method = resolve()
        if (method != null) {
            context.evaluator.computeArgumentMapping(this, method).entries
                .firstOrNull { (_, parameter) -> parameter.name == ENABLED_PARAMETER }
                ?.let { (argument, _) -> return argument }
        }
        return namedEnabledArgument()
    }

    /**
     * Fallback for call sites lint cannot resolve, where the argument mapping is unavailable but a
     * named argument is still readable from the source.
     */
    private fun UCallExpression.namedEnabledArgument(): UExpression? {
        val ktCall = sourcePsi as? KtCallExpression ?: return null
        val argument = ktCall.valueArguments.firstOrNull {
            it.getArgumentName()?.asName?.asString() == ENABLED_PARAMETER
        } ?: return null
        return valueArguments.firstOrNull { it.sourcePsi == argument.getArgumentExpression() }
    }

    private fun UCallExpression.hasModifierReceiver(): Boolean {
        receiverType?.let { type ->
            // `Modifier.clickable(...)` resolves to the companion object rather than the interface.
            val name = type.canonicalText
                .substringBefore('<')
                .removeSuffix(".Companion")
                .substringAfterLast('.')
            return ModifierNames.contains(name)
        }
        val function = PsiTreeUtil.getParentOfType(sourcePsi, KtNamedFunction::class.java)
            ?: return false
        return function.isComposable || function.isModifier
    }

    /**
     * True when the modifier is already applied inside a branch that tests the same thing the
     * `enabled` argument does, which is the shape the fix takes. Comparing the identifiers keeps a
     * guard on an unrelated condition — a menu button that is only composed when it is shown, say —
     * from hiding a row that still swallows taps once `enabled` turns false.
     */
    private fun UCallExpression.isGuardedBy(enabledArgument: UExpression): Boolean {
        val enabledIdentifiers = identifiersIn(enabledArgument.sourcePsi?.text)
        if (enabledIdentifiers.isEmpty()) return false

        var current: UElement? = this
        var parent = uastParent
        while (parent != null) {
            val condition = when {
                parent is UIfExpression && parent.condition != current -> parent.condition
                parent is USwitchClauseExpression -> parent.caseValues.firstOrNull()
                parent is ULambdaExpression -> {
                    val enclosingCall = parent.uastParent as? UCallExpression
                        ?: (parent.uastParent as? UQualifiedReferenceExpression)
                            ?.selector as? UCallExpression
                    enclosingCall
                        ?.takeIf { it.methodName in ConditionalModifierNames }
                        ?.valueArguments?.firstOrNull()
                }

                parent is UMethod -> return false
                else -> null
            }
            if (condition != null &&
                identifiersIn(condition.sourcePsi?.text).any { it in enabledIdentifiers }
            ) {
                return true
            }
            current = parent
            parent = parent.uastParent
        }
        return false
    }

    private fun identifiersIn(text: String?): Set<String> =
        text?.let { IdentifierPattern.findAll(it).map { match -> match.value }.toSet() }
            ?.minus(NonIdentifiers)
            .orEmpty()

    companion object {
        private const val ENABLED_PARAMETER = "enabled"
        private const val ISSUE_ID = "DisabledInteractionModifier"

        private val InteractionModifiers = setOf(
            "clickable",
            "combinedClickable",
            "toggleable",
            "selectable",
            "triStateToggleable",
        )

        private val ConditionalModifierNames = setOf("conditional")

        private val IdentifierPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")

        private val NonIdentifiers = setOf("true", "false", "null", "it", "this", "is", "in", "as")

        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = "Interaction modifier disabled instead of not applied",
            explanation = "Compose keeps a disabled interaction modifier in the layout: its node, " +
                    "its bounds and its pointer-input consumption all remain. `Modifier.clickable(enabled = false)` " +
                    "and its siblings therefore produce an element that offers no affordance to the " +
                    "user yet still absorbs taps meant for its parent and still announces itself to " +
                    "accessibility services, often over bounds smaller than the minimum touch target. " +
                    "Apply the modifier only when it is enabled — the repo has a `Modifier.conditional` " +
                    "helper for exactly this — and keep `semantics(mergeDescendants = true)` on the " +
                    "disabled branch so the element is still announced as a single node. Deliberately " +
                    "absorbing input so it cannot reach a parent is a legitimate but rare intent; " +
                    "suppress the check at those call sites so the intent is explicit.",
            category = Category.A11Y,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                DisabledInteractionModifierDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
