package mega.privacy.android.feature.clouddrive.navigation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineNavItemTest {
    private val underTest = OfflineNavItem()

    @Test
    fun `test that id is stable`() {
        assertThat(underTest.id).isEqualTo("offline")
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
    fun `test that destination is the offline nav key`() {
        assertThat(underTest.destination).isEqualTo(OfflineNavKey())
    }

    @Test
    fun `test that item is available offline`() {
        assertThat(underTest.availableOffline).isTrue()
    }

    @Test
    fun `test that badge is null`() {
        assertThat(underTest.badge).isNull()
    }

    @Test
    fun `test that label is the offline files section string`() {
        assertThat(underTest.label).isEqualTo(sharedR.string.general_section_offline_files)
    }

    @Test
    fun `test that drawer analytics event identifier defaults to the bottom navigation event`() {
        assertThat(underTest.drawerAnalyticsEventIdentifier)
            .isEqualTo(underTest.analyticsEventIdentifier)
    }
}
