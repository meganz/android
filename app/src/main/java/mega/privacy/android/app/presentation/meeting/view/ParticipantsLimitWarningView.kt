package mega.privacy.android.app.presentation.meeting.view

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.content.withStyledAttributes
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.banners.WarningBanner
import mega.privacy.android.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Participants limit warning view
 */
class ParticipantsLimitWarningView : AbstractComposeView {

    /**
     * whether the user is a moderator
     */
    var isModerator by mutableStateOf(false)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        context.withStyledAttributes(attrs, R.styleable.WarningBanner) {
        }
    }

    @Composable
    override fun Content() {
        MegaAppTheme(isDark = isSystemInDarkTheme()) {
            ParticipantsLimitWarningComposeView(
                isModerator = isModerator,
            )
        }
    }
}

/**
 * Participants limit warning compose view
 */
@Composable
fun ParticipantsLimitWarningComposeView(
    isModerator: Boolean,
    modifier: Modifier = Modifier,
) {
    var isWarningDismissed by rememberSaveable {
        mutableStateOf(false)
    }

    if (isModerator) {
        if (isWarningDismissed.not()) {
            val context = LocalContext.current
            WarningBanner(
                modifier = modifier.testTag(TEST_TAG_PARTICIPANTS_LIMIT_WARNING_MODERATOR_VIEW),
                textComponent = {
                    MegaSpannedClickableText(
                        value = stringResource(id = mega.privacy.android.app.R.string.meetings_free_call_organiser_number_of_participants_warning),
                        styles = hashMapOf(
                            SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                                MegaSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.None),
                                ), ""
                            ),
                            SpanIndicator('B') to MegaSpanStyleWithAnnotation(
                                MegaSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.Underline),
                                ), "upgrade"
                            ),
                        ),
                        onAnnotationClick = { annotation ->
                            if (annotation == "upgrade") {
                                context.startActivity(
                                    Intent(
                                        context,
                                        UpgradeAccountActivity::class.java
                                    )
                                )
                            }
                        },
                        baseStyle = MaterialTheme.typography.caption,
                        color = TextColor.Primary
                    )
                },
                onCloseClick = { isWarningDismissed = true }
            )
        }
    } else {
        if (isWarningDismissed.not()) {
            WarningBanner(
                modifier = modifier.testTag(TEST_TAG_PARTICIPANTS_LIMIT_WARNING_VIEW),
                textString = stringResource(id = mega.privacy.android.app.R.string.meetings_free_call_organiser_number_of_participants_warning),
                onCloseClick = { isWarningDismissed = true }
            )
        }
    }
}

internal const val TEST_TAG_PARTICIPANTS_LIMIT_WARNING_MODERATOR_VIEW =
    "participants_limit_warning_moderator_view"
internal const val TEST_TAG_PARTICIPANTS_LIMIT_WARNING_VIEW = "participants_limit_warning_view"

@CombinedThemePreviews
@Composable
private fun ParticipantsLimitWarningComposeViewPreview(
    @PreviewParameter(BooleanProvider::class) isModerator: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ParticipantsLimitWarningComposeView(
            isModerator = isModerator,
        )
    }
}
