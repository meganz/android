package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_DISABLED
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_FALSE
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_TRUE
import javax.inject.Inject

/**
 * Mapper for Sensitivity Filter Option
 */
class SensitivityFilterOptionIntMapper @Inject constructor() {

    /**
     * Invoke
     * @param filterOption search target
     *
     * @return Int value of search target
     */
    operator fun invoke(filterOption: SensitivityFilterOption): Int = when (filterOption) {
        SensitivityFilterOption.Disabled -> BOOL_FILTER_DISABLED
        SensitivityFilterOption.SensitiveOnly -> BOOL_FILTER_ONLY_FALSE
        SensitivityFilterOption.NonSensitiveOnly -> BOOL_FILTER_ONLY_TRUE
    }
}