package mega.privacy.android.feature.documentscanner.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.entity.PageQuality
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultScanSessionRepositoryTest {

    private lateinit var underTest: DefaultScanSessionRepository

    @BeforeEach
    fun setUp() {
        underTest = DefaultScanSessionRepository()
    }

    private fun createPage(id: String, order: Int = 0) = ScannedPage(
        id = id,
        imageUri = "file:///image_$id.jpg",
        thumbnailUri = "file:///thumb_$id.jpg",
        order = order,
        capturedAt = System.currentTimeMillis(),
        quality = PageQuality.GOOD,
        boundary = null,
    )

    @Test
    fun `test that addPage adds page to session`() = runTest {
        val page = createPage("1")

        underTest.addPage(page)

        val pages = underTest.getPages()
        assertThat(pages).hasSize(1)
        assertThat(pages[0].id).isEqualTo("1")
        assertThat(pages[0].order).isEqualTo(0)
    }

    @Test
    fun `test that addPage assigns correct order for multiple pages`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))
        underTest.addPage(createPage("3"))

        val pages = underTest.getPages()
        assertThat(pages).hasSize(3)
        assertThat(pages[0].order).isEqualTo(0)
        assertThat(pages[1].order).isEqualTo(1)
        assertThat(pages[2].order).isEqualTo(2)
    }

    @Test
    fun `test that removePage removes correct page and reindexes`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))
        underTest.addPage(createPage("3"))

        underTest.removePage("2")

        val pages = underTest.getPages()
        assertThat(pages).hasSize(2)
        assertThat(pages[0].id).isEqualTo("1")
        assertThat(pages[0].order).isEqualTo(0)
        assertThat(pages[1].id).isEqualTo("3")
        assertThat(pages[1].order).isEqualTo(1)
    }

    @Test
    fun `test that removePage does nothing for unknown id`() = runTest {
        underTest.addPage(createPage("1"))

        underTest.removePage("unknown")

        assertThat(underTest.getPages()).hasSize(1)
    }

    @Test
    fun `test that reorderPages moves page forward`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))
        underTest.addPage(createPage("3"))

        underTest.reorderPages(fromIndex = 0, toIndex = 2)

        val pages = underTest.getPages()
        assertThat(pages.map { it.id }).containsExactly("2", "3", "1").inOrder()
        assertThat(pages.map { it.order }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `test that reorderPages moves page backward`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))
        underTest.addPage(createPage("3"))

        underTest.reorderPages(fromIndex = 2, toIndex = 0)

        val pages = underTest.getPages()
        assertThat(pages.map { it.id }).containsExactly("3", "1", "2").inOrder()
        assertThat(pages.map { it.order }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `test that reorderPages ignores out of bounds indices`() = runTest {
        underTest.addPage(createPage("1"))

        underTest.reorderPages(fromIndex = 0, toIndex = 5)

        assertThat(underTest.getPages()).hasSize(1)
        assertThat(underTest.getPages()[0].id).isEqualTo("1")
    }

    @Test
    fun `test that replacePage replaces correct page and preserves order`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))

        val replacement = createPage("2-new")
        underTest.replacePage("2", replacement)

        val pages = underTest.getPages()
        assertThat(pages).hasSize(2)
        assertThat(pages[1].id).isEqualTo("2-new")
        assertThat(pages[1].order).isEqualTo(1)
    }

    @Test
    fun `test that clearSession removes all pages`() = runTest {
        underTest.addPage(createPage("1"))
        underTest.addPage(createPage("2"))

        underTest.clearSession()

        assertThat(underTest.getPages()).isEmpty()
    }

    @Test
    fun `test that getSession emits updates when pages change`() = runTest {
        underTest.getSession().test {
            val initial = awaitItem()
            assertThat(initial.pages).isEmpty()

            underTest.addPage(createPage("1"))
            val afterAdd = awaitItem()
            assertThat(afterAdd.pages).hasSize(1)

            underTest.clearSession()
            val afterClear = awaitItem()
            assertThat(afterClear.pages).isEmpty()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that clearSession resets session metadata`() = runTest {
        underTest.getSession().test {
            val initial = awaitItem()
            val initialId = initial.id
            val initialCreatedAt = initial.createdAt

            underTest.clearSession()
            val afterClear = awaitItem()

            assertThat(afterClear.id).isNotEqualTo(initialId)
            assertThat(afterClear.createdAt).isAtLeast(initialCreatedAt)
            assertThat(afterClear.pages).isEmpty()

            cancelAndConsumeRemainingEvents()
        }
    }
}
