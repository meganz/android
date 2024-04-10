package mega.privacy.android.app.presentation.recentactions.view


import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Composable for when Recent Actions is hidden in settings
 *
 * @param modifier [Modifier]
 * @param onShowActivityActionClick Callback when the show activity action button is clicked
 */
@Composable
fun RecentActionsHiddenView(
    modifier: Modifier = Modifier,
    onShowActivityActionClick: () -> Unit = {},
) {

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (image, text, button) = createRefs()

        Image(
            painter = painterResource(R.drawable.ic_recents),
            contentDescription = "Recent Actions Icon",
            modifier = Modifier
                .constrainAs(image) {
                    bottom.linkTo(text.top, 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(RECENTS_HIDDEN_IMAGE_TEST_TAG),
        )
        MegaSpannedText(
            value = stringResource(id = R.string.recents_activity_hidden),
            baseStyle = MaterialTheme.typography.body2,
            styles = mapOf(
                SpanIndicator('B') to MegaSpanStyle(
                    spanStyle = SpanStyle(
                        fontWeight = FontWeight.Normal
                    ),
                    color = TextColor.Secondary
                ),
            ),
            color = TextColor.Primary,
            modifier = Modifier
                .constrainAs(text) {
                    bottom.linkTo(button.top, 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(RECENTS_HIDDEN_TEXT_TEST_TAG),
        )
        OutlinedMegaButton(
            textId = R.string.show_activity_action,
            rounded = false,
            onClick = { onShowActivityActionClick() },
            modifier = Modifier
                .testTag(RECENTS_HIDDEN_BUTTON_TEST_TAG)
                .constrainAs(button) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        )
    }
}

internal const val RECENTS_HIDDEN_IMAGE_TEST_TAG = "recent_actions_hidden_view:image"
internal const val RECENTS_HIDDEN_TEXT_TEST_TAG = "recent_actions_hidden_view:text"
internal const val RECENTS_HIDDEN_BUTTON_TEST_TAG = "recent_actions_hidden_view:button"

@CombinedThemePreviews
@Composable
private fun RecentActionsHiddenViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RecentActionsHiddenView()
    }
}