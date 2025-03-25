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
    BackgroundTransfer("BACKGROUND_TRANSFER", TransferAppData.BackgroundTransfer::class),
    OriginalContentUri("ORIGINAL_URI", TransferAppData.OriginalContentUri::class),
    ChatDownload("CHAT_DOWNLOAD", TransferAppData.ChatDownload::class),
    GeoLocation("GEO_LOCATION", TransferAppData.Geolocation::class),
    TransferGroup("TRANSFER_GROUP", TransferAppData.TransferGroup::class),
    PreviewDownload("PREVIEW_DOWNLOAD", TransferAppData.PreviewDownload::class),
    OfflineDownload("OFFLINE_DOWNLOAD", TransferAppData.OfflineDownload::class),
    ;

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