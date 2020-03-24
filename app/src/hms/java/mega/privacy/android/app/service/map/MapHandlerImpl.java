package mega.privacy.android.app.service.map;

import android.graphics.Bitmap;

import mega.privacy.android.app.lollipop.megachat.MapsActivity;
import mega.privacy.android.app.middlelayer.map.AbstractMapHandler;

public class MapHandlerImpl extends AbstractMapHandler {

    public MapHandlerImpl(MapsActivity activity, Bitmap icon) {
        super(activity,icon);
    }

    @Override
    public boolean isMapNull() {
        return false;
    }

    @Override
    public void clearMap() {

    }

    @Override
    public void setMyLocationEnabled(boolean enable) {

    }

    @Override
    public void createSnapshot(double lati, double longi, int mapWidth) {

    }

    @Override
    public void initMap() {

    }

    @Override
    public void setMyLocation(boolean animateCamera) {

    }

    @Override
    public void removeMarker() {

    }

    @Override
    public void getMarkerInfo() {

    }

    @Override
    public void displayFullScreenMarker() {

    }

    @Override
    public boolean hideMarker() {
        return false;
    }
}
