package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.BackgroundTransfer
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.CameraUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.ChatDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.ChatUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.GeoLocation
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.OriginalContentUri
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.PreviewDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.OfflineDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.SDCardDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.TransferGroup
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.VoiceClip
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
                targetPathForSDK = "/storage/48F8-4FB7/Download mega",
                finalTargetUri = "content://com.android.externalstorage.documents/tree/48F8-4FB7%3ADownload%20mega"
            )
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }

        @Test
        fun `test that the sd card paths are mapped correctly when parentPath is received as param`() {
            val raw =
                "SD_CARD_DOWNLOAD>/storage/48F8-4FB7/Download mega>content://com.android.externalstorage.documents/tree/48F8-4FB7%3ADownload%20mega"
            val parentPath = "path/parentPath"
            val expected = TransferAppData.SdCardDownload(
                targetPathForSDK = "/storage/48F8-4FB7/Download mega",
                finalTargetUri = "content://com.android.externalstorage.documents/tree/48F8-4FB7%3ADownload%20mega",
                parentPath = parentPath
            )
            Truth.assertThat(underTest(appDataRaw = raw, parentPath = parentPath))
                .containsExactly(expected)
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

        @Test
        fun `test that a OriginalContentUri is mapped correctly`() {
            val uri = "content://com.android.externalstorage.documents/tree/primary%3A"
            val raw = "ORIGINAL_URI>$uri"
            val expected = TransferAppData.OriginalContentUri(uri)
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }

        @Test
        fun `test that a ChatDownload is mapped correctly`() {
            val chatId = 4252345L
            val msgId = 454L
            val msgIndex = 0
            val raw = "CHAT_DOWNLOAD>$chatId>$msgId>$msgIndex"
            val expected = TransferAppData.ChatDownload(chatId, msgId, msgIndex)
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }

        @Test
        fun `test that a Geolocation is mapped correctly`() {
            val raw = "GEO_LOCATION>$LAT>$LON"
            val expected = TransferAppData.Geolocation(LAT, LON)
            Truth.assertThat(underTest(raw)).containsExactly(expected)
        }


        @Test
        fun `test that a TransferGroup is mapped correctly`() {
            val raw = "TRANSFER_GROUP>$GROUP_ID"
            val expected = TransferAppData.TransferGroup(GROUP_ID)
            Truth.assertThat(underTest(raw)).containsExactly(expected)
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
        private const val FAKE_ID_2 = 23456L
        private const val FAKE_INDEX = 1
        private const val PARENT_PATH = "parentPath"
        private const val TARGET_PATH = "target"
        private const val TARGET_URI = "targetUri"
        private const val LAT = 41.60
        private const val LON = 2.28
        private const val GROUP_ID = 4567L
        private val wrongParameters = listOf(
            "",
            "something wrong",
            generateAppDataString(ChatUpload),
            "$SDCardDownload$APP_DATA_INDICATOR", //missing fields
        )

        internal val correctParameters = listOf(
            generateAppDataString(ChatUpload, FAKE_ID.toString())
                    to listOf(TransferAppData.ChatUpload(FAKE_ID)),
            generateAppDataString(VoiceClip)
                    to listOf(TransferAppData.VoiceClip),
            generateAppDataString(CameraUpload)
                    to listOf(TransferAppData.CameraUpload),
            generateAppDataString(SDCardDownload, TARGET_PATH, TARGET_URI)
                    to listOf(
                TransferAppData.SdCardDownload(
                    targetPathForSDK = TARGET_PATH,
                    finalTargetUri = TARGET_URI
                )
            ),
            generateAppDataString(BackgroundTransfer)
                    to listOf(TransferAppData.BackgroundTransfer),
            generateAppDataString(SDCardDownload, TARGET_PATH, TARGET_URI, PARENT_PATH)
                    to listOf(
                TransferAppData.SdCardDownload(
                    targetPathForSDK = TARGET_PATH,
                    finalTargetUri = TARGET_URI,
                    parentPath = PARENT_PATH
                )
            ),
            generateAppDataString(OriginalContentUri, TARGET_URI)
                    to listOf(TransferAppData.OriginalContentUri(TARGET_URI)),
            generateAppDataString(
                ChatDownload,
                FAKE_ID.toString(),
                FAKE_ID_2.toString(),
                FAKE_INDEX.toString()
            ) to listOf(TransferAppData.ChatDownload(FAKE_ID, FAKE_ID_2, FAKE_INDEX)),
            generateAppDataString(
                GeoLocation,
                LAT.toString(), LON.toString()
            ) to listOf(TransferAppData.Geolocation(LAT, LON)),
            generateAppDataString(
                TransferGroup,
                GROUP_ID.toString()
            ) to listOf(TransferAppData.TransferGroup(GROUP_ID)),
            generateAppDataString(PreviewDownload)
                    to listOf(TransferAppData.PreviewDownload),
            generateAppDataString(OfflineDownload)
                    to listOf(TransferAppData.OfflineDownload),
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