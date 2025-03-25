package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.settings.compose.home.model.MyAccountSettingsState
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.ContactAvatar.Companion.invoke
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect

internal fun LazyListScope.myAccountSettingsEntryView(
    data: MyAccountSettingsState,
) {

    item {
        Row {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        ContactAvatar(id = data.userId)
                    )
                    .transformations(CircleCropTransformation())
                    .memoryCacheKey("${data.userId.id}_0}")
                    .build(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Inside,
            )
            Column {
                MegaText(text = data.name, textColor = TextColor.Primary)
                MegaText(text = data.email, textColor = TextColor.Secondary)
            }
        }
    }
}

internal fun LazyListScope.myAccountSettingsEntryLoadingView() {
    item { ListItemLoadingSkeleton() }
}

/**
 * Placeholder while we add controls to new shared ui components
 */
@Composable
private fun ListItemLoadingSkeleton() {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .clip(shape = RoundedCornerShape(10.dp))
                .shimmerEffect()
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth()
        ) {
            Spacer(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(16.dp)
                    .width(120.dp)
                    .shimmerEffect()
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(16.dp)
                    .width(183.dp)
                    .shimmerEffect()
            )
        }
    }
}


internal fun settingsHeaderTag(key: String) = "settings_list_view:header_item_$key"
internal fun settingsItemTag(key: String) = "settings_list_view:settingItem_item_$key"