package mega.privacy.android.domain.usecase.permisison

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasCameraUploadsPermissionUseCaseTest {

    private lateinit var underTest: HasCameraUploadsPermissionUseCase

    private val permissionRepository: PermissionRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = HasCameraUploadsPermissionUseCase(
            permissionRepository = permissionRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(permissionRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct result is returned`(hasPermission: Boolean) {
        whenever(permissionRepository.hasCameraUploadsPermission()) doReturn hasPermission

        val actual = underTest()

        assertThat(actual).isEqualTo(hasPermission)
    }
}
