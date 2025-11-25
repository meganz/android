package mega.privacy.mobile.home.presentation.home.widget.recents

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsWidgetUiState
import mega.privacy.mobile.home.presentation.home.widget.recents.view.RecentsListItemView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RecentsWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "RecentsWidgetProvider"
    override val defaultOrder: Int = 2
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.StringRes(R.string.section_recents)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        onNavigate: (NavKey) -> Unit,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<RecentsWidgetViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        RecentsView(
            uiState = uiState,
            modifier = modifier,
            onNavigate = onNavigate
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsView(
    uiState: RecentsWidgetUiState,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: Loading, empty states

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MegaText(
                stringResource(R.string.section_recents),
                style = AppTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                ),
                modifier = Modifier.testTag(TITLE_TEST_TAG)
            )
            // TODO Implement option button
        }

        val grouped = remember(uiState.recentActionItems) {
            uiState.recentActionItems.groupBy { it.timestampText.dateOnlyTimestamp }
        }

        grouped.forEach { (dateTimestamp, itemsForDate) ->
            RecentDateHeader(dateTimestamp)

            itemsForDate.forEach { item ->
                RecentsListItemView(
                    item = item,
                    onItemClicked = {
                        // TODO: Handle item click navigation
                    },
                    onMenuClicked = {
                        // TODO: Handle menu click
                    }
                )
            }
        }
    }
}

@Composable
fun RecentDateHeader(timestamp: Long) {
    MegaText(
        text = FormatRecentsDate(timestamp),
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(DATE_HEADER_TEST_TAG)
    )
}

/**
 * Format date from timestamp for header
 */
@Composable
fun FormatRecentsDate(timestamp: Long): String {
    val locale = Locale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val timestampInstant = Instant.ofEpochSecond(timestamp)
    val timestampDate = timestampInstant.atZone(zoneId).toLocalDate()
    val todayDate = LocalDate.now(zoneId)
    val yesterdayDate = todayDate.minusDays(1)

    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(
                locale,
                "EEEE, d MMM yyyy"
            )
        ).withLocale(locale)
    }

    return when (timestampDate) {
        todayDate -> {
            stringResource(R.string.label_today)
        }

        yesterdayDate -> {
            stringResource(R.string.label_yesterday)
        }

        else -> {
            timestampInstant.atZone(zoneId).format(dateTimeFormatter)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsViewPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsWidgetUiState(
                recentActionItems = listOf(
                    createMockRecentsUiItem(
                        title = RecentActionTitleText.MediaBucketImagesOnly(5),
                        parentFolderName = LocalizedText.Literal("Photos"),
                        timestamp = System.currentTimeMillis() / 1000 - 3600,
                        icon = IconPackR.drawable.ic_image_stack_medium_solid,
                        isMediaBucket = true,
                        shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
                    ),
                    createMockRecentsUiItem(
                        title = RecentActionTitleText.SingleNode("Document.pdf"),
                        parentFolderName = LocalizedText.Literal("Cloud Drive"),
                        timestamp = System.currentTimeMillis() / 1000 - 207200,
                        icon = IconPackR.drawable.ic_pdf_medium_solid,
                        isFavourite = true,
                        nodeLabel = NodeLabel.RED,
                    ),
                    createMockRecentsUiItem(
                        title = RecentActionTitleText.RegularBucket("Presentation.pptx", 3),
                        parentFolderName = LocalizedText.Literal("Work"),
                        timestamp = System.currentTimeMillis() / 1000 - 207200,
                        icon = IconPackR.drawable.ic_generic_medium_solid,
                        isUpdate = true,
                        updatedByText = LocalizedText.StringRes(
                            mega.privacy.android.feature.home.R.string.update_action_bucket,
                            listOf("John Doe")
                        ),
                        userName = "John Doe",
                    ),
                ),
                isLoading = false,
            ),
            onNavigate = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsViewEmptyPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsWidgetUiState(
                recentActionItems = emptyList(),
                isLoading = false,
            ),
            onNavigate = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsViewLoadingPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsWidgetUiState(
                recentActionItems = emptyList(),
                isLoading = true,
            ),
            onNavigate = {}
        )
    }
}

private fun createMockRecentsUiItem(
    title: RecentActionTitleText,
    parentFolderName: LocalizedText,
    timestamp: Long,
    icon: Int = IconPackR.drawable.ic_generic_medium_solid,
    shareIcon: Int? = null,
    isMediaBucket: Boolean = false,
    isUpdate: Boolean = false,
    updatedByText: LocalizedText? = null,
    userName: String? = null,
    isFavourite: Boolean = false,
    nodeLabel: NodeLabel? = null,
): RecentsUiItem {
    val mockBucket = RecentActionBucket(
        timestamp = timestamp,
        userEmail = "test@example.com",
        parentNodeId = NodeId(1L),
        isUpdate = isUpdate,
        isMedia = isMediaBucket,
        nodes = emptyList(),
    )
    return RecentsUiItem(
        title = title,
        icon = icon,
        shareIcon = shareIcon,
        parentFolderName = parentFolderName,
        timestampText = RecentsTimestampText(timestamp),
        isMediaBucket = isMediaBucket,
        isUpdate = isUpdate,
        updatedByText = updatedByText,
        userName = userName,
        isFavourite = isFavourite,
        nodeLabel = nodeLabel,
        bucket = mockBucket,
    )
}

internal const val TITLE_TEST_TAG = "recents_widget:title"
internal const val DATE_HEADER_TEST_TAG = "recents_widget:date_header"