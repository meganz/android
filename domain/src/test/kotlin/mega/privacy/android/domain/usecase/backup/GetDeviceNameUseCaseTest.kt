package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetDeviceNameUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetDeviceNameUseCaseTest {

    private lateinit var underTest: GetDeviceNameUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDeviceNameUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @ParameterizedTest(name = "returns {0}")
    @NullAndEmptySource
    @ValueSource(strings = ["Samsung S23 Ultra"])
    fun `test that when invoked device name is returned`(deviceName: String?) = runTest {
        whenever(backupRepository.getDeviceName(any())).thenReturn(deviceName)
        val actual = underTest("abcd")
        Truth.assertThat(actual).isEqualTo(deviceName)
    }
}
