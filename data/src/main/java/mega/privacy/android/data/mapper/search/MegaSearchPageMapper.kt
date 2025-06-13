package mega.privacy.android.data.mapper.search

import nz.mega.sdk.MegaSearchPage
import javax.inject.Inject

class MegaSearchPageMapper @Inject constructor() {
    operator fun invoke(offset: Long, limit: Long): MegaSearchPage =
        MegaSearchPage.createInstance(offset, limit)
}