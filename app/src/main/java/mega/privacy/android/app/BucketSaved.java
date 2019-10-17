package mega.privacy.android.app;

import java.io.Serializable;

import nz.mega.sdk.MegaRecentActionBucket;

public class BucketSaved implements Serializable {

    private long timestamp;
    private String userMail;
    private long parentHandle;
    private boolean isMedia;
    private boolean isUpdate;


    public BucketSaved(MegaRecentActionBucket bucket) {
        this.timestamp = bucket.getTimestamp();
        this.userMail = bucket.getUserEmail();
        this.parentHandle = bucket.getParentHandle();
        this.isMedia = bucket.isMedia();
        this.isUpdate = bucket.isUpdate();
    }

    public boolean isTheSameBucket(MegaRecentActionBucket bucket) {

        if (bucket.getTimestamp() == this.getTimestamp()
                && bucket.getUserEmail().equals(this.getUserMail())
                && bucket.getParentHandle() == this.getParentHandle()
                && bucket.isMedia() == this.isMedia()
                && bucket.isUpdate() == this.isUpdate()) {
            return true;
        }

        return false;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserMail() {
        return userMail;
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public boolean isMedia() {
        return isMedia;
    }

    public boolean isUpdate() {
        return isUpdate;
    }
}
