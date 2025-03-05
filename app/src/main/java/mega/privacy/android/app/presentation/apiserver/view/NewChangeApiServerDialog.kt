package mega.privacy.android.app.presentation.apiserver.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.settings.SettingsOptionsModal
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.apiserver.ApiServerViewModel
import mega.privacy.android.app.presentation.apiserver.extensions.getTextId
import mega.privacy.android.app.presentation.apiserver.view.navigation.openLoginActivity
import mega.privacy.android.domain.entity.apiserver.ApiServer

@Composable
internal fun NewChangeApiServerDialog(
    viewModel: ApiServerViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedItem = uiState.newApiServer ?: uiState.currentApiServer

    SettingsOptionsModal<ApiServer>(
        key = "server_dialog",
        content = {
            addHeader(
                stringResource(id = R.string.title_change_server),
                stringResource(id = R.string.staging_api_url_text)
            )

            ApiServer.entries.forEach {
                addItem(
                    isSelected = selectedItem == it,
                    value = it,
                    valueToString = { item -> context.getString(item.getTextId()) }
                )
            }
        },
        onDismiss = onDismissRequest,
    ) {
        viewModel.updateNewApiServer(it)
        viewModel.confirmUpdateApiServer()
        openLoginActivity(context)
    }
}