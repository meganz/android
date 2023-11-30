package mega.privacy.android.app.presentation.apiserver.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.apiserver.ApiServerViewModel
import mega.privacy.android.app.presentation.apiserver.extensions.getTextId
import mega.privacy.android.app.presentation.apiserver.model.ApiServerUIState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.apiserver.ApiServer

/**
 * Change api server dialog wit radio buttons for each available api server.
 *
 */
@Composable
internal fun ChangeApiServerDialog(
    viewModel: ApiServerViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    ChangeApiServerDialog(
        uiState = uiState,
        onOptionSelected = viewModel::updateNewApiServer,
        onDismissRequest = {
            uiState.currentApiServer?.let { viewModel.updateNewApiServer(it) }
            onDismissRequest()
        },
        onConfirmRequest = {
            viewModel.confirmUpdateApiServer()
            onDismissRequest()
        },
    )
}

@Composable
internal fun ChangeApiServerDialog(
    uiState: ApiServerUIState,
    onOptionSelected: (ApiServer) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (ApiServer) -> Unit = {},
) = ConfirmationDialogWithRadioButtons(
    radioOptions = ApiServer.values().toList(),
    onOptionSelected = onOptionSelected,
    onDismissRequest = onDismissRequest,
    cancelButtonText = stringResource(id = R.string.general_cancel),
    confirmButtonText = stringResource(id = R.string.general_ok),
    onConfirmRequest = onConfirmRequest,
    titleText = stringResource(id = R.string.title_change_server),
    subTitleText = stringResource(id = R.string.staging_api_url_text),
    initialSelectedOption = uiState.newApiServer ?: uiState.currentApiServer,
    optionDescriptionMapper = { stringResource(id = it.getTextId()) }
)

@CombinedThemePreviews
@Composable
private fun ChangeApiServerDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChangeApiServerDialog(onDismissRequest = {})
    }
}