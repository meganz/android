package mega.privacy.android.app.middlelayer.iab;


/**
 * Generic SKU object, used to unify corresponding platform dependent purchase object.
 *
 * In HMS, it's ProductInfo.
 * In GMS, it's SkuDetails.
 */
public class MegaSku {

    /**
     * SKU of the product.
     */
    private String sku;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
