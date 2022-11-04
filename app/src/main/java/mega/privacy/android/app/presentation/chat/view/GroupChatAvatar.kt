package mega.privacy.android.app.presentation.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR

@Composable
fun GroupChatAvatar(
    modifier: Modifier = Modifier
        .padding(16.dp)
        .size(40.dp)
        .clip(CircleShape),
    firstUser: ContactGroupUser?,
    lastUser: ContactGroupUser?,
    titleChat: String,
) {
    if (firstUser == null && lastUser == null) {
        Box(contentAlignment = Alignment.Center,
            modifier = modifier
                .background(color = Color(AvatarUtil.getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR)),
                    shape = CircleShape)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val currentHeight = placeable.height
                    var heightCircle = currentHeight
                    if (placeable.width > heightCircle)
                        heightCircle = placeable.width

                    layout(heightCircle, heightCircle) {
                        placeable.placeRelative(0, (heightCircle - currentHeight) / 2)
                    }
                }) {
            Text(
                text = AvatarUtil.getFirstLetter(titleChat),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
        }
    }
    /* DefaultGroupChatAvatar(modifier = modifier,
         color = Color(contactItem.defaultAvatarColor.toColorInt()),
         content = contactItem.getAvatarFirstLetter())*/
}