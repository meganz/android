package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Top app bar for file info screen
 * @param count total amount of selected contacts
 * @param onActionClick event for menu action click
 */
@Composable
internal fun FileInfoSelectActionModeTopBar(
    count: Int,
    onActionClick: (FileInfoMenuAction) -> Unit,
) {
    val tintColor = MaterialTheme.colors.secondary
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = systemUiController.statusBarDarkContentEnabled
    )
    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(R.plurals.general_selection_num_contacts, count, count),
                style = MaterialTheme.typography.subtitle1.copy(color = tintColor),
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                onActionClick(FileInfoMenuAction.SelectionModeAction.ClearSelection)
            }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = tintColor,
                )
            }
        },
        actions = {
            FileInfoMenuActions(
                actions = FileInfoMenuAction.SelectionModeAction.all(),
                onActionClick = onActionClick,
                tint = tintColor,
            )
        },
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    )
}


@CombinedTextAndThemePreviews
@Composable
private fun FileInfoSelectActionModeTopBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoSelectActionModeTopBar(
            count = 2,
            onActionClick = {},
        )
    }
}