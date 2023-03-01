package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.extensions.getValueFor
import nz.mega.sdk.MegaStringMap
import javax.inject.Inject

/**
 * Default implementation of [CameraUploadsHandlesMapper]
 */
internal class CameraUploadsHandlesMapperImpl @Inject constructor() : CameraUploadsHandlesMapper {
    override fun invoke(stringMap: MegaStringMap?): Pair<String?, String?> =
        Pair(stringMap.getValueFor("h"), stringMap.getValueFor("sh"))
}