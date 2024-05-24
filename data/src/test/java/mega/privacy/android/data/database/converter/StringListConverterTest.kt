package mega.privacy.android.data.database.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringListConverterTest {
    @Test
    fun testRestoreList() {
        val stringListConverter = StringListConverter()
        val list = stringListConverter.restoreList("a,b,c")
        assertEquals(listOf("a", "b", "c"), list)
    }

    @Test
    fun testSaveList() {
        val stringListConverter = StringListConverter()
        val string = stringListConverter.saveList(listOf("a", "b", "c"))
        assertEquals("a,b,c", string)
    }
}