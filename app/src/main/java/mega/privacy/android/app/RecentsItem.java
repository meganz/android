package mega.privacy.android.app;

import android.content.Context;

import mega.privacy.android.app.utils.TimeUtils;
import nz.mega.sdk.MegaRecentActionBucket;

public class RecentsItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_BUCKET = 1;

    private int viewType;
    private MegaRecentActionBucket bucket = null;
    private String date = "";
    private String time = "";

    public RecentsItem(Context context, MegaRecentActionBucket bucket) {
        setViewType(TYPE_BUCKET);
        setBucket(bucket);
        setDate(TimeUtils.formatBucketDate(context, bucket.getTimestamp()));
        setTime(TimeUtils.formatTime(bucket.getTimestamp()));
    }

    public RecentsItem(String date) {
        setViewType(TYPE_HEADER);
        setDate(date);
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public MegaRecentActionBucket getBucket() {
        return bucket;
    }

    public void setBucket(MegaRecentActionBucket bucket) {
        this.bucket = bucket;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
