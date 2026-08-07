package mega.privacy.android.app.presentation.settings.customisenavigation.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NavigationSelectionTest {

    @Test
    fun `test that addNavigationItem appends the id`() {
        val actual = listOf("home", "drive", "media").addNavigationItem("chat")

        assertThat(actual).isEqualTo(listOf("home", "drive", "media", "chat"))
    }

    @Test
    fun `test that addNavigationItem appends the id even when the selection is full`() {
        val actual = listOf("home", "drive", "media", "chat").addNavigationItem("offline")

        assertThat(actual).isEqualTo(listOf("home", "drive", "media", "chat", "offline"))
    }

    @Test
    fun `test that addNavigationItem returns the same selection when the id is already selected`() {
        val selection = listOf("home", "drive", "media")

        assertThat(selection.addNavigationItem("drive")).isEqualTo(selection)
    }

    @Test
    fun `test that removeNavigationItem removes the id`() {
        val actual = listOf("home", "drive", "media", "chat").removeNavigationItem("drive")

        assertThat(actual).isEqualTo(listOf("home", "media", "chat"))
    }

    @Test
    fun `test that removeNavigationItem removes the id even when at the minimum`() {
        val actual = listOf("home", "drive", "media").removeNavigationItem("drive")

        assertThat(actual).isEqualTo(listOf("home", "media"))
    }

    @Test
    fun `test that removeNavigationItem returns the same selection when the id is not selected`() {
        val selection = listOf("home", "drive", "media", "chat")

        assertThat(selection.removeNavigationItem("offline")).isEqualTo(selection)
    }

    @Test
    fun `test that navigationSelectionError returns TooFewItems when below the minimum`() {
        val actual = listOf("home", "drive").navigationSelectionError()

        assertThat(actual).isEqualTo(NavigationSelectionError.TooFewItems)
    }

    @Test
    fun `test that navigationSelectionError returns TooManyItems when above the maximum`() {
        val actual = listOf("home", "drive", "media", "chat", "offline").navigationSelectionError()

        assertThat(actual).isEqualTo(NavigationSelectionError.TooManyItems)
    }

    @Test
    fun `test that navigationSelectionError returns null when within the allowed range`() {
        assertThat(listOf("home", "drive", "media").navigationSelectionError()).isNull()
        assertThat(listOf("home", "drive", "media", "chat").navigationSelectionError()).isNull()
    }

    @Test
    fun `test that moveNavigationItem moves the id to the target index`() {
        val actual = listOf("home", "drive", "media", "chat").moveNavigationItem(3, 0)

        assertThat(actual).isEqualTo(listOf("chat", "home", "drive", "media"))
    }

    @Test
    fun `test that moveNavigationItem returns the same list when an index is out of bounds`() {
        val selection = listOf("home", "drive", "media")

        assertThat(selection.moveNavigationItem(0, 3)).isEqualTo(selection)
        assertThat(selection.moveNavigationItem(-1, 0)).isEqualTo(selection)
    }
}
