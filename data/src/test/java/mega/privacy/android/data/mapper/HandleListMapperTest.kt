package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import nz.mega.sdk.MegaHandleList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class HandleListMapperTest {
    private lateinit var underTest: HandleListMapper

    @Before
    fun setUp() {
        underTest = HandleListMapper()
    }

    @Test
    fun `test that empty MegaHandleList returns an empty list`() {
        val megaHandleListTest1 = mock<MegaHandleList>()
        Truth.assertThat(underTest(megaHandleListTest1)).isEmpty()
    }

    @Test
    fun `test that a MegaHandleList with 1 item returns a list with 1 item`() {
        val megaHandleListTest2 = mock<MegaHandleList>()
        megaHandleListTest2.addMegaHandle(1)
        Truth.assertThat(underTest(megaHandleListTest2)).isEmpty()
    }

    @Test
    fun `test that a MegaHandleList with 5 items returns a list with 5 items`() {
        val megaHandleListTest3 = mock<MegaHandleList>()
        megaHandleListTest3.addMegaHandle(1)
        megaHandleListTest3.addMegaHandle(2)
        megaHandleListTest3.addMegaHandle(3)
        megaHandleListTest3.addMegaHandle(4)
        megaHandleListTest3.addMegaHandle(5)
        Truth.assertThat(underTest(megaHandleListTest3)).isEmpty()
    }
}

