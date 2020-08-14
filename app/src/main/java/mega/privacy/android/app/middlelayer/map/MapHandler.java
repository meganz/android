package mega.privacy.android.app.middlelayer.map;

/**
 * Defines methods for hanlding Map related functions.
 */
public interface MapHandler {

    int MAX_SIZE = 45000;

    int SNAPSHOT_SIZE = 750;

    int RADIUS = 500;

    float DEFAULT_ZOOM = 18f;

    /**
     * Check if current map object is null.
     * @return true if map object is null, otherwise false.
     */
    boolean isMapNull();

    /**
     * Clear map.
     */
    void clearMap();

    /**
     *
     * @param enable If enable my location.
     */
    void setMyLocationEnabled(boolean enable);

    /**
     * Create a snapshot image for current location.
     *
     * @param lati Latitude of current location.
     * @param longi Longitude of current location.
     * @param mapWidth The size of snapshot image.
     */
    void createSnapshot(double lati, double longi, int mapWidth);

    /**
     * Init platform dependent map object.
     */
    void initMap();

    /**
     * Move to current location.
     *
     * @param animateCamera Use camera moving animation if true, otherwise if false.
     */
    void setMyLocation(boolean animateCamera);

    /**
     * Remove added location marker.
     */
    void removeMarker();

    /**
     * Get location marker info.
     */
    void getMarkerInfo();

    /**
     * Display fullscreen marker.
     */
    void displayFullScreenMarker();

    /**
     * Hide marker.
     *
     * @return true if successful, otherwise false.
     */
    boolean hideMarker();
}
