package test.mega.privacy.android.app

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes id: Int
) = onNodeWithText(fromId(id = id))

internal fun fromId(@StringRes id: Int) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

internal fun hasBackgroundColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has a background color of '$color'"
) {
    return@SemanticsMatcher it.layoutInfo.getModifierInfo().any {
        it.modifier == Modifier.background(color)
    }
}