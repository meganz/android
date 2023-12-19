package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.RetentionTimeUpdatedMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Retention time updated message view
 *
 * @param message The message
 * @param modifier Modifier
 * @param viewModel The view model
 */
@Composable
fun RetentionTimeUpdatedMessageView(
    message: RetentionTimeUpdatedMessage,
    modifier: Modifier = Modifier,
    viewModel: ManagementMessageViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    var ownerActionFullName by remember {
        mutableStateOf(context.getString(R.string.unknown_name_label))
    }

    LaunchedEffect(Unit) {
        if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        }?.let { ownerActionFullName = it }
    }

    RetentionTimeUpdatedMessageView(
        ownerActionFullName = ownerActionFullName,
        message.retentionTime,
        modifier = modifier
    )
}

/**
 * Retention time updated message view
 *
 * @param ownerActionFullName
 * @param modifier
 */
@Composable
fun RetentionTimeUpdatedMessageView(
    ownerActionFullName: String,
    newRetentionTime: Long,
    modifier: Modifier = Modifier,
) = ChatManagementMessageView(
    text = getRetentionTimeString(LocalContext.current, newRetentionTime)?.let {
        stringResource(id = R.string.retention_history_changed_by, ownerActionFullName, it)
    } ?: stringResource(id = R.string.retention_history_disabled, ownerActionFullName),
    modifier = modifier,
    styles = mapOf(
        SpanIndicator('A') to MegaSpanStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
            ),
            color = TextColor.Primary
        ),
        SpanIndicator('B') to MegaSpanStyle(
            SpanStyle(),
            color = TextColor.Secondary
        ),
    )
)

internal fun getRetentionTimeString(context: Context, timeInSeconds: Long) = when {
    timeInSeconds == 0L -> {
        null
    }

    timeInSeconds % SECONDS_IN_YEAR == 0L -> {
        context.getString(R.string.subtitle_properties_manage_chat_label_year)
    }

    timeInSeconds % SECONDS_IN_MONTH_30 == 0L -> {
        val numberOfMonths = (timeInSeconds / SECONDS_IN_MONTH_30).toInt()
        context.resources.getQuantityString(
            R.plurals.subtitle_properties_manage_chat_label_months, numberOfMonths, numberOfMonths
        )
    }

    timeInSeconds % SECONDS_IN_WEEK == 0L -> {
        val numberOfWeeks = (timeInSeconds / SECONDS_IN_WEEK).toInt()
        context.resources.getQuantityString(
            R.plurals.subtitle_properties_manage_chat_label_weeks, numberOfWeeks, numberOfWeeks
        )
    }

    timeInSeconds % SECONDS_IN_DAY == 0L -> {
        val numberOfDays = (timeInSeconds / SECONDS_IN_DAY).toInt()
        context.resources.getQuantityString(
            R.plurals.label_time_in_days_full, numberOfDays, numberOfDays
        )
    }

    timeInSeconds % SECONDS_IN_HOUR == 0L -> {
        val numberOfHours = (timeInSeconds / SECONDS_IN_HOUR).toInt()
        context.resources.getQuantityString(
            R.plurals.subtitle_properties_manage_chat_label_hours,
            numberOfHours,
            numberOfHours
        )
    }

    else -> null
}

@CombinedThemePreviews
@Composable
private fun RetentionTimeUpdatedMessagePreview(
    @PreviewParameter(RetentionTimeProvider::class) retentionTime: Long,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RetentionTimeUpdatedMessageView(
            ownerActionFullName = "Owner",
            newRetentionTime = retentionTime,
        )
    }
}

internal class RetentionTimeProvider : PreviewParameterProvider<Long> {
    override val values = listOf(
        0L,
        SECONDS_IN_HOUR,
        SECONDS_IN_DAY,
        SECONDS_IN_WEEK,
        SECONDS_IN_MONTH_30,
        SECONDS_IN_YEAR
    ).asSequence()
}

internal const val SECONDS_IN_MINUTE = 60L
internal const val SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60
internal const val SECONDS_IN_DAY = SECONDS_IN_HOUR * 24
internal const val SECONDS_IN_WEEK = SECONDS_IN_DAY * 7
internal const val SECONDS_IN_MONTH_30 = SECONDS_IN_DAY * 30
internal const val SECONDS_IN_YEAR = SECONDS_IN_DAY * 365