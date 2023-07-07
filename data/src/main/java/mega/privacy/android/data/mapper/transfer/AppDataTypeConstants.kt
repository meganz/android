package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferAppData
import kotlin.reflect.KClass

internal enum class AppDataTypeConstants(
    val sdkTypeValue: String,
    val clazz: KClass<*>,
) {
    VoiceClip("VOICE_CLIP", TransferAppData.VoiceClip::class),
    ChatUpload("CHAT_UPLOAD", TransferAppData.ChatUpload::class),
    CameraUpload("CU_UPLOAD", TransferAppData.CameraUpload::class),
    SDCardDownload("SD_CARD_DOWNLOAD", TransferAppData.SdCardDownload::class),
    TextFileUpload("TXT_FILE_UPLOAD", TransferAppData.TextFileUpload::class),
    BackgroundTransfer("BACKGROUND_TRANSFER", TransferAppData.BackgroundTransfer::class);

    override fun toString() = sdkTypeValue

    companion object {

        private val constantsDictionary by lazy { values().associateBy { it.sdkTypeValue } }

        private val classDictionary by lazy { values().associateBy { it.clazz } }

        fun getTypeFromSdkValue(sdkTypeConstant: String) =
            constantsDictionary[sdkTypeConstant]

        fun <T : TransferAppData> getTypeFromTransferAppDataClass(clazz: KClass<T>) =
            classDictionary[clazz]
    }
}