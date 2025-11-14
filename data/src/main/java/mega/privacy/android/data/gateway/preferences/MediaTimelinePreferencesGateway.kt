package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

internal interface MediaTimelinePreferencesGateway {

    val cameraUploadShownFlow: Flow<Boolean>

    suspend fun setCameraUploadShown()
}
