package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferAppData.TextFileUpload.Mode

internal enum class TextFileModeConstants(
    val sdkFileModeValue: String,
    val textFileDataMode: Mode,
) {
    Create("CREATE_MODE", Mode.Create),
    Edit("EDIT_MODE", Mode.Edit),
    View("VIEW_MODE", Mode.View);

    override fun toString() = sdkFileModeValue

    companion object {

        private val constantsDictionary by lazy { values().associateBy { it.sdkFileModeValue } }
        private val modesDictionary by lazy { values().associateBy { it.textFileDataMode } }

        fun getFileDataModeFromSdkValue(sdkTypeConstant: String?) =
            constantsDictionary[sdkTypeConstant]?.textFileDataMode

        fun getSdkValueFromFileDataMode(mode: Mode) =
            modesDictionary[mode]?.sdkFileModeValue
    }
}