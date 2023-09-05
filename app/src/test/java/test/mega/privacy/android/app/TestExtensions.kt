package test.mega.privacy.android.app

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
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.DrawableResId

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes id: Int,
) = onNodeWithText(fromId(id = id))

internal fun SemanticsNodeInteractionsProvider.onNodeWithPlural(
    @PluralsRes id: Int, quantity: Int,
) = onNodeWithText(fromPluralId(id = id, quantity))

internal fun fromId(@StringRes id: Int) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

internal fun fromId(@StringRes id: Int, vararg args: Any) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

internal fun fromPluralId(@PluralsRes id: Int, quantity: Int) =
    InstrumentationRegistry.getInstrumentation().targetContext.resources
        .getQuantityString(id, quantity, quantity)

internal fun hasBackgroundColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has a background color of '$color'"
) {
    return@SemanticsMatcher it.layoutInfo.getModifierInfo().any {
        it.modifier == Modifier.background(color)
    }
}

internal fun hasTextColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has a text color of '$color'"
) { semanticsNode ->
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    semanticsNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)

    return@SemanticsMatcher if (textLayoutResults.isNotEmpty()) {
        textLayoutResults.first().layoutInput.style.color == color
    } else {
        false
    }
}

internal fun hasDrawable(@DrawableRes id: Int): SemanticsMatcher =
    SemanticsMatcher.expectValue(
        DrawableResId, id
    )

internal fun ComposeContentTestRule.performClickOnAllNodes(testTag: String) {
    onAllNodesWithTag(testTag)
        .filter(hasClickAction())
        .apply {
            fetchSemanticsNodes().forEachIndexed { i, _ ->
                get(i).performSemanticsAction(SemanticsActions.OnClick)
            }
        }
}
