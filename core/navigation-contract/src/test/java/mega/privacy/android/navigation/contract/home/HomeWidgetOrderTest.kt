package mega.privacy.android.navigation.contract.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HomeWidgetOrderTest {

    @Test
    fun `test that HomeWidgetOrder entries are declared in the expected default order`() {
        assertThat(HomeWidgetOrder.entries).containsExactly(
            HomeWidgetOrder.Shortcuts,
            HomeWidgetOrder.MyAccount,
            HomeWidgetOrder.Banner,
            HomeWidgetOrder.Recents,
            HomeWidgetOrder.ViewedLinks,
            HomeWidgetOrder.ContinueWhereLeftOff,
        ).inOrder()
    }
}
