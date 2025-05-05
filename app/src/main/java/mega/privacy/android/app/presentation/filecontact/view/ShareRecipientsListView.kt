package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.presentation.filecontact.model.ShareRecipientPreviewParameterProvider
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.icon.pack.R as IconPackR

@Composable
internal fun ShareRecipientsListView(
    items: ImmutableList<ShareRecipient>,
    onRecipientClick: (ShareRecipient) -> Unit,
    onRecipientLongClick: (ShareRecipient) -> Unit,
    onOptionsClick: (ShareRecipient) -> Unit,
    selectedItems: List<ShareRecipient>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items) { recipient ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShareRecipientView(
                    shareRecipient = recipient,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .combinedClickable(
                            onClick = {
                                onRecipientClick(recipient)
                            },
                            onLongClick = {
                                onRecipientLongClick(recipient)
                            }
                        )
                        .testTag(SHARE_RECIPIENT_LIST_ITEM),
                    selected = selectedItems.contains(recipient),
                )
                if (selectedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onOptionsClick(recipient) }
                            .testTag(SHARE_RECIPIENT_LIST_ITEM_OPTIONS),
                        contentAlignment = Alignment.Center
                    ) {
                        MegaIcon(
                            painter = painterResource(id = IconPackR.drawable.ic_more_vertical_medium_regular_outline),
                            contentDescription = "More",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            }
            SubtleDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

//Currently does not display a preview as the android view does not support preview. We will replace this with a compose version in the future.
@CombinedThemePreviews
@Composable
private fun ShareRecipientsListViewPreview(@PreviewParameter(ShareRecipientPreviewParameterProvider::class) recipients: List<ShareRecipient>) {
    AndroidThemeForPreviews {
        var selectedItems by remember { mutableStateOf(emptyList<ShareRecipient>()) }
        val onClick = { recipient: ShareRecipient ->
            selectedItems = if (selectedItems.contains(recipient)) {
                selectedItems - recipient
            } else {
                selectedItems + recipient
            }
        }
        ShareRecipientsListView(
            items = recipients.toImmutableList(),
            onRecipientClick = onClick,
            onRecipientLongClick = onClick,
            onOptionsClick = {},
            selectedItems = selectedItems,
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal const val SHARE_RECIPIENT_LIST_ITEM = "share_recipient_list:item"
internal const val SHARE_RECIPIENT_LIST_ITEM_OPTIONS = "share_recipient_list:item_options"