package test.mega.privacy.android.app.presentation.manager

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.manager.model.SharesTab
import org.junit.Test

class SharesTabTest {

    @Test
    fun `test that incoming tab returns position 0`() {
        assertThat(SharesTab.INCOMING_TAB.position).isEqualTo(0)
    }

    @Test
    fun `test that outgoing tab returns position 1`() {
        assertThat(SharesTab.OUTGOING_TAB.position).isEqualTo(1)
    }

    @Test
    fun `test that link tab returns position 2`() {
        assertThat(SharesTab.LINKS_TAB.position).isEqualTo(2)
    }

    @Test
    fun `test that position 0 is associated to incoming tab`() {
        assertThat(SharesTab.fromPosition(0)).isEqualTo(SharesTab.INCOMING_TAB)
    }

    @Test
    fun `test that position 1 is associated to outgoing tab`() {
        assertThat(SharesTab.fromPosition(1)).isEqualTo(SharesTab.OUTGOING_TAB)
    }

    @Test
    fun `test that position 2 is associated to links tab`() {
        assertThat(SharesTab.fromPosition(2)).isEqualTo(SharesTab.LINKS_TAB)
    }

}