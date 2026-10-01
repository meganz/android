package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAlbumLinkNodeContentUriUseCaseTest {
    private lateinit var underTest: GetAlbumLinkNodeContentUriUseCase

    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val getAlbumPhotoFileUrlByNodeIdUseCase = mock<GetAlbumPhotoFileUrlByNodeIdUseCase>()

    private val nodeId = NodeId(1L)
    private val expectedUrl = "url"

    @BeforeAll
    fun setup() {
        underTest = GetAlbumLinkNodeContentUriUseCase(
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            getAlbumPhotoFileUrlByNodeIdUseCase = getAlbumPhotoFileUrlByNodeIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase,
            getAlbumPhotoFileUrlByNodeIdUseCase,
        )
    }

    @Test
    fun `test that invoke returns RemoteContentUri with shouldStop true when the main http server is not running`() =
        runTest {
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(nodeId)).thenReturn(expectedUrl)
            val actual = underTest(nodeId)
            assertThat(actual).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = true)
            )
            verify(megaApiHttpServerStartUseCase).invoke()
        }

    @Test
    fun `test that invoke returns RemoteContentUri with shouldStop false when the main http server is already running`() =
        runTest {
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(nodeId)).thenReturn(expectedUrl)
            val actual = underTest(nodeId)
            assertThat(actual).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = false)
            )
            verifyNoInteractions(megaApiHttpServerStartUseCase)
        }

    @Test
    fun `test that invoke throws IllegalStateException when getAlbumPhotoFileUrlByNodeIdUseCase returns null`() =
        runTest {
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(nodeId)).thenReturn(null)
            assertThrows<IllegalStateException> {
                underTest(nodeId)
            }
        }
}
