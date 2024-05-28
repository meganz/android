package mega.privacy.android.data.mapper.featureflag

import mega.privacy.android.domain.entity.featureflag.FlagTypes
import nz.mega.sdk.MegaFlag
import javax.inject.Inject

/**
 * Mapper to convert type to [FlagTypes]
 */
internal class FlagTypesMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param flagTypes input term code
     * @return [FlagTypes]
     */
    operator fun invoke(flagTypes: Int): FlagTypes = when (flagTypes) {
        MegaFlag.FLAG_TYPE_FEATURE -> FlagTypes.Feature
        MegaFlag.FLAG_TYPE_AB_TEST -> FlagTypes.ABTest
        else -> FlagTypes.Invalid
    }
}