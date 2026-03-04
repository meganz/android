package mega.privacy.android.feature.payment

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.DrawableResId

internal fun fromId(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

internal fun fromId(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

internal fun hasDrawable(@DrawableRes id: Int): SemanticsMatcher =
    SemanticsMatcher.expectValue(DrawableResId, id)

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(@StringRes id: Int) =
    onNodeWithText(fromId(id = id))

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(@StringRes id: Int, vararg args: Any) =
    onNodeWithText(fromId(id = id, *args))
