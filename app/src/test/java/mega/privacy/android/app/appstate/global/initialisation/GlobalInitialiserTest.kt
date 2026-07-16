package mega.privacy.android.app.appstate.global.initialisation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiserAction
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
            val first = AppCreateInitialiserAction(name = "first", isCritical = true) {
                executionOrder += "first"
            }
            val second = AppCreateInitialiserAction(name = "second", isCritical = true) {
                executionOrder += "second"
            }

            initUnderTest(
                testScope = this,
                appCreateInitialisers = listOf(first, second),
            )

            underTest.onAppCreate()

            assertThat(executionOrder).containsExactly("first", "second").inOrder()
        }

    @Test
    fun `test that onAppCreate propagates exception when a critical initialiser fails`() =
        runTest {
            val critical = AppCreateInitialiserAction(name = "critical", isCritical = true) {
                throw RuntimeException("Critical boot failure")
            }

            initUnderTest(
                testScope = this,
                appCreateInitialisers = listOf(critical),
            )

            assertThrows<RuntimeException> {
                underTest.onAppCreate()
            }
        }

    @Test
    fun `test that onAppCreate runs all async initialisers when one fails`() = runTest {
        val invoked = mutableListOf<String>()
        val failing = AppCreateInitialiserAction(name = "failing", isCritical = false) {
            throw RuntimeException("Async failure")
        }
        val other = AppCreateInitialiserAction(name = "other", isCritical = false) {
            invoked += "other"
        }
        val another = AppCreateInitialiserAction(name = "another", isCritical = false) {
            invoked += "another"
        }

        initUnderTest(
            testScope = this,
            appCreateInitialisers = listOf(failing, other, another),
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
        val included = AppCreateInitialiserAction(name = "included", isCritical = true) {
            invoked += "included"
        }
        val excluded = AppCreateInitialiserAction(name = "excluded", isCritical = false) {
            invoked += "excluded"
        }

        initUnderTest(
            testScope = this,
            appCreateInitialisers = listOf(included, excluded),
        )

        underTest.onAppCreate { it.name != "excluded" }
        advanceUntilIdle()

        assertThat(invoked).containsExactly("included")
    }

    @Test
    fun `test that onAppCreate is a no-op when called a second time`() = runTest {
        val invoked = mutableListOf<String>()
        val critical = AppCreateInitialiserAction(name = "critical", isCritical = true) {
            invoked += "critical"
        }
        val async = AppCreateInitialiserAction(name = "async", isCritical = false) {
            invoked += "async"
        }

        initUnderTest(
            testScope = this,
            appCreateInitialisers = listOf(critical, async),
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
        appCreateInitialisers: List<AppCreateInitialiser> = emptyList(),
        appStartInitialisers: Set<AppStartInitialiserAction> = emptySet(),
        postLoginInitialisers: Set<PostLoginInitialiserAction> = emptySet(),
    ) {
        underTest = GlobalInitialiser(
            coroutineScope = testScope,
            appCreateInitialisers = appCreateInitialisers,
            appStartInitialisers = appStartInitialisers,
            postLoginInitialisers = { postLoginInitialisers },
        )
    }
}
