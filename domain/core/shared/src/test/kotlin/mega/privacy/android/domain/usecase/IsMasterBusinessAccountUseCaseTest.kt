package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BusinessRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test

class IsMasterBusinessAccountUseCaseTest {

    private val businessRepository = mock<BusinessRepository>()
    private val useCase = IsMasterBusinessAccountUseCase(businessRepository)

    @Test
    fun `test that invoke calls isMasterBusinessAccount from repository`() = runTest {
        useCase()
        verify(businessRepository).isMasterBusinessAccount()
    }
}