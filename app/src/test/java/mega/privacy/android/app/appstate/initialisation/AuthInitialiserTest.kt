package mega.privacy.android.app.appstate.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInitialiserTest {
    private lateinit var underTest: GlobalInitialiser

    @Test
    fun `test that app start initialisers are called in onAppStart`() = runTest {
        // Create local initializer mocks
        val appStartInitialiser1 = mock<AppStartInitialiserAction>()
        val appStartInitialiser2 = mock<AppStartInitialiserAction>()

        // Setup initializers to return Unit
        appStartInitialiser1.stub { onBlocking { invoke() }.thenReturn(Unit) }
        appStartInitialiser2.stub { onBlocking { invoke() }.thenReturn(Unit) }

        // Create ViewModel with initializers
        initUnderTest(
            testScope = this,
            appStartInitialisers = setOf(appStartInitialiser1, appStartInitialiser2),
        )

        // Call onAppStart
        underTest.onAppStart()

        advanceUntilIdle()

        // Verify initializers were called
        verify(appStartInitialiser1).invoke()
        verify(appStartInitialiser2).invoke()

    }

    @Test
    fun `test that post login initialisers are called in onPostLogin`() = runTest {
        // Create local initializer mocks
        val postLoginInitialiser1 = mock<PostLoginInitialiserAction>()
        val postLoginInitialiser2 = mock<PostLoginInitialiserAction>()

        // Setup initializers to return Unit
        postLoginInitialiser1.stub { onBlocking { invoke(any(), eq(true)) }.thenReturn(Unit) }
        postLoginInitialiser2.stub { onBlocking { invoke(any(), eq(true)) }.thenReturn(Unit) }

        // Create ViewModel with initializers
        initUnderTest(
            testScope = this,
            postLoginInitialisers = setOf(postLoginInitialiser1, postLoginInitialiser2),
        )

        // Call onPostLogin
        underTest.onPostLogin("Session", true)

        advanceUntilIdle()

        // Verify initializers were called
        verify(postLoginInitialiser1).invoke("Session", true)
        verify(postLoginInitialiser2).invoke("Session", true)
    }

    @Test
    fun `test that initializers handle exceptions gracefully`() = runTest {
        // Create local initializer mocks
        val appStartInitialiser1 = mock<AppStartInitialiserAction>()
        val postLoginInitialiser1 = mock<PostLoginInitialiserAction>()

        // Setup initializers to throw exceptions
        appStartInitialiser1.stub { onBlocking { invoke() }.thenThrow(RuntimeException("App start error")) }
        postLoginInitialiser1.stub { onBlocking { invoke(any(), eq(true)) }.thenThrow(RuntimeException("Post login error")) }

        // Create ViewModel with initializers
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
        appStartInitialisers: Set<AppStartInitialiserAction> = emptySet(),
        postLoginInitialisers: Set<PostLoginInitialiserAction> = emptySet(),
    ) {
        underTest = GlobalInitialiser(
            coroutineScope = testScope,
            appStartInitialisers = appStartInitialisers,
            postLoginInitialisers = { postLoginInitialisers },
        )

    }

}