package mega.privacy.android.data.mapper.featureflag

import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import nz.mega.sdk.MegaFlag
import javax.inject.Inject

/**
 * Flag mapper
 */
internal class FlagMapper @Inject constructor(private val flagTypesMapper: FlagTypesMapper) {

    operator fun invoke(megaFlag: MegaFlag): Flag = Flag(
        type = flagTypesMapper(megaFlag.type.toInt()),
        group = if (megaFlag.group > 0) GroupFlagTypes.Enabled else GroupFlagTypes.Disabled
    )
}