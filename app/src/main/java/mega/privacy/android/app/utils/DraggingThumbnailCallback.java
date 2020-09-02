package mega.privacy.android.app.utils;

public interface DraggingThumbnailCallback {
    int DRAGGING_THUMBNAIL_CALLBACKS_SIZE = 4;

    void setVisibility(int visibility);

    void getLocationOnScreen(int[] location);
}
