package mega.privacy.android.app;

import android.content.Context;
import android.os.StatFs;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;

public class VideoCompressor {

    private VideoDownsampling compressor;

    private List<SyncRecord> pendingList;

    private String outputRoot;

    private VideoCompressionCallback updater;

    private long totalInputSize;

    private long totalRead;

    private int totalCount;

    private int currentFileIndex;

    private boolean isRunning;

    public void stop() {
        isRunning = false;
        log("video compressor stopped");
    }

    public VideoCompressor(Context context, VideoCompressionCallback callback) {
        this.updater = callback;
        compressor = new VideoDownsampling(context);
    }

    public void setPendingList(List<SyncRecord> pendingList) {
        this.pendingList = pendingList;
        totalCount = pendingList.size();
        log("total compression videos count is " + totalCount);
        calculateTotalSize();
    }

    private void calculateTotalSize() {
        for (int i = 0; i < totalCount; i++) {
            totalInputSize += new File(pendingList.get(i).getLocalPath()).length();
        }
        log("total compression size is " + totalInputSize);
    }

    public long getTotalInputSize() {
        return this.totalInputSize;
    }

    public void setOutputRoot(String root) {
        this.outputRoot = root;
    }

    private boolean notEnoughSpace(long size) {
        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(outputRoot);
            availableFreeSpace = stat.getAvailableBytes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (size > availableFreeSpace);
    }

    public void start() {
        isRunning = true;
        for (int i = 0; i < totalCount && isRunning; i++) {
            currentFileIndex = i + 1;
            SyncRecord record = pendingList.get(i);
            log("video compressor start: " + record.toString());
            String path = record.getLocalPath();
            File src = new File(path);
            long size = src.length();
            if (notEnoughSpace(size)) {
                updater.onInsufficientSpace();
                return;
            }

            VideoDownsampling.VideoUpload video = new VideoDownsampling.VideoUpload(path, record.getNewPath(), size, -1);
            try {
                compressor.prepareAndChangeResolution(video);
                if (isRunning) {
                    updater.onCompressSuccessful(record);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                log(ex.getMessage());
                updater.onCompressFailed(record);
                currentFileIndex++;
                totalRead += size;
            }
        }
        updater.onCompressFinished(totalCount + "/" + totalCount);
        stop();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCurrentFileIndex() {
        return currentFileIndex;
    }

    private static void log(String message) {
        Util.log("VideoCompressor", message);
    }
}
