package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.FolderVersionInfo
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetFolderVersionInfo
import mega.privacy.android.app.domain.usecase.GetFolderVersionInfo

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetFolderVersionInfoTest {
    private lateinit var underTest: GetFolderVersionInfo
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFolderVersionInfo(filesRepository = filesRepository)
    }

    @Test
    fun `test that negative version counts return null`() = runTest {
        whenever(filesRepository.getRootFolderVersionInfo()).thenReturn(FolderVersionInfo(-1, 20))
        assertThat(underTest()).isNull()
    }

    @Test
    fun `test that 0 versions return a value`() = runTest {
        val expected = FolderVersionInfo(0, 20)
        whenever(filesRepository.getRootFolderVersionInfo()).thenReturn(expected)
        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    fun `test that a positive number of versions returns a value`() = runTest {
        val expected = FolderVersionInfo(8, 20)
        whenever(filesRepository.getRootFolderVersionInfo()).thenReturn(expected)
        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    fun `test that returns null when error from api is thrown`() = runTest {
        whenever(filesRepository.getRootFolderVersionInfo()).thenThrow(MegaException(null, null))
        assertThat(underTest()).isEqualTo(null)
    }

}