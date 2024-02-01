package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import nz.mega.sdk.MegaStringList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaStringListMapperTest {

    private lateinit var underTest: MegaStringListMapper

    private val megaStringListProvider = mock<MegaStringListProvider>()

    @BeforeAll
    fun setup() {
        underTest = MegaStringListMapper(megaStringListProvider)
    }

    @Test
    fun `test that mega string list mapper returns correctly`() {
        val item1 = "item1"
        val item2 = "item2"
        val item3 = "item3"
        val stringList = listOf(item1, item2, item3)
        val megaStringList = mock<MegaStringList> {
            on { size() } doReturn 3
            on { get(0) } doReturn item1
            on { get(1) } doReturn item2
            on { get(2) } doReturn item3
        }
        megaStringListProvider.stub {
            on { invoke() }.thenReturn(megaStringList)
        }
        Truth.assertThat(underTest.invoke(stringList)).isEqualTo(megaStringList)
    }
}