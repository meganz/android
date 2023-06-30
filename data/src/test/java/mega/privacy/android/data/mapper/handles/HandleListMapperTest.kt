package mega.privacy.android.data.mapper.handles

import com.google.common.truth.Truth
import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleListMapperTest {

    private lateinit var underTest: HandleListMapper

    @BeforeAll
    fun setUp() {
        underTest = HandleListMapper()
    }

    @ParameterizedTest(name = " {0} when received list is {0}")
    @MethodSource("provideParameters")
    fun `test that handle list mapper returns`(
        list: List<Long>,
        megaHandleList: MegaHandleList,
    ) {
        Truth.assertThat(underTest(megaHandleList)).isEqualTo(list)
    }

    private val emptyMegaHandleList = mock<MegaHandleList> {
        on { size() }.thenReturn(0)
    }

    private val handle1 = 1L
    private val megaHandleList1 = mock<MegaHandleList> {
        on { size() }.thenReturn(1)
        on { get(0) }.thenReturn(handle1)
    }
    private val handle2 = 2L
    private val handle3 = 3L
    private val megaHandleList2 = mock<MegaHandleList> {
        on { size() }.thenReturn(3)
        on { get(0) }.thenReturn(handle1)
        on { get(1) }.thenReturn(handle2)
        on { get(2) }.thenReturn(handle3)
    }

    private fun provideParameters(): Stream<Arguments?>? =
        Stream.of(
            Arguments.of(emptyList<Long>(), emptyMegaHandleList),
            Arguments.of(listOf(handle1), megaHandleList1),
            Arguments.of(listOf(handle1, handle2, handle3), megaHandleList2),
        )
}

