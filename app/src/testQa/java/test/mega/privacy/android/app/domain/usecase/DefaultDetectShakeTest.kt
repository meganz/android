package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.usecase.DefaultDetectShake
import mega.privacy.android.app.domain.model.ShakeEvent
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.usecase.DetectShake
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDetectShakeTest {

    private lateinit var underTest: DetectShake
    private val repository = mock<ShakeDetectorRepository>()

    @Before
    fun setUp() {
        underTest = DefaultDetectShake(repository)
    }

    @Test
    fun `test that operator function gets invoked and filters shake event`() {
        underTest()
        verify(repository).monitorShakeEvents()
        val shakeEventFlow = flowOf(ShakeEvent(11.1F, 12.2F, 13.3F))
        whenever(repository.monitorShakeEvents()).thenReturn(shakeEventFlow)
        runTest {
            underTest().collect {
                assertEquals(11.1F, it.x)
                assertEquals(12.2F, it.y)
                assertEquals(13.3F, it.z)
            }
        }
    }
}