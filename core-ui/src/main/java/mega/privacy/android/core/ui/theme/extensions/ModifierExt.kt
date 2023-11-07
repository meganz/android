package mega.privacy.android.core.ui.theme.extensions

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/**
 * Conditional execution wrapper for the Modifier class
 * it's the equivalent of `if (condition)` on the modifier
 * @param condition the condition of which the block will executes
 * @param modifier block to invoke and returns [Modifier] as self
 */
@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.conditional(
    condition: Boolean,
    modifier: @Composable Modifier.() -> Modifier,
): Modifier = composed {
    if (condition) then(modifier(Modifier)) else this
}

/**
 * Modifier extension to give the ability of autofill to Compose TextField
 * @param autofillTypes types of AutoFill, see [AutofillType]
 * @param onAutoFilled block to invoke and returns [Modifier] as self
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onAutoFilled: ((String) -> Unit),
): Modifier = composed {
    val node = AutofillNode(onFill = onAutoFilled, autofillTypes = autofillTypes)
    val autofill = LocalAutofill.current
    LocalAutofillTree.current += node

    this.onGloballyPositioned {
        node.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        autofill?.run {
            if (focusState.isFocused && node.boundingBox != null) {
                requestAutofillForNode(node)
            } else {
                cancelAutofillForNode(node)
            }
        }
    }
}