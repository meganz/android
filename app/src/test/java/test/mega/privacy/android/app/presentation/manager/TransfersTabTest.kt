package test.mega.privacy.android.app.presentation.manager

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import org.junit.Test

class TransfersTabTest {

    @Test
    fun `test that pending tab returns position 0`() {
        Truth.assertThat(TransfersTab.PENDING_TAB.position).isEqualTo(0)
    }

    @Test
    fun `test that completed tab returns position 1`() {
        Truth.assertThat(TransfersTab.COMPLETED_TAB.position).isEqualTo(1)
    }

    @Test
    fun `test that position 0 is associated to pending tab`() {
        Truth.assertThat(TransfersTab.fromPosition(0)).isEqualTo(TransfersTab.PENDING_TAB)
    }

    @Test
    fun `test that position 1 is associated to completed tab`() {
        Truth.assertThat(TransfersTab.fromPosition(1)).isEqualTo(TransfersTab.COMPLETED_TAB)
    }

}