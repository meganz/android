package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.tokens.TextColor

@Composable
fun ChatObserverIndicator(
    numObservers: String,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier
        .testTag(TEST_TAG_OBSERVER_INDICATOR)
        .padding(8.dp),
    contentColor = MegaTheme.colors.background.surface3,
    shape = CircleShape,
    elevation = 3.dp
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag(TEST_TAG_OBSERVER_ICON),
            imageVector = ImageVector.vectorResource(R.drawable.ic_eye_medium_regular),
            contentDescription = numObservers,
            tint = MegaTheme.colors.text.primary
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        MegaText(
            modifier = modifier
                .testTag(TEST_TAG_OBSERVER_NUMBER),
            text = numObservers,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle2medium
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatObserverIndicatorPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatObserverIndicator(numObservers = "2")
    }
}

internal const val TEST_TAG_OBSERVER_INDICATOR = "chat_view:observer_indicator"
internal const val TEST_TAG_OBSERVER_ICON = "chat_view:observer_indicator:icon"
internal const val TEST_TAG_OBSERVER_NUMBER = "chat_view:observer_indicator:number"