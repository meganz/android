package test.mega.privacy.android.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes id: Int
) = onNodeWithText(fromId(id = id))

internal fun fromId(@StringRes id: Int) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)