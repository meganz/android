package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

@Composable
internal fun ContactInfoContent(
    uiState: ContactInfoState,
    modifier: Modifier = Modifier,
    contentHeight: Dp = 0.dp,
) = Column(modifier = modifier.heightIn(contentHeight)) {
    InfoOptionsView(
        primaryDisplayName = uiState.primaryDisplayName,
        secondaryDisplayName = uiState.secondaryDisplayName,
        modifyNickNameTextId = uiState.modifyNickNameTextId,
        email = uiState.email,
    )
    Divider(color = MaterialTheme.colors.grey_alpha_012_white_alpha_012)
    ChatOptions()
    Divider(color = MaterialTheme.colors.grey_alpha_012_white_alpha_012)
}
