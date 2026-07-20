package mega.privacy.android.app.appstate.global.initialisation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiserAction
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiserAction
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalInitialiserTest {
    private lateinit var underTest: GlobalInitialiser

    @Test
    fun `test that onAppCreate runs critical initialisers in list order before returning`() =
        runTest {
            val executionOrder = mutableListOf<String>()
            val first =
                SynchronousAppCreateInitialiserAction(
                    name = "first"
                ) {
                    executionOrder += "first"
                }
            val second =
                SynchronousAppCreateInitialiserAction(
                    name = "second"
                ) {
                    executionOrder += "second"
                }

            initUnderTest(
                testScope = this,
                syncAppCreateInitialisers = listOf(first, second),
            )

            underTest.onAppCreate()

            assertThat(executionOrder).containsExactly("first", "second").inOrder()
        }

    @Test
    fun `test that onAppCreate runs critical initialisers before async ones regardless of list order`() =
        runTest {
            val executionOrder = mutableListOf<String>()
            val async = AsyncAppCreateInitialiserAction(name = "async") {
                executionOrder += "async"
            }
            val critical = SynchronousAppCreateInitialiserAction(name = "critical") {
                executionOrder += "critical"
            }

            initUnderTest(
                testScope = this,
                syncAppCreateInitialisers = listOf(critical),
                asyncAppCreateInitialisers = setOf(async),
            )

            underTest.onAppCreate()
            advanceUntilIdle()

            assertThat(executionOrder).containsExactly("critical", "async").inOrder()
        }

    @Test
    fun `test that onAppCreate propagates exception when a critical initialiser fails`() =
        runTest {
            val critical = SynchronousAppCreateInitialiserAction(name = "critical") {
                throw RuntimeException("Critical boot failure")
            }

            initUnderTest(
                testScope = this,
                syncAppCreateInitialisers = listOf(critical),
            )

            assertThrows<RuntimeException> {
                underTest.onAppCreate()
            }
        }

    @Test
    fun `test that onAppCreate runs all async initialisers when one fails`() = runTest {
        val invoked = mutableListOf<String>()
        val failing = AsyncAppCreateInitialiserAction(name = "failing") {
            throw RuntimeException("Async failure")
        }
        val other = AsyncAppCreateInitialiserAction(name = "other") {
            invoked += "other"
        }
        val another = AsyncAppCreateInitialiserAction(name = "another") {
            invoked += "another"
        }

        initUnderTest(
            testScope = this,
            asyncAppCreateInitialisers = setOf(failing, other, another),
        )

        assertDoesNotThrow {
            underTest.onAppCreate()
            advanceUntilIdle()
        }

        assertThat(invoked).containsExactly("other", "another")
    }

    @Test
    fun `test that onAppCreate skips initialisers rejected by the filter`() = runTest {
        val invoked = mutableListOf<String>()
        val included = SynchronousAppCreateInitialiserAction(name = "included") {
            invoked += "included"
        }
        val excluded = AsyncAppCreateInitialiserAction(name = "excluded") {
            invoked += "excluded"
        }

        initUnderTest(
            testScope = this,
            syncAppCreateInitialisers = listOf(included),
            asyncAppCreateInitialisers = setOf(excluded),
        )

        underTest.onAppCreate { it.name != "excluded" }
        advanceUntilIdle()

        assertThat(invoked).containsExactly("included")
    }

    @Test
    fun `test that onAppCreate is a no-op when called a second time`() = runTest {
        val invoked = mutableListOf<String>()
        val critical = SynchronousAppCreateInitialiserAction(name = "critical") {
            invoked += "critical"
        }
        val async = AsyncAppCreateInitialiserAction(name = "async") {
            invoked += "async"
        }

        initUnderTest(
            testScope = this,
            syncAppCreateInitialisers = listOf(critical),
            asyncAppCreateInitialisers = setOf(async),
        )

        underTest.onAppCreate()
        advanceUntilIdle()
        underTest.onAppCreate()
        advanceUntilIdle()

        assertThat(invoked).containsExactly("critical", "async")
    }

    @Test
    fun `test that app start initialisers are called in onAppStart`() = runTest {
        val appStartInitialiser1 = mock<AppStartInitialiserAction>()
        val appStartInitialiser2 = mock<AppStartInitialiserAction>()

        appStartInitialiser1.stub { onBlocking { invoke() }.thenReturn(Unit) }
        appStartInitialiser2.stub { onBlocking { invoke() }.thenReturn(Unit) }

        initUnderTest(
            testScope = this,
            appStartInitialisers = setOf(appStartInitialiser1, appStartInitialiser2),
        )

        underTest.onAppStart()

        advanceUntilIdle()

        verify(appStartInitialiser1).invoke()
        verify(appStartInitialiser2).invoke()
    }

    @Test
    fun `test that post login initialisers are called in onPostLogin`() = runTest {
        val postLoginInitialiser1 = mock<PostLoginInitialiserAction>()
        val postLoginInitialiser2 = mock<PostLoginInitialiserAction>()

        postLoginInitialiser1.stub { onBlocking { invoke(any(), eq(true)) }.thenReturn(Unit) }
        postLoginInitialiser2.stub { onBlocking { invoke(any(), eq(true)) }.thenReturn(Unit) }

        initUnderTest(
            testScope = this,
            postLoginInitialisers = setOf(postLoginInitialiser1, postLoginInitialiser2),
        )

        underTest.onPostLogin("Session", true)

        advanceUntilIdle()

        verify(postLoginInitialiser1).invoke("Session", true)
        verify(postLoginInitialiser2).invoke("Session", true)
    }

    @Test
    fun `test that app start and post login initialisers handle exceptions gracefully`() =
        runTest {
            val appStartInitialiser1 = mock<AppStartInitialiserAction>()
            val postLoginInitialiser1 = mock<PostLoginInitialiserAction>()

            appStartInitialiser1.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("App start error"))
            }
            postLoginInitialiser1.stub {
                onBlocking { invoke(any(), eq(true)) }.thenThrow(RuntimeException("Post login error"))
            }

            initUnderTest(
                testScope = this,
                appStartInitialisers = setOf(appStartInitialiser1),
                postLoginInitialisers = setOf(postLoginInitialiser1),
            )

            assertDoesNotThrow {
                underTest.onAppStart()
                advanceUntilIdle()
            }
            assertDoesNotThrow {
                underTest.onPostLogin("Session", true)
                advanceUntilIdle()
            }
        }

    private fun initUnderTest(
        testScope: CoroutineScope,
        syncAppCreateInitialisers: List<SynchronousAppCreateInitialiser> = emptyList(),
        asyncAppCreateInitialisers: Set<AsyncAppCreateInitialiser> = emptySet(),
        appStartInitialisers: Set<AppStartInitialiserAction> = emptySet(),
        postLoginInitialisers: Set<PostLoginInitialiserAction> = emptySet(),
    ) {
        underTest = GlobalInitialiser(
            coroutineScope = testScope,
            syncAppCreateInitialisers = syncAppCreateInitialisers,
            asyncAppCreateInitialisers = asyncAppCreateInitialisers,
            appStartInitialisers = appStartInitialisers,
            postLoginInitialisers = { postLoginInitialisers },
        )
    }
}
