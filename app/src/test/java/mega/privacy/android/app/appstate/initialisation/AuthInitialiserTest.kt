package mega.privacy.android.app.appstate.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PreLoginInitialiser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInitialiserTest {
    private lateinit var underTest: AuthInitialiser

    @Test
    fun `test that app start initialisers are called in onAppStart`() = runTest {
        // Create local initializer mocks
        val appStartInitialiser1 = mock<AppStartInitialiser>()
        val appStartInitialiser2 = mock<AppStartInitialiser>()

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
    fun `test that pre login initialisers are called with null in onPreLogin if no session passed`() =
        runTest {
            // Create local initializer mocks
            val preLoginInitialiser1 = mock<PreLoginInitialiser>()
            val preLoginInitialiser2 = mock<PreLoginInitialiser>()

            // Setup initializers to return Unit
            preLoginInitialiser1.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }
            preLoginInitialiser2.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }

            // Create ViewModel with initializers
            initUnderTest(
                testScope = this,
                preLoginInitialisers = setOf(preLoginInitialiser1, preLoginInitialiser2),
            )

            // Call onPreLogin
            underTest.onPreLogin(null)

            advanceUntilIdle()

            // Verify initializers were called
            verify(preLoginInitialiser1).invoke(null)
            verify(preLoginInitialiser2).invoke(null)

        }

    @Test
    fun `test that pre login initialisers are called with session in onPreLogin if session passed`() =
        runTest {
            // Create local initializer mocks
            val preLoginInitialiser1 = mock<PreLoginInitialiser>()
            val preLoginInitialiser2 = mock<PreLoginInitialiser>()
            val session = "test-session"

            // Setup initializers to return Unit
            preLoginInitialiser1.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }
            preLoginInitialiser2.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }

            // Create ViewModel with initializers
            initUnderTest(
                testScope = this,
                preLoginInitialisers = setOf(preLoginInitialiser1, preLoginInitialiser2),
            )

            underTest.onPreLogin(session)

            advanceUntilIdle()

            verify(preLoginInitialiser1).invoke(session)
            verify(preLoginInitialiser2).invoke(session)
        }

    @Test
    fun `test that post login initialisers are called in onPostLogin`() = runTest {
        // Create local initializer mocks
        val postLoginInitialiser1 = mock<PostLoginInitialiser>()
        val postLoginInitialiser2 = mock<PostLoginInitialiser>()

        // Setup initializers to return Unit
        postLoginInitialiser1.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }
        postLoginInitialiser2.stub { onBlocking { invoke(any()) }.thenReturn(Unit) }

        // Create ViewModel with initializers
        initUnderTest(
            testScope = this,
            postLoginInitialisers = setOf(postLoginInitialiser1, postLoginInitialiser2),
        )

        // Call onPostLogin
        underTest.onPostLogin("Session")

        advanceUntilIdle()

        // Verify initializers were called
        verify(postLoginInitialiser1).invoke("Session")
        verify(postLoginInitialiser2).invoke("Session")
    }

    @Test
    fun `test that initializers handle exceptions gracefully`() = runTest {
        // Create local initializer mocks
        val appStartInitialiser1 = mock<AppStartInitialiser>()
        val preLoginInitialiser1 = mock<PreLoginInitialiser>()
        val postLoginInitialiser1 = mock<PostLoginInitialiser>()

        // Setup initializers to throw exceptions
        appStartInitialiser1.stub { onBlocking { invoke() }.thenThrow(RuntimeException("App start error")) }
        preLoginInitialiser1.stub { onBlocking { invoke(any()) }.thenThrow(RuntimeException("Pre login error")) }
        postLoginInitialiser1.stub { onBlocking { invoke(any()) }.thenThrow(RuntimeException("Post login error")) }

        // Create ViewModel with initializers
        initUnderTest(
            testScope = this,
            appStartInitialisers = setOf(appStartInitialiser1),
            preLoginInitialisers = setOf(preLoginInitialiser1),
            postLoginInitialisers = setOf(postLoginInitialiser1),
        )

        assertDoesNotThrow {
            underTest.onAppStart()
            advanceUntilIdle()
        }
        assertDoesNotThrow {
            underTest.onPreLogin("Session")
            advanceUntilIdle()
        }
        assertDoesNotThrow {
            underTest.onPostLogin("Session")
            advanceUntilIdle()
        }

    }

    private fun initUnderTest(
        testScope: CoroutineScope,
        appStartInitialisers: Set<AppStartInitialiser> = emptySet(),
        preLoginInitialisers: Set<PreLoginInitialiser> = emptySet(),
        postLoginInitialisers: Set<PostLoginInitialiser> = emptySet(),
    ) {
        underTest = AuthInitialiser(
            coroutineScope = testScope,
            appStartInitialisers = appStartInitialisers,
            preLoginInitialisers = preLoginInitialisers,
            postLoginInitialisers = postLoginInitialisers,
        )

    }

}