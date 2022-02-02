package mega.privacy.android.app.interfaces

interface ChatRoomToolbarBottomSheetDialogActionListener {

    fun sendFileFromCloudDrive()

    fun sendFileFromFileSystem()

    fun startCall(videoOn: Boolean)

    fun scanDocument()

    fun takePicture()

    fun sendVoiceClip()

    fun sendGIF()

    fun sendLocation()

    fun sendContact()
}