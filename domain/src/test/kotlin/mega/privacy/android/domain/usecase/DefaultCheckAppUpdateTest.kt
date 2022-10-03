package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FirstVersionResult
import mega.privacy.android.domain.entity.NewVersionResult
import mega.privacy.android.domain.entity.SameVersionResult
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class DefaultCheckAppUpdateTest {
    lateinit var underTest: CheckAppUpdate

    private val environmentRepository = mock<EnvironmentRepository>()

    @Before
    fun setUp() {
        underTest = DefaultCheckAppUpdate(
            repository = environmentRepository
        )
    }

    @Test
    fun `when last app version code is zero then return FirstVersionResult`() = runTest {
        val installedVersionCode = 10
        whenever(environmentRepository.getLastSavedVersionCode()).thenReturn(0)
        whenever(environmentRepository.getInstalledVersionCode()).thenReturn(installedVersionCode)
        assertEquals(underTest(), FirstVersionResult(installedVersionCode))
    }

    @Test
    fun `when last app version code equal current app version code then return SameVersionResult`() =
        runTest {
            val installedVersionCode = 10
            whenever(environmentRepository.getLastSavedVersionCode())
                .thenReturn(installedVersionCode)
            whenever(environmentRepository.getInstalledVersionCode())
                .thenReturn(installedVersionCode)
            assertEquals(underTest(), SameVersionResult(installedVersionCode))
        }

    @Test
    fun `when last app version code smaller than current app version code then return NewVersionResult`() =
        runTest {
            val lastSavedVersionCode = 9
            val installedVersionCode = 10
            whenever(environmentRepository.getLastSavedVersionCode())
                .thenReturn(lastSavedVersionCode)
            whenever(environmentRepository.getInstalledVersionCode())
                .thenReturn(installedVersionCode)
            assertEquals(underTest(), NewVersionResult(lastSavedVersionCode, installedVersionCode))
        }
}