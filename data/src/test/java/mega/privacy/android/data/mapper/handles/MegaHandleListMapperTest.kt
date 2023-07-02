package mega.privacy.android.data.mapper.handles

import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaHandleListMapperTest {

    private lateinit var underTest: MegaHandleListMapper

    private val megaHandleListProvider = mock<MegaHandleListProvider>()

    @BeforeAll
    fun setUp() {
        underTest = MegaHandleListMapper(megaHandleListProvider = megaHandleListProvider)
    }

    @ParameterizedTest(name = "When received list is {0}")
    @MethodSource("provideParameters")
    fun `test that handle list mapper adds all handles`(
        list: List<Long>,
        assertions: List<(MegaHandleList) -> Unit>,
    ) {
        val mockMegaHandleList = mock<MegaHandleList>()
        megaHandleListProvider.stub {
            on { invoke() }.thenReturn(mockMegaHandleList)
        }

        val megaHandleList = underTest(list)
        assertions.forEach {
            it.invoke(megaHandleList ?: throw NullPointerException())
        }
    }


    private val handle1 = 1L
    private val handle2 = 2L
    private val handle3 = 3L


    private fun provideParameters(): Stream<Arguments?>? =
        Stream.of(
            Arguments.of(
                emptyList<Long>(),
                listOf { mock: MegaHandleList -> verifyNoInteractions(mock) },
            ),
            Arguments.of(
                listOf(handle1),
                listOf { mock: MegaHandleList -> verify(mock).addMegaHandle(handle1) },
            ),
            Arguments.of(
                listOf(handle1, handle2, handle3),
                listOf(
                    { mock: MegaHandleList -> verify(mock).addMegaHandle(handle1) },
                    { mock: MegaHandleList -> verify(mock).addMegaHandle(handle2) },
                    { mock: MegaHandleList -> verify(mock).addMegaHandle(handle3) },
                ),
            ),
        )
}