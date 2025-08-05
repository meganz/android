package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
fun CloudDriveEmptyView(
    modifier: Modifier = Modifier,
    onAddFilesClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(120.dp),
            painter = painterResource(iconPackR.drawable.ic_empty_folder_glass),
            contentDescription = "Empty",
        )

        Spacer(modifier = Modifier.height(24.dp))

        MegaText(
            text = "Empty folder", // TODO string resource
            style = AppTheme.typography.titleLarge,
            textColor = TextColor.Primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryFilledButton(
            text = "Add files", // TODO string resource
            modifier = Modifier,
            onClick = onAddFilesClick,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CloudDriveEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            onAddFilesClick = { }
        )
    }
}