package mega.privacy.android.domain.usecase.permisison

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [HasNotificationPermissionUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasNotificationPermissionUseCaseTest {

    private lateinit var underTest: HasNotificationPermissionUseCase

    private val permissionRepository = mock<PermissionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HasNotificationPermissionUseCase(
            permissionRepository = permissionRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(permissionRepository)
    }

    @ParameterizedTest(name = "hasPermission: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that use case returns repository result`(hasPermission: Boolean) {
        whenever(permissionRepository.hasNotificationPermission()).thenReturn(hasPermission)

        val result = underTest()

        assertThat(result).isEqualTo(hasPermission)
    }

    @Test
    fun `test that use case calls repository hasNotificationPermission`() {
        whenever(permissionRepository.hasNotificationPermission()).thenReturn(true)

        underTest()

        verify(permissionRepository).hasNotificationPermission()
    }
}

