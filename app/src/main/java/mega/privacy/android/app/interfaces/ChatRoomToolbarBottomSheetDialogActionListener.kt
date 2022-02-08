package mega.privacy.android.app.interfaces

/*
 * This interface is to define what methods should
 * implement when having ChatRoomToolbarBottomSheetDialogFragment
 */
interface ChatRoomToolbarBottomSheetDialogActionListener {

    fun takePicture()

    fun showGallery()

    fun sendFile()

    fun startCall(videoOn: Boolean)

    fun scanDocument()

    fun sendGIF()

    fun sendLocation()

    fun sendContact()
}