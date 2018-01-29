package mega.privacy.android.app.components.scrollBar.viewprovider;

public interface ViewBehavior {
    void onHandleGrabbed();
    void onHandleReleased();
    void onScrollStarted();
    void onScrollFinished();
}