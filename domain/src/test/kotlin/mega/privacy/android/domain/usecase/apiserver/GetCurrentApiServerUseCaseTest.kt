package mega.privacy.android.domain.usecase.apiserver

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCurrentApiServerUseCaseTest {

    private lateinit var underTest: GetCurrentApiServerUseCase

    private val apiServerRepository = mock<ApiServerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetCurrentApiServerUseCase(apiServerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(apiServerRepository)
    }

    @ParameterizedTest(name = " if repository returns {0}")
    @EnumSource(ApiServer::class)
    fun `test that get current api server returns correctly`(
        apiServer: ApiServer,
    ) = runTest {
        whenever(apiServerRepository.getCurrentApi()).thenReturn(apiServer)
        Truth.assertThat(underTest.invoke()).isEqualTo(apiServer)
    }
}