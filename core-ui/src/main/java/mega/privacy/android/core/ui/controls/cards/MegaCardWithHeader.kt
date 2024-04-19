package mega.privacy.android.core.ui.controls.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Card with header to display info
 *
 * @param header    Header of the card
 * @param body      Body of the card
 * @param modifier  [Modifier]
 */
@Composable
fun MegaCardWithHeader(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.border(
            width = 1.dp,
            color = MegaTheme.colors.border.strong,
            shape = RoundedCornerShape(12.dp),
        )
    ) {
        Box(
            modifier = modifier
                .height(28.dp)
                .background(
                    color = MegaTheme.colors.background.surface1,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
        ) {
            header()
        }

        body()
    }
}

@CombinedThemePreviews
@Composable
private fun MegaCardWithHeaderPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaCardWithHeader(
            header = {
                Row(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MegaText(
                        text = "Card Header Text",
                        textColor = TextColor.Secondary,
                        style = MaterialTheme.typography.caption
                    )
                }
            },
            body = {
                Row(
                    Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    MegaText(
                        text = "Card Body Text",
                        textColor = TextColor.Primary,
                        modifier = Modifier.padding(start = 24.dp),
                        style = MaterialTheme.typography.body1,
                    )
                }
            },
        )
    }
}