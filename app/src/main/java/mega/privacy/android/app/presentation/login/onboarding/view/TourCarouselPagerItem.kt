package mega.privacy.android.app.presentation.login.onboarding.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.icon.pack.R as iconPackR

sealed class TourCarouselPagerItem(
    @DrawableRes open val image: Int,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
) {
    data object First : TourCarouselPagerItem(
        iconPackR.drawable.ic_usp_1,
        sharedR.string.carousel_page_title_slide_1,
        sharedR.string.carousel_page_description_slide_1
    )

    data object Second : TourCarouselPagerItem(
        iconPackR.drawable.ic_usp_2,
        sharedR.string.carousel_page_title_slide_2,
        sharedR.string.carousel_page_description_slide_2
    )

    data object Third : TourCarouselPagerItem(
        iconPackR.drawable.ic_usp_3,
        sharedR.string.carousel_page_title_slide_3,
        sharedR.string.carousel_page_description_slide_3
    )

    data object Fourth: TourCarouselPagerItem(
        iconPackR.drawable.ic_usp_4,
        sharedR.string.carousel_page_title_slide_4,
        sharedR.string.carousel_page_description_slide_4
    )
}