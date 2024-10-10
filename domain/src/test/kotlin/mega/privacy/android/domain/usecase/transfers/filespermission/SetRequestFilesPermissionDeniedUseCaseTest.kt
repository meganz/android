package mega.privacy.android.domain.usecase.transfers.filespermission

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetRequestFilesPermissionDeniedUseCaseTest {

    private lateinit var underTest: SetRequestFilesPermissionDeniedUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetRequestFilesPermissionDeniedUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that setRequestFilesPermissionDenied is invoked in repository when this is invoked`() =
        runTest {
            underTest.invoke()

            verify(transfersRepository).setRequestFilesPermissionDenied()
        }
}