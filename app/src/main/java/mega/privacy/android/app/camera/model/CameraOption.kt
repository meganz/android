package mega.privacy.android.app.camera.model

internal enum class CameraOption {
    Photo,
    Video;

    val inverse: CameraOption
        get() = when (this) {
            Photo -> Video
            Video -> Photo
        }
}
