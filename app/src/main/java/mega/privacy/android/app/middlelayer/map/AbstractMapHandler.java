package mega.privacy.android.app.middlelayer.map;

import android.content.Context;
import android.graphics.Bitmap;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.MapAddress;
import mega.privacy.android.app.lollipop.megachat.MapsActivity;

public abstract class AbstractMapHandler implements MapHandler {

    private MapsActivity activity;

    protected Bitmap fullscreenIconMarker;

    public AbstractMapHandler(MapsActivity activity, Bitmap fullscreenIconMarker) {
        this.activity = activity;
        this.fullscreenIconMarker = fullscreenIconMarker;
    }

    protected boolean isFullScreen() {
        return activity.isFullScreenEnabled();
    }

    protected void setAnimatingMarker(long duration) {
        activity.setAnimatingMarker(duration);
    }

    protected void setAddress(MapAddress fullScreenAddress) {
        activity.setActivityResult(fullScreenAddress);
    }

    protected void enableLocationUpdates() {
        activity.enableLocationUpdates();
    }

    protected boolean isGPSEnabled() {
        return activity.isGPSEnabled();
    }

    protected void askForGPS() {
        activity.showEnableLocationDialog();
    }

    protected void setCurrentLocationVisibility() {
        activity.setCurrentLocationVisibility();
    }

    protected Context getContext() {
        return activity.getApplicationContext();
    }

    protected void sendSnapshot(byte[] byteArray, double latitude, double longitude) {
        activity.onSnapshotReady(byteArray, latitude, longitude);
    }

    protected void dismissProgress() {
        activity.dismissProgressBar();
    }

    protected boolean getInitResult() {
        return activity.onInit();
    }

    protected void setFullScreen() {
        activity.setFullScreen();
    }

    protected void setMyLocationAnimate() {
        activity.setMyLocationAnimateCamera();
    }

    protected void showError() {
        activity.showError();
    }

    protected void onGetLastLocation(double lati, double longi) {
        activity.onGetLastLocation(lati, longi);
    }

    protected void showIconShadow() {
        activity.showMarkerIconShadow();
    }

    protected void showMarker() {
        activity.showMarker();
    }

    protected String getInfoTitle() {
        return activity.getString(R.string.title_marker_maps);
    }
}
