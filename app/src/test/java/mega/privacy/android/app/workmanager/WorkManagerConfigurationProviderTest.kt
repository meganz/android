package mega.privacy.android.app.workmanager

import androidx.hilt.work.HiltWorkerFactory
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class WorkManagerConfigurationProviderTest {

    @Test
    fun `test that workManagerConfiguration is built with the hilt worker factory`() {
        val hiltWorkerFactory = mock<HiltWorkerFactory>()
        val underTest = WorkManagerConfigurationProvider(hiltWorkerFactory)

        val configuration = underTest.workManagerConfiguration

        assertThat(configuration.workerFactory).isSameInstanceAs(hiltWorkerFactory)
    }
}
