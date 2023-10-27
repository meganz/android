package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.domain.entity.Offline
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineAvailabilityMapperTest {
    private lateinit var underTest: OfflineAvailabilityMapper
    private val fileGateway: FileGateway = mock()
    private val megaLocalRoomGateway: MegaLocalRoomGateway = mock()

    private val expectedName = "testName"
    private val expectedSize = 1000L
    private val expectedLabel = MegaNode.NODE_LBL_RED
    private val expectedId = 1L
    private val expectedParentId = 2L
    private val expectedBase64Id = "1L"
    private val expectedModificationTime = 123L
    private val expectedFingerprint = "fingerprint"
    private val expectedOriginalFingerprint = "expectedOriginalFingerprint"
    private val expectedDuration = 100
    private val expectedPublicLink = "publicLink"
    private val expectedPublicLinkCreationTime = 456L
    private val expectedSerializedString = "serializedString"

    @BeforeAll
    fun setUp() {
        underTest = OfflineAvailabilityMapper(
            ioDispatcher = UnconfinedTestDispatcher(),
            fileGateway = fileGateway,
            megaLocalRoomGateway = megaLocalRoomGateway
        )
    }

    @ParameterizedTest(name = "Test file Node ")
    @MethodSource("provideParameters")
    fun `test that when file available offline with timestamp compares timestamp with server and returns value`(
        lastModifiedTime: Long,
        expected: Boolean,
    ) = runTest {
        val megaNode = getMockNode()
        val offline = getOffline(lastModifiedTime)
        val file: File = mock()
        whenever(file.lastModified().milliseconds.inWholeSeconds).thenReturn(
            expectedModificationTime
        )
        whenever(megaLocalRoomGateway.getOfflineInformation(megaNode.handle)).thenReturn(offline)
        whenever(
            fileGateway.getLocalFile(
                fileName = expectedName,
                fileSize = expectedSize,
                lastModifiedDate = expectedModificationTime
            )
        ).thenReturn(file)

        val availableOffline = underTest(megaNode, offline)
        Truth.assertThat(availableOffline).isEqualTo(expected)
    }

    private fun getMockNode(
        name: String = expectedName,
        size: Long = expectedSize,
        label: Int = expectedLabel,
        id: Long = expectedId,
        parentId: Long = expectedParentId,
        base64Id: String = expectedBase64Id,
        modificationTime: Long = expectedModificationTime,
        originalFingerprint: String = expectedOriginalFingerprint,
        fingerprint: String = expectedFingerprint,
        duration: Int = expectedDuration,
        isExported: Boolean = true,
        publicLink: String = expectedPublicLink,
        publicLinkCreationTime: Long = expectedPublicLinkCreationTime,
    ): MegaNode {
        val node = mock<MegaNode> {
            on { this.name }.thenReturn(name)
            on { this.size }.thenReturn(size)
            on { this.label }.thenReturn(label)
            on { this.handle }.thenReturn(id)
            on { this.parentHandle }.thenReturn(parentId)
            on { this.base64Handle }.thenReturn(base64Id)
            on { this.modificationTime }.thenReturn(modificationTime)
            on { this.fingerprint }.thenReturn(fingerprint)
            on { this.originalFingerprint }.thenReturn(originalFingerprint)
            on { this.duration }.thenReturn(duration)
            on { this.isFile }.thenReturn(true)
            on { this.isFolder }.thenReturn(false)
            on { this.isExported }.thenReturn(isExported)
            on { this.publicLink }.thenReturn(publicLink)
            on { this.publicLinkCreationTime }.thenReturn(publicLinkCreationTime)
            on { this.serialize() }.thenReturn(expectedSerializedString)
        }
        return node
    }

    private fun getOffline(lastModifiedTime: Long) = Offline(
        id = 1,
        handle = "$expectedId",
        path = "/path",
        name = expectedName,
        parentId = expectedParentId.toInt(),
        type = null,
        origin = 0,
        handleIncoming = "",
        lastModifiedTime = lastModifiedTime
    )

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, true),
        Arguments.of(1L, false),
        Arguments.of(0L, false),
    )
}