package mega.privacy.android.shared.nodes.namecollision

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NameCollisionResultEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that an outcome invokes onOutcome and clears the result`() {
        val expected = NameCollisionOutcome.ActionCompleted(NameCollisionActionResult("Moved"))
        var received: NameCollisionOutcome? = null
        var clearedKey: String? = null
        val monitorResult: (String) -> Flow<Any?> = { key ->
            if (key == NameCollisionNavKey.RESULT) flowOf(expected) else emptyFlow()
        }

        composeTestRule.setContent {
            NameCollisionResultEffect(
                monitorResult = monitorResult,
                clearResult = { clearedKey = it },
                onOutcome = { received = it },
            )
        }

        assertThat(received).isEqualTo(expected)
        assertThat(clearedKey).isEqualTo(NameCollisionNavKey.RESULT)
    }

    @Test
    fun `test that no result does not invoke onOutcome`() {
        var received: NameCollisionOutcome? = null
        var clearedKey: String? = null

        composeTestRule.setContent {
            NameCollisionResultEffect(
                monitorResult = { emptyFlow() },
                clearResult = { clearedKey = it },
                onOutcome = { received = it },
            )
        }

        assertThat(received).isNull()
        assertThat(clearedKey).isNull()
    }

    @Test
    fun `test that a result of another type is ignored`() {
        var received: NameCollisionOutcome? = null
        var clearedKey: String? = null

        composeTestRule.setContent {
            NameCollisionResultEffect(
                monitorResult = { flowOf("not an outcome") },
                clearResult = { clearedKey = it },
                onOutcome = { received = it },
            )
        }

        assertThat(received).isNull()
        assertThat(clearedKey).isNull()
    }
}
