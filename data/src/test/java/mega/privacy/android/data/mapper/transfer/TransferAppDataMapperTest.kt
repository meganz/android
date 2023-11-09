package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.*
import mega.privacy.android.data.mapper.transfer.TextFileModeConstants.*
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper.Companion.APP_DATA_INDICATOR
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper.Companion.APP_DATA_REPEATED_TRANSFER_SEPARATOR
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper.Companion.APP_DATA_SEPARATOR
import mega.privacy.android.domain.entity.transfer.TransferAppData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferAppDataMapperTest {

    private lateinit var underTest: TransferAppDataMapper

    @BeforeAll
    fun setup() {
        underTest = TransferAppDataMapper()
    }

    @Test
    fun `test that an empty string is mapped to an empty list`() {
        Truth.assertThat(underTest("")).isEmpty()
    }

    @ParameterizedTest(name = "Input parameters: {0} expected result: {1}")
    @MethodSource("provideParameters")
    fun `test that a list with mapped values is returned when mapping correct parameters`(
        appDataRaw: String,
        expectedResult: List<TransferAppData>,
    ) {
        Truth.assertThat(underTest(appDataRaw)).containsExactlyElementsIn(expectedResult)
    }

    @ParameterizedTest(name = "Input parameters: {0}")
    @MethodSource("provideWrongParameters")
    fun `test that an empty list is returned when mapping wrong parameters`(
        appDataRaw: String,
    ) {
        Truth.assertThat(underTest(appDataRaw)).isEmpty()
    }

    @ParameterizedTest(name = "Input parameters: {0} expected result: {1}")
    @MethodSource("provideJoinedParameters")
    fun `test that a list with correct mapped values is returned when mapping correct and wrong joined parameters`(
        appDataRaw: String,
        expectedResult: List<TransferAppData>,
    ) {
        Truth.assertThat(underTest(appDataRaw)).containsExactlyElementsIn(expectedResult)
    }

    @ParameterizedTest(name = "Input parameters: {0} expected result: {1}")
    @MethodSource("provideRepeatedParameters")
    fun `test that a list with correct mapped values is returned when mapping correct and wrong repeated parameters`(
        appDataRaw: String,
        expectedResult: List<TransferAppData>,
    ) {
        Truth.assertThat(underTest(appDataRaw)).containsExactlyElementsIn(expectedResult)
    }

    @Nested
    inner class RealParameters {

        @Test
        fun `test that the sd card paths are mapped correctly when there are - char in the path`() {
            val raw =
                "SD_CARD_DOWNLOAD>/storage/48F8-4FB7/Download mega>content://com.android.externalstorage.documents/tree/48F8-4FB7%3ADownload%20mega"
            val expected = TransferAppData.SdCardDownload(
                "/storage/48F8-4FB7/Download mega",
                "content://com.android.externalstorage.documents/tree/48F8-4FB7%3ADownload%20mega"
            )
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }

        @Test
        fun `test that a chat upload is mapped correctly if the message id is a number`() {
            val chatId = 1265L
            val raw = "CHAT_UPLOAD>$chatId"
            val expected = TransferAppData.ChatUpload(chatId)
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }

        @Test
        fun `test that a chat upload is mapped to null, returning an empty list, if the message id is not a number`() {
            val raw = "CHAT_UPLOAD>chatId"
            Truth.assertThat(underTest(raw)).isEmpty()
        }
    }

    private fun provideWrongParameters() = wrongParameters

    private fun provideParameters() = correctParameters.map {
        Arguments.of(it.first, it.second)
    }

    /**
     * returns parameters to check when app data contains more than one value due to different types of app data, this is usually used only for [VoiceClip]
     */
    private fun provideJoinedParameters() = joinParameters(APP_DATA_SEPARATOR)

    /**
     * returns parameters to check when app data contains more than one value due to repeated transfers of the same node
     */
    private fun provideRepeatedParameters() =
        joinParameters(APP_DATA_REPEATED_TRANSFER_SEPARATOR)

    companion object {
        private const val FAKE_ID = 12345L
        private const val TARGET_PATH = "target"
        private const val TARGET_URI = "targetUri"
        private val wrongParameters = listOf(
            "",
            "something wrong",
            generateAppDataString(ChatUpload),
            generateAppDataString(TextFileUpload, Create.toString()), //missing mandatory field
            "$TextFileUpload$Create${APP_DATA_INDICATOR}true", // missing APP_DATA_INDICATOR
            generateAppDataString(TextFileUpload, Create.toString(), "treu"), //wrong value
            "$SDCardDownload$APP_DATA_INDICATOR", //missing fields
        )

        internal val correctParameters = listOf(
            generateAppDataString(ChatUpload, FAKE_ID.toString())
                    to listOf(TransferAppData.ChatUpload(FAKE_ID)),
            generateAppDataString(VoiceClip)
                    to listOf(TransferAppData.VoiceClip),
            generateAppDataString(CameraUpload)
                    to listOf(TransferAppData.CameraUpload),
            generateAppDataString(SDCardDownload, TARGET_PATH)
                    to listOf(TransferAppData.SdCardDownload(TARGET_PATH, null)),
            generateAppDataString(BackgroundTransfer)
                    to listOf(TransferAppData.BackgroundTransfer),
            generateAppDataString(SDCardDownload, TARGET_PATH, TARGET_URI)
                    to listOf(TransferAppData.SdCardDownload(TARGET_PATH, TARGET_URI)),
            generateAppDataString(TextFileUpload, Create.toString(), "true")
                    to listOf(
                TransferAppData.TextFileUpload(
                    TransferAppData.TextFileUpload.Mode.Create,
                    true
                )
            ),
            generateAppDataString(TextFileUpload, Edit.toString(), "false")
                    to listOf(
                TransferAppData.TextFileUpload(
                    TransferAppData.TextFileUpload.Mode.Edit,
                    false
                )
            )

        )

        private fun generateAppDataString(
            type: AppDataTypeConstants,
            vararg values: String,
        ) = type.toString() +
                (values.takeIf {
                    it.isNotEmpty()
                }?.joinToString(
                    prefix = APP_DATA_INDICATOR,
                    separator = APP_DATA_INDICATOR
                ) ?: "")

        private fun joinParameters(separator: String): List<Arguments> {

            val wrongWithResult = wrongParameters.map { it to emptyList<TransferAppData>() }
            val reducer =
                { acc: Pair<String, List<TransferAppData>>, pair: Pair<String, List<TransferAppData>> ->
                    acc.first + separator + pair.first to acc.second + pair.second
                }
            val joinedCorrect = correctParameters.reduce(reducer)
            val joinedWrong = wrongWithResult.reduce(reducer)
            val joinedMix = (wrongWithResult + correctParameters).reduce(reducer)
            return listOf(
                Arguments.of(joinedCorrect.first, joinedCorrect.second),
                Arguments.of(joinedWrong.first, joinedWrong.second),
                Arguments.of(joinedMix.first, joinedMix.second),
            )
        }
    }
}