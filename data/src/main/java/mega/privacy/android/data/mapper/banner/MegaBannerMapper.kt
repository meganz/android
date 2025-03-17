package mega.privacy.android.data.mapper.banner

import mega.privacy.android.domain.entity.banner.Banner
import nz.mega.sdk.MegaBannerList
import javax.inject.Inject

internal class MegaBannerMapper @Inject constructor() {
    /**
     * Maps a MegaBannerList to a List of Banner domain models
     *
     * @param megaBannerList The MegaBannerList to be mapped
     * @return List of Banner domain models
     */
    operator fun invoke(megaBannerList: MegaBannerList): List<Banner> {
        return (0 until megaBannerList.size()).map { i ->
            val megaBanner = megaBannerList.get(i)
            Banner(
                id = megaBanner.id,
                title = megaBanner.title.orEmpty(),
                description = megaBanner.description.orEmpty(),
                image = megaBanner.image.orEmpty(),
                backgroundImage = megaBanner.backgroundImage.orEmpty(),
                url = megaBanner.url.orEmpty(),
                imageLocation = megaBanner.imageLocation.orEmpty()
            )
        }
    }
}