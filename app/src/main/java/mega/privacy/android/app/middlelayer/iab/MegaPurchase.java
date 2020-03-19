package mega.privacy.android.app.middlelayer.iab;


public class MegaPurchase {

    private String sku;

    private String receipt;

    private String userHandle;

    private int state;

    private String token;

    public String getSku() {
        return sku;
    }

    public String getReceipt() {
        return receipt;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public int getState() {
        return state;
    }

    public String getToken() {
        return token;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
