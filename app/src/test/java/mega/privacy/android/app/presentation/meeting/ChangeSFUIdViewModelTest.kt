package mega.privacy.android.app.presentation.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.ChangeSFUIdViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.meeting.SetSFUIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeSFUIdViewModelTest {
    private lateinit var underTest: ChangeSFUIdViewModel

    private val setSFUIdUseCase: SetSFUIdUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = ChangeSFUIdViewModel(setSFUIdUseCase)
    }

    @Test
    fun `test that new sfu id is set when changeSFUId is invoked`() =
        runTest {
            val sfuId = 123
            underTest.changeSFUId(sfuId)

            verify(setSFUIdUseCase).invoke(sfuId)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }

}
