package mega.privacy.android.rules.compose

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.UMethod

internal abstract class ComposableFunctionDetector(vararg options: LintOption) :
    OptionLoadingDetector(*options), SourceCodeScanner {

    final override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    final override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (!isKotlin(context.uastFile?.lang)) return null
        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                val ktFunction = node.sourcePsi as? KtFunction ?: return
                if (ktFunction.isComposable) {
                    visitComposable(context, ktFunction)
                }
            }
        }
    }

    abstract fun visitComposable(context: JavaContext, function: KtFunction)
}