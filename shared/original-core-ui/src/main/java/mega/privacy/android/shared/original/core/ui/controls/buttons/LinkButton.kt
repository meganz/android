package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.button,
    textAlign: TextAlign? = null,
) = Text(
    text = text,
    modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    ),
    color = MegaOriginalTheme.colors.link.primary,
    style = style,
    textAlign = textAlign
)