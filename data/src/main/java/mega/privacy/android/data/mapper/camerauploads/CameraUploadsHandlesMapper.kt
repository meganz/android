package mega.privacy.android.data.mapper.camerauploads

import nz.mega.sdk.MegaStringMap

/**
 * Mapper that converts a [MegaStringMap] into a [String] [Pair]
 */
internal fun interface CameraUploadsHandlesMapper {

    /**
     * Invocation function
     *
     * @param stringMap The [MegaStringMap]
     *
     * @return an equivalent [String] [Pair]
     */
    operator fun invoke(stringMap: MegaStringMap?): Pair<String?, String?>
}