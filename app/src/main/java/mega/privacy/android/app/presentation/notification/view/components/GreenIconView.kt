package mega.privacy.android.app.presentation.notification.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun GreenIconView(
    greenIconLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(12.dp)
            )

    ) {
        MegaText(
            text = stringResource(greenIconLabelRes),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag(GREEN_ICON_VIEW_TEST_TAG),
            textColor = TextColor.Inverse,
            style = MaterialTheme.typography.body4,
            overflow = LongTextBehaviour.Visible()
        )
    }
}

internal const val GREEN_ICON_VIEW_TEST_TAG = "green_icon_view:text_label"

@Composable
@Preview
private fun GreenIconViewPreview(
    @PreviewParameter(BooleanProvider::class) booleanParameter: Boolean,
) {
    OriginalTempTheme(isDark = booleanParameter) {
        GreenIconView(greenIconLabelRes = R.string.notifications_screen_notification_label_promo)
    }
}