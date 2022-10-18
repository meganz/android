package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultIsPendingShare
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.IsPendingShare
import mega.privacy.android.data.repository.FilesRepository
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultIsPendingShareTest {

    private lateinit var underTest: IsPendingShare
    private val getNodeByHandle = mock<GetNodeByHandle> {
        onBlocking { invoke(any()) }.thenReturn(null)
    }
    private val filesRepository = mock<FilesRepository> {
        onBlocking { isPendingShare(any()) }.thenReturn(false)
    }

    @Before
    fun setUp() {
        underTest = DefaultIsPendingShare(
            getNodeByHandle,
            filesRepository,
        )
    }

    @Test
    fun `test that if handle is -1L then return false`() = runTest {
        Truth.assertThat(underTest(-1L)).isFalse()
    }

    @Test
    fun `test that get node by handle use case is called`() = runTest {
        val handle = 123L
        underTest(handle)
        verify(getNodeByHandle).invoke(handle)
    }

    @Test
    fun `test that is pending share files repository is called with result of get node by handle use case`() =
        runTest {
            val handle = 123L
            val node = mock<MegaNode> {
                on { this.handle }.thenReturn(handle)
            }
            whenever(getNodeByHandle(handle)).thenReturn(node)

            underTest(handle)
            verify(filesRepository).isPendingShare(node)
        }

    @Test
    fun `test that is get node by handle is null then return false`() =
        runTest {
            val handle = 123L
            whenever(getNodeByHandle(handle)).thenReturn(null)
            Truth.assertThat(underTest(handle)).isFalse()
        }

    @Test
    fun `test that if handle is not -1L and get node by handle returns a node, then return result of is pending share`() =
        runTest {
            val handle = 123L
            val node = mock<MegaNode> {
                on { this.handle }.thenReturn(handle)
            }
            whenever(getNodeByHandle(handle)).thenReturn(node)

            val expected = true
            whenever(filesRepository.isPendingShare(node)).thenReturn(expected)
            Truth.assertThat(underTest(handle)).isEqualTo(expected)
        }
}