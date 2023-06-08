package mega.privacy.android.data.mapper.transfer

internal enum class AppDataTypeConstants(val sdkTypeValue: String) {
    VoiceClip("VOICE_CLIP"),
    ChatUpload("CHAT_UPLOAD"),
    CameraUpload("CU_UPLOAD"),
    SDCardDownload("SD_CARD_DOWNLOAD"),
    TextFileUpload("TXT_FILE_UPLOAD"),
    BackgroundTransfer("BACKGROUND_TRANSFER");

    override fun toString() = sdkTypeValue

    companion object {

        private val constantsDictionary by lazy { values().associateBy { it.sdkTypeValue } }
        fun getTypeFromSdkValue(sdkTypeConstant: String) =
            constantsDictionary[sdkTypeConstant]
    }
}