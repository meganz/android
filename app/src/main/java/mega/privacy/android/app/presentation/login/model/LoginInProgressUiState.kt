package mega.privacy.android.app.presentation.login.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import timber.log.Timber

/**
 * UI state for the login in progress screen.
 * @param fetchNodesUpdate The current fetch nodes update, if available.
 * @param isFastLoginInProgress Indicates if a fast login is currently in progress.
 * @param loginTemporaryError Temporary error during login or fetch nodes, if any.
 * @param requestStatusProgress The current progress of the request status, if available.
 * @param snackbarMessage The message to show in the Snackbar, if any.
 * @param isFastLogin Indicates if it's fast login flow
 */
data class LoginInProgressUiState(
    val fetchNodesUpdate: FetchNodesUpdate? = null,
    val isFastLoginInProgress: Boolean = false,
    val loginTemporaryError: TemporaryWaitingError? = null,
    val requestStatusProgress: Progress? = null,
    val snackbarMessage: StateEventWithContent<Int> = consumed(),
    val isFastLogin: Boolean = false,
) {
    /**
     * True if the request status progress event is being processed
     */
    val isRequestStatusInProgress: Boolean
        get() = requestStatusProgress != null

    /**
     * Calculate the current progress of the login and fetch nodes
     * Weights:
     * - 50% for login
     * - 45% for updating files
     * - 5% for preparing files
     */
    val currentProgress: Float
        get() {
            val progressAfterLogin = if (!isFastLogin) 0.3f else 0.5f
            val progressAfterFetchNode = progressAfterLogin + if (!isFastLogin) 0.1f else 0.45f
            return when {
                isFastLoginInProgress -> progressAfterLogin
                fetchNodesUpdate?.progress != null -> {
                    Timber.d("CongHai fetchNodesUpdate?.progress  = ${fetchNodesUpdate.progress}")
                    val fetchNodeProgress = fetchNodesUpdate.progress?.floatValue ?: 0f
                    if (fetchNodeProgress > 0f)
                        progressAfterFetchNode + (fetchNodeProgress * (1.0f - progressAfterFetchNode))
                    else
                        progressAfterFetchNode
                }

                fetchNodesUpdate != null -> progressAfterFetchNode
                else -> progressAfterLogin
            }
        }
} 