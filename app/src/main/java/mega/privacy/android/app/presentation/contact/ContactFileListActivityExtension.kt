package mega.privacy.android.app.presentation.contact

import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava

/**
 * Collect flows
 *
 */
fun ContactFileListActivity.collectFlows() {
    collectFlow(viewModel.state) { uiState ->
        val result = uiState.moveRequestResult
        if (result != null) {
            showMovementResult(result, result.nodes.first())
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                moveRequestMessageMapper.invoke(result),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )

            viewModel.markHandleMoveRequestResult()
        }
    }
}