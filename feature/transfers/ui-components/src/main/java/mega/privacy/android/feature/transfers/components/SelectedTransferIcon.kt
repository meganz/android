package mega.privacy.android.feature.transfers.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.R

@Composable
fun SelectedTransferIcon() =
    MegaIcon(
        painterResource(id = R.drawable.ic_check_square_medium_thin_solid),
        tint = IconColor.Primary,
        modifier = Modifier
            .size(32.dp)
            .testTag(TEST_TAG_TRANSFER_SELECTED)
    )

/**
 * Tag for selected transfer check icon.
 */
internal const val TEST_TAG_TRANSFER_SELECTED = "$TEST_TAG_ACTIVE_TAB:selected_icon"