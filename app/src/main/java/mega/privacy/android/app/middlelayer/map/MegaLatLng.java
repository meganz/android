package mega.privacy.android.app.middlelayer.map;

/**
 * Generic coordinate object, used to unify corresponding platform dependent purchase object.
 */
public class MegaLatLng {

    private double latitude;

    private double longitude;

    public MegaLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "MegaLatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
