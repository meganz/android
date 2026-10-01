package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsMeetingPermissionsEducationShownUseCaseTest {
    private val permissionRepository: PermissionRepository = mock()

    private val underTest = IsMeetingPermissionsEducationShownUseCase(permissionRepository)

    @BeforeEach
    fun resetMocks() {
        reset(permissionRepository)
    }

    @ParameterizedTest(name = "expected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke returns the repository value`(expected: Boolean) = runTest {
        whenever(permissionRepository.isMeetingPermissionsEducationShown()).thenReturn(expected)

        assertThat(underTest()).isEqualTo(expected)
    }
}
