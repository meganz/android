package mega.privacy.android.feature.clouddrive.navigation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.destination.FavouritesNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MyMenuFavouritesNavigationItemEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouritesNavItemTest {
    private val underTest = FavouritesNavItem()

    @Test
    fun `test that id is stable`() {
        assertThat(underTest.id).isEqualTo("favourites")
    }

    @Test
    fun `test that preferred slot is None`() {
        assertThat(underTest.preferredSlot).isEqualTo(PreferredSlot.None)
    }

    @Test
    fun `test that item is flagged with the customisable bottom navigation feature`() {
        assertThat(underTest.feature).isEqualTo(ApiFeatures.CustomisableBottomNavigation)
    }

    @Test
    fun `test that destination is the favourites nav key`() {
        assertThat(underTest.destination).isEqualTo(FavouritesNavKey)
    }

    @Test
    fun `test that item is not available offline`() {
        assertThat(underTest.availableOffline).isFalse()
    }

    @Test
    fun `test that badge is null`() {
        assertThat(underTest.badge).isNull()
    }

    @Test
    fun `test that label is the favourites string`() {
        assertThat(underTest.label)
            .isEqualTo(sharedR.string.video_section_title_favourite_playlist)
    }

    @Test
    fun `test that drawer analytics event identifier is the favourites drawer event`() {
        assertThat(underTest.drawerAnalyticsEventIdentifier)
            .isEqualTo(MyMenuFavouritesNavigationItemEvent)
    }
}
