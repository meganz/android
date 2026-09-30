package mega.privacy.android.core.ui.test

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.sharedcomponents.semantics.DrawableResId

/**
 * Resolves [id] against the target context's resources.
 */
fun fromId(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

/**
 * Resolves [id] against the target context's resources, formatting it with [args].
 */
fun fromId(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/**
 * Resolves the plural [id] for [quantity], passing the quantity as the sole format argument.
 */
fun fromPluralId(@PluralsRes id: Int, quantity: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.resources
        .getQuantityString(id, quantity, quantity)

/**
 * Resolves the plural [id] for [quantity], formatting it with [args].
 */
fun fromPluralId(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.resources
        .getQuantityString(id, quantity, *args)

/**
 * Finds the node showing the string [id].
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(@StringRes id: Int) =
    onNodeWithText(fromId(id = id))

/**
 * Finds the node showing the string [id] formatted with [args].
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(@StringRes id: Int, vararg args: Any) =
    onNodeWithText(fromId(id = id, *args))

/**
 * Finds the node showing the plural [id] for [quantity].
 */
fun SemanticsNodeInteractionsProvider.onNodeWithPlural(@PluralsRes id: Int, quantity: Int) =
    onNodeWithText(fromPluralId(id = id, quantity))

/**
 * Finds the node showing the plural [id] for [quantity] formatted with [args].
 */
fun SemanticsNodeInteractionsProvider.onNodeWithPlural(
    @PluralsRes id: Int,
    quantity: Int,
    vararg args: Any,
) = onNodeWithText(fromPluralId(id = id, quantity, args))

/**
 * Matches a composable that published [id] through [DrawableResId].
 */
fun hasDrawable(@DrawableRes id: Int): SemanticsMatcher =
    SemanticsMatcher.expectValue(DrawableResId, id)

/**
 * Matches a composable whose modifier chain sets a background of [color].
 */
fun hasBackgroundColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has a background color of '$color'"
) { semanticsNode ->
    semanticsNode.layoutInfo.getModifierInfo().any {
        it.modifier == Modifier.background(color)
    }
}

/**
 * Matches a text composable laid out in [color].
 */
fun hasTextColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has a text color of '$color'"
) { semanticsNode ->
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    semanticsNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)

    if (textLayoutResults.isNotEmpty()) {
        textLayoutResults.first().layoutInput.style.color == color
    } else {
        false
    }
}

/**
 * Clicks every clickable node tagged [testTag].
 */
fun ComposeContentTestRule.performClickOnAllNodes(testTag: String) {
    onAllNodesWithTag(testTag)
        .filter(hasClickAction())
        .apply {
            fetchSemanticsNodes().forEachIndexed { index, _ ->
                get(index).performSemanticsAction(SemanticsActions.OnClick)
            }
        }
}
