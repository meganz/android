package mega.privacy.android.legacy.core.ui.controls.appbar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Top app bar with title and required subtitle.
 *
 * Because native TopAppBar doesn't support subtitle, we have to create our own.
 */
@Composable
@Deprecated(
    message = "This component doesn't follow our design system correctly",
    replaceWith = ReplaceWith("MegaAppBar")
)
fun SimpleTopAppBarWithSubtitle(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    elevation: Boolean,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colors.surface
    AppBar(
        backgroundColor,
        contentColorFor(backgroundColor),
        if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        AppBarDefaults.ContentPadding,
        RectangleShape,
        modifier
    ) {
        Row(TitleIconModifier, verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(
                LocalContentAlpha provides ContentAlpha.high,
                content = {
                    IconButton(onClick = onBackPressed, enabled = isEnabled) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back button",
                            tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                        )
                    }
                }
            )
        }
        Row(
            Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.h6) {
                CompositionLocalProvider(
                    LocalContentAlpha provides ContentAlpha.high,
                    content = {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.textColorSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                )
            }

        }
    }
}

/*
    androidx.compose.material.AppBar is private, so I had to copy it here
 */
@Composable
private fun AppBar(
    backgroundColor: Color,
    contentColor: Color,
    elevation: Dp,
    contentPadding: PaddingValues,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        shape = shape,
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .height(AppBarHeight),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/*
    Default values which are private in androidx.compose.material.AppBar
 */

private val AppBarHorizontalPadding = 4.dp
private val AppBarHeight = 56.dp

// Start inset for the title when there is a navigation icon provided
private val TitleIconModifier = Modifier
    .fillMaxHeight()
    .width(72.dp - AppBarHorizontalPadding)

@CombinedThemePreviews
@Composable
private fun PreviewSimpleTopAppBar() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SimpleTopAppBarWithSubtitle(
            title = "Sync",
            subtitle = "Choose folders",
            elevation = false,
            onBackPressed = {}
        )
    }
}


