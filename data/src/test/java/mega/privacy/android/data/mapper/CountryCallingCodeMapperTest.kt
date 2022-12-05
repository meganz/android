package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringListMap
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CountryCallingCodeMapperTest {

    @Test
    fun `test that country calling codes can be mapped correctly`() {
        val countryCode = "AD"
        val callingCode = "376"
        val keyList = mock<MegaStringList> {
            on { size() }.thenReturn(1)
        }
        whenever(keyList[0]).thenReturn(countryCode)

        val listMap = mock<MegaStringListMap>() {
            on { size() }.thenReturn(1)
            on { keys }.thenReturn(keyList)
        }

        val mockDialCodes = mock<MegaStringList> {
            on { size() }.thenReturn(1)
        }
        whenever(mockDialCodes[0]).thenReturn(callingCode)

        whenever(listMap[countryCode]).thenReturn(mockDialCodes)

        val actual = toCountryCallingCodes(listMap)
        Truth.assertThat(actual).isEqualTo(listOf("$countryCode:$callingCode,"))
    }
}
