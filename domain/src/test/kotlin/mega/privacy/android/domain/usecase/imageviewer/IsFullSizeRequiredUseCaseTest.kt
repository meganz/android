package mega.privacy.android.domain.usecase.imageviewer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IsFullSizeRequiredUseCaseTest {

    private val networkRepository = mock<NetworkRepository>()
    private val settingsRepository = mock<SettingsRepository>()
    private val node = mock<TypedFileNode> {
        on { id }.thenReturn(NodeId(1L))
    }
    private val fullSize = true

    val underTest = IsFullSizeRequiredUseCase(networkRepository, settingsRepository)

    @BeforeEach
    fun resetMock() {
        reset(networkRepository, settingsRepository, node)
    }


    @Test
    fun `test that when node is taken down it returns false`() = runTest {
        whenever(node.type).thenReturn(mock<VideoFileTypeInfo>())
        whenever(node.isTakenDown).thenReturn(true)
        val actual = underTest(node, fullSize)
        assertThat(actual).isEqualTo(false)
    }

    @Test
    fun `test that when file is video it returns false`() = runTest {
        whenever(node.type).thenReturn(mock<VideoFileTypeInfo>())
        whenever(node.isTakenDown).thenReturn(false)
        val actual = underTest(node, fullSize)
        assertThat(actual).isEqualTo(false)
    }

    @Test
    fun `test that when node size is less than 1MB it returns true`() = runTest {
        whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
        whenever(node.isTakenDown).thenReturn(false)
        whenever(node.size).thenReturn(1L)
        val actual = underTest(node, fullSize)
        assertThat(actual).isEqualTo(true)
    }

    @Test
    fun `test that when node size is between 1MB and 50 MB and fullSize is true then it returns true`() =
        runTest {
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_1_MB)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(true)
        }


    @Test
    fun `test that when node size is between 1MB and 50 MB and fullSize is false then if isMobileDataAllowed is true it returns true`() =
        runTest {
            val fullSize = false
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_1_MB)
            whenever(settingsRepository.isMobileDataAllowed()).thenReturn(true)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(true)
        }

    @Test
    fun `test that when node size is between 1MB and 50 MB, fullSize is false, isMobileDataAllowed is false then if isMeteredConnection is false it returns true`() =
        runTest {
            val fullSize = false
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_1_MB)
            whenever(settingsRepository.isMobileDataAllowed()).thenReturn(false)
            whenever(networkRepository.isMeteredConnection()).thenReturn(false)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(true)
        }

    @Test
    fun `test that when node size is between 1MB and 50 MB, fullSize is false, isMobileDataAllowed is false then if isMeteredConnection is true it returns false`() =
        runTest {
            val fullSize = false
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_1_MB)
            whenever(settingsRepository.isMobileDataAllowed()).thenReturn(false)
            whenever(networkRepository.isMeteredConnection()).thenReturn(true)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(false)
        }

    @Test
    fun `test that when node size is between 1MB and 50 MB, fullSize is false, isMobileDataAllowed is false, isMeteredConnection returns null it returns true`() =
        runTest {
            val fullSize = false
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_50_MB)
            whenever(settingsRepository.isMobileDataAllowed()).thenReturn(false)
            whenever(networkRepository.isMeteredConnection()).thenReturn(null)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(true)
        }

    @Test
    fun `test that when node size is larger than 50 MB it returns false`() =
        runTest {
            val fullSize = false
            whenever(node.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(node.isTakenDown).thenReturn(false)
            whenever(node.size).thenReturn(SIZE_50_MB + SIZE_1_MB)
            val actual = underTest(node, fullSize)
            assertThat(actual).isEqualTo(false)
        }

    companion object {
        private const val SIZE_1_MB = 1024 * 1024 * 1L
        private const val SIZE_50_MB = SIZE_1_MB * 50L
    }
}