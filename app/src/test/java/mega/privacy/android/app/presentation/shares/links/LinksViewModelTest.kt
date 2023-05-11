package mega.privacy.android.app.presentation.shares.links

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.shares.links.model.LinksState
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LinksViewModelTest {
    private lateinit var underTest: LinksViewModel

    private lateinit var monitorLinksChannel: Channel<List<PublicLinkNode>>
    private val monitorPublicLinksUseCase =
        mock<MonitorPublicLinksUseCase> {
            on { invoke() }.thenAnswer {
                monitorLinksChannel = Channel()
                monitorLinksChannel.consumeAsFlow()
            }
        }

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    internal fun setUp() {
        underTest = LinksViewModel(
            monitorPublicLinksUseCase = monitorPublicLinksUseCase
        )
    }


    @Test
    internal fun `test that public links are returned`() = runTest {
        val publicLinkNodes = listOf<PublicLinkFolder>(mock())

        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
            monitorLinksChannel.send(publicLinkNodes)
            val expected = awaitItem()
            assertThat(expected).isInstanceOf(LinksState.Data::class.java)
            assertThat((expected as LinksState.Data).links).isEqualTo(publicLinkNodes)
        }
    }

    @Test
    internal fun `test that an empty list of public links return no links state`() = runTest {

        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
            monitorLinksChannel.send(emptyList())
            assertThat(awaitItem()).isInstanceOf(LinksState.NoPublicLinks::class.java)
        }
    }

    @Test
    internal fun `test that calling open folder returns the children`() = runTest {
        val publicLinkNodes = listOf<PublicLinkFolder>(mock())
        val flow = flow {
            emit(publicLinkNodes)
            awaitCancellation()
        }
        val parentNode = mock<PublicLinkFolder> {
            on { children }.thenReturn(flow)
        }

        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
            underTest.openFolder(parentNode)
            val expected = awaitItem()
            assertThat(expected).isInstanceOf(LinksState.ChildData::class.java)
            assertThat((expected as LinksState.ChildData).links).isEqualTo(publicLinkNodes)
        }
    }

    @Test
    internal fun `test that updates from the root are ignored while children are displayed`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(publicLinkNodes)
                awaitCancellation()
            }
            val parentNode = mock<PublicLinkFolder> {
                on { children }.thenReturn(flow)
            }

            underTest.state.onEach {
                println(it)
            }.test {
                assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
                monitorLinksChannel.send(emptyList())
                assertThat(awaitItem()).isInstanceOf(LinksState.NoPublicLinks::class.java)
                underTest.openFolder(parentNode)
                val expected = awaitItem()
                assertThat(expected).isInstanceOf(LinksState.ChildData::class.java)
                assertThat(monitorLinksChannel.isClosedForSend).isTrue()
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @Test
    internal fun `test that calling closeFolder returns the children of the parent folder`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(publicLinkNodes)
                awaitCancellation()
            }
            val parentNode = mock<PublicLinkFolder> {
                on { children }.thenReturn(flow)
            }

            val currentFolder = mock<PublicLinkFolder> {
                on { parent }.thenReturn(parentNode)
            }

            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
                underTest.closeFolder(currentFolder)
                val expected = awaitItem()
                assertThat(expected).isInstanceOf(LinksState.ChildData::class.java)
                assertThat((expected as LinksState.ChildData).links).isEqualTo(publicLinkNodes)
            }
        }

    @Test
    internal fun `test that calling closeFolder on the first level returns all public nodes`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())

            val currentFolder = mock<PublicLinkFolder> {
                on { parent }.thenReturn(null)
            }

            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(LinksState.Loading::class.java)
                underTest.closeFolder(currentFolder)
                monitorLinksChannel.send(publicLinkNodes)
                val expected = awaitItem()
                assertThat(expected).isInstanceOf(LinksState.Data::class.java)
                assertThat((expected as LinksState.Data).links).isEqualTo(publicLinkNodes)
            }
        }

}