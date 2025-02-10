package mega.privacy.android.app.presentation.offline.view


import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Composable for when offline page is empty
 * @param modifier [Modifier]
 */
@Composable
fun OfflineEmptyView(
    modifier: Modifier = Modifier,
) {

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (image, text) = createRefs()

        Image(
            painter = painterResource(IconPackR.drawable.ic_arrow_circle_down_glass),
            contentScale = ContentScale.FillBounds,
            contentDescription = "Download Icon",
            modifier = Modifier
                .constrainAs(image) {
                    bottom.linkTo(text.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(OFFLINE_EMPTY_IMAGE_TEST_TAG),
        )
        MegaSpannedText(
            value = stringResource(id = R.string.context_empty_offline),
            baseStyle = MaterialTheme.typography.subtitle2,
            styles = mapOf(
                SpanIndicator('A') to MegaSpanStyle(
                    spanStyle = SpanStyle(),
                ),
                SpanIndicator('B') to MegaSpanStyle(
                    spanStyle = SpanStyle(),
                ),
            ),
            color = TextColor.Primary,
            modifier = Modifier
                .constrainAs(text) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(OFFLINE_EMPTY_TEXT_TEST_TAG),
        )
    }
}

internal const val OFFLINE_EMPTY_IMAGE_TEST_TAG = "offline_empty_view:image"
internal const val OFFLINE_EMPTY_TEXT_TEST_TAG = "offline_empty_view:text"

@CombinedThemePreviews
@Composable
private fun OfflineEmptyViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OfflineEmptyView()
    }
}