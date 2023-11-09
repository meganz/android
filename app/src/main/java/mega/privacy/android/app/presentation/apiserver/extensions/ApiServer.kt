package mega.privacy.android.app.presentation.apiserver.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.apiserver.ApiServer

/**
 * Gets [ApiServer] text id.
 *
 * @return [ApiServer] text id.
 */
fun ApiServer.getTextId(): Int = when (this) {
    ApiServer.Production -> R.string.production_api_server
    ApiServer.Staging -> R.string.staging_api_server
    ApiServer.Staging444 -> R.string.staging444_api_server
    ApiServer.Sandbox3 -> R.string.sandbox3_api_server
}