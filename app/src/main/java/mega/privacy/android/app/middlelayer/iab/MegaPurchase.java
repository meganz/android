package mega.privacy.android.app.middlelayer.iab;

/**
 * Generic purchase object, used to unify corresponding platform dependent purchase object.
 *
 * In HMS, it's InAppPurchaseData.
 * In GMS, it's Purchase.
 */
public class MegaPurchase {

    /**
     * SKU of the product.
     */
    private String sku;

    /**
     * Receipt of the purchase, will be submitted to API.
     */
    private String receipt;

    /**
     * State of the purchase.
     */
    private int state;

    /**
     * Token of the purchase.
     */
    private String token;

    public String getSku() {
        return sku;
    }

    public String getReceipt() {
        return receipt;
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

    public void setState(int state) {
        this.state = state;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
