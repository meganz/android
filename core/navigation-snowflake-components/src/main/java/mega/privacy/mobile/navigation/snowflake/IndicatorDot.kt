package mega.privacy.mobile.navigation.snowflake

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.R
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.ComponentsColor
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun IndicatorDot(
    modifier: Modifier = Modifier,
    size: Dp = 4.dp,
    color: ComponentsColor = ComponentsColor.Interactive,
    shape: Shape = CircleShape,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .padding(start = spacing.x2, bottom = spacing.x4)
            .size(size)
            .background(color.getComponentsColor(DSTokens.colors.components), shape = shape)
    )
}

@Composable
@CombinedThemePreviews
private fun IndicatorDotPreview() {
    AndroidThemeForPreviews {

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            for (color in ComponentsColor.entries) {
                Row {
//                    Small dot
                    BadgedBox(
                        badge = {
                            IndicatorDot(color = color, size = 2.dp)
                        }
                    ) {
                        MegaIcon(
                            painter = painterResource(id = R.drawable.ic_close_medium_thin_outline),
                            tint = IconColor.Primary
                        )
                    }
//                    Default dot
                    BadgedBox(
                        badge = {
                            IndicatorDot(color = color)
                        }
                    ) {
                        MegaIcon(
                            painter = painterResource(id = R.drawable.ic_close_medium_thin_outline),
                            tint = IconColor.Primary
                        )
                    }
//                      Square shape
                    BadgedBox(
                        badge = {
                            IndicatorDot(color = color, shape = RoundedCornerShape(1.dp))
                        }
                    ) {
                        MegaIcon(
                            painter = painterResource(id = R.drawable.ic_close_medium_thin_outline),
                            tint = IconColor.Primary
                        )
                    }
                }
            }
        }
    }
}