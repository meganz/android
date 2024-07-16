package test.mega.privacy.android.app.namecollision.usecase

import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckNameCollisionUseCaseTest {
    private lateinit var underTest: CheckNameCollisionUseCase

    @BeforeAll
    internal fun initialise() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()

    @BeforeEach
    internal fun setUp() {
        underTest = CheckNameCollisionUseCase(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            getChatMessageUseCase = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @AfterAll
    fun tearDown() {
        RxAndroidPlugins.reset()
    }

    @Nested
    @DisplayName("Inner most check call")
    inner class InnerCheck {
        @Test
        internal fun `test that check with a null parent throws ParentDoesNotExistException`() =
            runTest {
                assertThrows<MegaNodeException.ParentDoesNotExistException>() {
                    underTest.checkAsync(
                        "name",
                        null
                    )
                }
            }

        @Test
        internal fun `test that check with no child throws ChildDoesNotExistsException`() =
            runTest {
                megaApiGateway.stub {
                    onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(null)
                }
                assertThrows<MegaNodeException.ChildDoesNotExistsException>() {
                    underTest.checkAsync(
                        "name",
                        mock()
                    )
                }
            }

        @Test
        internal fun `test that check returns child handle when found`() = runTest {
            val expected = 1234L
            val child = mock<MegaNode> {
                on { handle }.thenReturn(expected)
            }
            megaApiGateway.stub {
                onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
            }
            assertThat(underTest.checkAsync("name", mock()))
                .isEqualTo(expected)
        }
    }
}