package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultMonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultMonitorNodeUpdatesTest {

    private lateinit var underTest: MonitorNodeUpdates

    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorNodeUpdates(filesRepository)
    }

    @Test
    fun `test that node update from repository is passed to the use case`() = runTest {
        val list = listOf<MegaNode>(mock())
        whenever(filesRepository.monitorNodeUpdates()).thenReturn(flowOf(list))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(list)
            awaitComplete()
        }
    }
}