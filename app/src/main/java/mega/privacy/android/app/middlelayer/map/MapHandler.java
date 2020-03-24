package mega.privacy.android.app.middlelayer.map;

public interface MapHandler {

    int MAX_SIZE = 45000;

    int SNAPSHOT_SIZE = 750;

    int RADIUS = 500;

    float DEFAULT_ZOOM = 18f;

    boolean isMapNull();

    void clearMap();

    void setMyLocationEnabled(boolean enable);

    void createSnapshot(double lati, double longi, int mapWidth);

    void initMap();

    void setMyLocation(boolean animateCamera);

    void removeMarker();

    void getMarkerInfo();

    void displayFullScreenMarker();

    boolean hideMarker();
}
