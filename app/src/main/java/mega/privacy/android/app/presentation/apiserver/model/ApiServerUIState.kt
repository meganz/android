package mega.privacy.android.app.presentation.apiserver.model

import mega.privacy.android.domain.entity.apiserver.ApiServer

/**
 * Api server UI state
 *
 * @property currentApiServer [ApiServer]
 * @property newApiServer [ApiServer]
 */
data class ApiServerUIState(
    val currentApiServer: ApiServer? = null,
    val newApiServer: ApiServer? = null,
)
