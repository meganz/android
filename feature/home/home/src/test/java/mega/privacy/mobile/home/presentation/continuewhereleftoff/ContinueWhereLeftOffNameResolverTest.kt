package mega.privacy.mobile.home.presentation.continuewhereleftoff

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

class ContinueWhereLeftOffNameResolverTest {

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()
    private lateinit var underTest: ContinueWhereLeftOffNameResolver

    @BeforeEach
    fun setUp() {
        underTest = ContinueWhereLeftOffNameResolver(getNodeByIdUseCase, durationInSecondsTextMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(getNodeByIdUseCase)
    }

    @Test
    fun `test that blank title is resolved from node`() = runTest {
        val items = listOf(item(1L, ""))
        stubNode(1L, "resolved.pdf")

        val result = underTest.resolveBlankNames(items)

        assertThat(result).isTrue()
        assertThat(underTest.applyCachedNames(items)[0].title).isEqualTo("resolved.pdf")
    }

    @Test
    fun `test that non-blank title is not resolved`() = runTest {
        val items = listOf(item(1L, "existing.pdf"))

        val result = underTest.resolveBlankNames(items)

        assertThat(result).isFalse()
        assertThat(underTest.applyCachedNames(items)[0].title).isEqualTo("existing.pdf")
    }

    @Test
    fun `test that resolved name is cached on second call`() = runTest {
        val items = listOf(item(1L, ""))
        stubNode(1L, "resolved.pdf")

        underTest.resolveBlankNames(items)
        val secondResult = underTest.resolveBlankNames(items)

        assertThat(secondResult).isFalse()
    }

    @Test
    fun `test that partial failure returns true when at least one resolved`() = runTest {
        val items = listOf(item(1L, ""), item(2L, ""))
        stubNode(1L, "resolved.pdf")
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(null)

        val result = underTest.resolveBlankNames(items)

        assertThat(result).isTrue()
        val resolved = underTest.applyCachedNames(items)
        assertThat(resolved[0].title).isEqualTo("resolved.pdf")
        assertThat(resolved[1].title).isEmpty()
    }

    @Test
    fun `test that all failures returns false`() = runTest {
        val items = listOf(item(1L, ""), item(2L, ""))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(null)

        val result = underTest.resolveBlankNames(items)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that mixed blank and non-blank only resolves blank`() = runTest {
        val items = listOf(item(1L, "existing.pdf"), item(2L, ""))
        stubNode(2L, "resolved.mp4")

        underTest.resolveBlankNames(items)

        val resolved = underTest.applyCachedNames(items)
        assertThat(resolved[0].title).isEqualTo("existing.pdf")
        assertThat(resolved[1].title).isEqualTo("resolved.mp4")
    }

    @Test
    fun `test that duration is resolved for audio node`() = runTest {
        val items = listOf(item(1L, "", type = RecentlyUsedType.Audio))
        val node = mock<TypedFileNode> {
            on { name }.thenReturn("song.mp3")
            on { type }.thenReturn(AudioFileTypeInfo("audio/mpeg", "mp3", 94.seconds))
        }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)

        underTest.resolveBlankNames(items)

        val resolved = underTest.applyCachedNames(items)
        assertThat(resolved[0].duration).isEqualTo("1:34")
    }

    @Test
    fun `test that duration is null for non-media node`() = runTest {
        val items = listOf(item(1L, ""))
        val node = mock<TypedFileNode> {
            on { name }.thenReturn("doc.pdf")
            on { type }.thenReturn(PdfFileTypeInfo)
        }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)

        underTest.resolveBlankNames(items)

        val resolved = underTest.applyCachedNames(items)
        assertThat(resolved[0].duration).isNull()
    }

    private fun item(
        handle: Long,
        title: String,
        type: RecentlyUsedType = RecentlyUsedType.PDF,
    ) = ContinueWhereLeftOffItem(
        nodeHandle = handle,
        type = type,
        title = title,
        lastAccessedTimestamp = 1000L,
    )

    private suspend fun stubNode(handle: Long, name: String) {
        val node = mock<TypedFileNode> { on { this.name }.thenReturn(name) }
        whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(node)
    }
}
