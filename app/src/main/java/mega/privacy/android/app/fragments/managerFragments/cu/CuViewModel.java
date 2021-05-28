package mega.privacy.android.app.fragments.managerFragments.cu;

import android.content.Context;
import android.os.Environment;
import android.util.Pair;

import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.arch.BaseRxViewModel;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.listeners.BaseListener;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.utils.Constants.GET_THUMBNAIL_THROTTLE_MS;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.PreviewUtils.getPreviewFolder;
import static mega.privacy.android.app.utils.RxUtil.IGNORE;
import static mega.privacy.android.app.utils.RxUtil.logErr;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder;
import static mega.privacy.android.app.utils.Util.fromEpoch;

class CuViewModel extends BaseRxViewModel {
    private final MegaApiAndroid mMegaApi;
    private final DatabaseHandler mDbHandler;
    private final MegaNodeRepo mRepo;
    private final Context mAppContext;
    private final SortOrderManagement mSortOrderManagement;

    private final MutableLiveData<Boolean> camSyncEnabled = new MutableLiveData<>();
    private final MutableLiveData<List<Pair<CUCard, CuNode>>> dayCards = new MutableLiveData<>();
    private final MutableLiveData<List<Pair<CUCard, CuNode>>> monthCards = new MutableLiveData<>();
    private final MutableLiveData<List<Pair<CUCard, CuNode>>> yearCards = new MutableLiveData<>();
    private final MutableLiveData<List<CuNode>> mCuNodes = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToOpen = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToAnimate = new MutableLiveData<>();
    private final MutableLiveData<String> mActionBarTitle = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mActionMode = new MutableLiveData<>();

    private final Subject<Pair<Integer, CuNode>> mOpenNodeAction = PublishSubject.create();
    private final Subject<Object> mCreatingThumbnailFinished = PublishSubject.create();
    private final Subject<Object> creatingPreviewFinished = PublishSubject.create();

    private final MegaRequestListenerInterface mCreateThumbnailRequest;
    private final MegaRequestListenerInterface createPreviewRequest;
    private final LongSparseArray<MegaNode> mSelectedNodes = new LongSparseArray<>(5);
    private boolean mSelecting;
    private int mRealNodeCount;
    private boolean enableCUShown;

    public boolean isEnableCUShown() {
        return enableCUShown;
    }

    public void setEnableCUShown(boolean shown) {
        enableCUShown = shown;
    }

    @Inject
    public CuViewModel(@MegaApi MegaApiAndroid megaApi, DatabaseHandler dbHandler,
                       MegaNodeRepo repo, Context context, SortOrderManagement sortOrderManagement) {
        mMegaApi = megaApi;
        mDbHandler = dbHandler;
        mRepo = repo;
        mAppContext = context.getApplicationContext();
        mSortOrderManagement = sortOrderManagement;
        mCreateThumbnailRequest = new BaseListener(mAppContext) {
            @Override
            public void onRequestFinish(@NotNull MegaApiJava api, @NotNull MegaRequest request, @NotNull MegaError e) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    mCreatingThumbnailFinished.onNext(true);
                }
            }
        };

        createPreviewRequest = new BaseListener(mAppContext) {
            @Override
            public void onRequestFinish(@NotNull MegaApiJava api, @NotNull MegaRequest request, @NotNull MegaError e) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    creatingPreviewFinished.onNext(true);
                }
            }
        };

        loadCuNodes();

        add(mOpenNodeAction.throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mNodeToOpen::setValue, logErr("openNodeAction")));

        add(mCreatingThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> loadCuNodes(), logErr("creatingThumbnailFinished")));

        add(creatingPreviewFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> loadCuNodes(), logErr("creatingPreviewFinished")));
    }

    public LiveData<List<Pair<CUCard, CuNode>>> getDayCardsData() {
        return dayCards;
    }

    public LiveData<List<Pair<CUCard, CuNode>>> getMonthCardsData() {
        return monthCards;
    }

    public LiveData<List<Pair<CUCard, CuNode>>> getYearCardsData() {
        return yearCards;
    }

    public List<Pair<CUCard, CuNode>> getDayCards() {
        return dayCards.getValue();
    }

    public List<Pair<CUCard, CuNode>> getMonthCards() {
        return monthCards.getValue();
    }

    public List<Pair<CUCard, CuNode>> getYearCards() {
        return yearCards.getValue();
    }

    public boolean isCUEnabled() {
        return camSyncEnabled != null && camSyncEnabled.getValue();
    }

    public LiveData<List<CuNode>> cuNodes() {
        return mCuNodes;
    }

    public List<CuNode> getCUNodes() {
        return mCuNodes.getValue();
    }

    public LiveData<Pair<Integer, CuNode>> nodeToOpen() {
        return mNodeToOpen;
    }

    public LiveData<Pair<Integer, CuNode>> nodeToAnimate() {
        return mNodeToAnimate;
    }

    public LiveData<String> actionBarTitle() {
        return mActionBarTitle;
    }

    public LiveData<Boolean> actionMode() {
        return mActionMode;
    }

    public void loadCuNodes() {
        loadNodes(Single.defer(() -> Single.just(getCuNodes())));
    }

    public long[] getSearchResultNodeHandles() {
        List<CuNode> nodes = mCuNodes.getValue();
        if (nodes == null || nodes.isEmpty()) {
            return new long[0];
        }

        List<Long> handleList = new ArrayList<>();
        for (CuNode node : nodes) {
            if (node.getNode() != null) {
                handleList.add(node.getNode().getHandle());
            }
        }

        long[] handles = new long[handleList.size()];
        for (int i = 0, n = handleList.size(); i < n; i++) {
            handles[i] = handleList.get(i);
        }
        return handles;
    }

    public boolean isSelecting() {
        return mSelecting;
    }

    /**
     * Handle node click & long click event.
     * <p>
     * In selection mode, we need need animate the selection icon, so we don't
     * trigger nodes update through {@code cuNodes.setValue(nodes); }, we only
     * update node's selected property here, for consistency.
     *
     * @param position clicked node position in RV
     * @param node     clicked node
     */
    public void onNodeClicked(int position, CuNode node) {
        if (mSelecting) {
            List<CuNode> nodes = mCuNodes.getValue();
            if (nodes == null || position < 0 || position >= nodes.size()
                    || nodes.get(position).getNode() == null
                    || nodes.get(position).getNode().getHandle() != node.getNode().getHandle()) {
                return;
            }

            nodes.get(position).setSelected(!nodes.get(position).isSelected());
            if (nodes.get(position).isSelected()) {
                mSelectedNodes.put(node.getNode().getHandle(), node.getNode());
            } else {
                mSelectedNodes.remove(node.getNode().getHandle());
            }
            mSelecting = !mSelectedNodes.isEmpty();
            mActionMode.setValue(mSelecting);

            mNodeToAnimate.setValue(Pair.create(position, node));
        } else {
            mOpenNodeAction.onNext(Pair.create(position, node));
        }
    }

    public void onNodeLongClicked(int position, CuNode node) {
        mSelecting = true;
        onNodeClicked(position, node);
    }

    public List<MegaNode> getSelectedNodes() {
        List<MegaNode> nodes = new ArrayList<>();
        for (int i = 0, n = mSelectedNodes.size(); i < n; i++) {
            nodes.add(mSelectedNodes.valueAt(i));
        }

        return nodes;
    }

    public int getSelectedNodesCount() {
        return mSelectedNodes.size();
    }

    public int getRealNodesCount() {
        return mRealNodeCount;
    }

    public void selectAll() {
        List<CuNode> nodes = mCuNodes.getValue();
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            CuNode node = nodes.get(i);
            if (node.getNode() != null) {
                if (!node.isSelected()) {
                    mNodeToAnimate.setValue(Pair.create(i, node));
                }
                node.setSelected(true);
                mSelectedNodes.put(node.getNode().getHandle(), node.getNode());
            }
        }

        mSelecting = true;
        mCuNodes.setValue(nodes);
        mActionMode.setValue(true);
    }

    public void clearSelection() {
        if (mSelecting) {
            mSelecting = false;
            mActionMode.setValue(false);

            List<CuNode> nodes = mCuNodes.getValue();
            if (nodes == null || nodes.isEmpty()) {
                return;
            }

            for (int i = 0; i < nodes.size(); i++) {
                CuNode node = nodes.get(i);
                if (node.isSelected()) {
                    mNodeToAnimate.setValue(Pair.create(i, node));
                }
                node.setSelected(false);
            }
            mSelectedNodes.clear();

            mCuNodes.setValue(nodes);
        }
    }

    public void setInitialPreferences() {
        add(Completable.fromCallable(
                () -> {
                    logDebug("setInitialPreferences");

                    mDbHandler.setFirstTime(false);
                    mDbHandler.setStorageAskAlways(true);
                    File defaultDownloadLocation = buildDefaultDownloadDir(mAppContext);
                    defaultDownloadLocation.mkdirs();

                    mDbHandler.setStorageDownloadLocation(
                            defaultDownloadLocation.getAbsolutePath());
                    mDbHandler.setPasscodeLockEnabled(false);
                    mDbHandler.setPasscodeLockCode("");

                    ArrayList<MegaNode> nodeLinks = mMegaApi.getPublicLinks();
                    if (nodeLinks == null || nodeLinks.size() == 0) {
                        logDebug("No public links: showCopyright set true");
                        mDbHandler.setShowCopyright(true);
                    } else {
                        logDebug("Already public links: showCopyright set false");
                        mDbHandler.setShowCopyright(false);
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("setInitialPreferences")));
    }

    public void setCamSyncEnabled(boolean enabled) {
        add(Completable.fromCallable(
                () -> {
                    mDbHandler.setCamSyncEnabled(enabled);
                    return enabled;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("setCamSyncEnabled")));
    }

    public void enableCu(boolean enableCellularSync, boolean syncVideo) {
        add(Completable.fromCallable(
                () -> {
                    File localFile =
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DCIM);
                    mDbHandler.setCamSyncLocalPath(localFile.getAbsolutePath());
                    mDbHandler.setCameraFolderExternalSDCard(false);
                    mDbHandler.setCamSyncWifi(!enableCellularSync);
                    mDbHandler.setCamSyncFileUpload(
                            syncVideo ? MegaPreferences.PHOTOS_AND_VIDEOS
                                    : MegaPreferences.ONLY_PHOTOS);

                    mDbHandler.setCameraUploadVideoQuality(MEDIUM);
                    mDbHandler.setConversionOnCharging(true);
                    mDbHandler.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
                    // After target and local folder setup, then enable CU.
                    mDbHandler.setCamSyncEnabled(true);
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("enableCu")));
    }

    public LiveData<Boolean> camSyncEnabled() {
        add(Single.fromCallable(
                () -> Boolean.parseBoolean(mDbHandler.getPreferences().getCamSyncEnabled()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(camSyncEnabled::setValue, logErr("camSyncEnabled")));

        return camSyncEnabled;
    }

    private void loadNodes(Single<List<CuNode>> source) {
        add(source.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCuNodes::setValue, logErr("loadCuNodes")));
    }

    private List<CuNode> getCuNodes() {
        List<Pair<CUCard, CuNode>> days = new ArrayList<>();
        List<Pair<CUCard, CuNode>> months = new ArrayList<>();
        List<Pair<CUCard, CuNode>> years = new ArrayList<>();
        List<CuNode> nodes = new ArrayList<>();
        List<MegaNode> nodesWithoutThumbnail = new ArrayList<>();
        List<MegaNode> nodesWithoutPreview = new ArrayList<>();
        long dayItemsCount = 0;
        LocalDate lastDayDate = null;
        LocalDate lastMonthDate = null;
        LocalDate lastYearDate = null;
        List<Pair<Integer, MegaNode>> realNodes =
                mRepo.getCuChildren(mSortOrderManagement.getOrderCamera(), null);

        for (Pair<Integer, MegaNode> pair : realNodes) {
            MegaNode node = pair.second;
            boolean shouldGetPreview = false;
            File thumbnail = new File(getThumbFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
            File preview = new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
            LocalDate modifyDate = fromEpoch(node.getModificationTime());
            String day = ofPattern("dd").format(modifyDate);
            String month = ofPattern("MMM").format(modifyDate);
            String year = ofPattern("yyyy").format(modifyDate);
            String dateString = ofPattern("MMM yyyy").format(modifyDate);
            boolean sameYear = Year.from(LocalDate.now()).equals(Year.from(modifyDate));
            CuNode cuNode = new CuNode(node, pair.first,
                    thumbnail.exists() ? thumbnail : null,
                    preview.exists() ? preview : null,
                    isVideoFile(node.getName()) ? CuNode.TYPE_VIDEO : CuNode.TYPE_IMAGE,
                    dateString,
                    mSelectedNodes.containsKey(node.getHandle()));

            dayItemsCount++;

            if (lastDayDate == null || lastDayDate.getDayOfYear() != modifyDate.getDayOfYear()) {
                lastDayDate = modifyDate;
                String date = ofPattern(sameYear ? "dd MMM" : "dd MMM yyyy").format(lastDayDate);
                days.add(new Pair<>(new CUCard(day, month, year, date, null), cuNode));

                int daysSize = days.size();
                if (daysSize > 1) {
                    days.get(daysSize - 2).first.setNumItems(dayItemsCount);
                }

                dayItemsCount = 0;
                shouldGetPreview = true;
            }

            if (lastMonthDate == null
                    || !YearMonth.from(lastMonthDate).equals(YearMonth.from(modifyDate))) {
                lastMonthDate = modifyDate;
                nodes.add(new CuNode(dateString, new Pair<>(month, sameYear ? "" : year)));
                String date = sameYear ? month : ofPattern("MMM yyyy").format(modifyDate);
                months.add(new Pair<>(new CUCard(null, month, year, date, null), cuNode));
                shouldGetPreview = true;
            }

            if (lastYearDate == null || !Year.from(lastYearDate).equals(Year.from(modifyDate))) {
                lastYearDate = modifyDate;
                years.add(new Pair<>(new CUCard(null, null, year, year, null), cuNode));
                shouldGetPreview = true;
            }

            nodes.add(cuNode);

            if (!thumbnail.exists()) {
                nodesWithoutThumbnail.add(node);
            }

            if (shouldGetPreview && !preview.exists()) {
                nodesWithoutPreview.add(node);
            }
        }

        mRealNodeCount = realNodes.size();

        // Fetch thumbnails of nodes in computation thread, fetching each in 50ms interval
        add(Observable.fromIterable(nodesWithoutThumbnail)
                .zipWith(Observable.interval(GET_THUMBNAIL_THROTTLE_MS, MILLISECONDS),
                        (node, interval) -> node)
                .observeOn(Schedulers.computation())
                .subscribe(node -> {
                    File thumbnail =
                            new File(getThumbFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
                    mMegaApi.getThumbnail(node, thumbnail.getAbsolutePath(), mCreateThumbnailRequest);
                }, logErr("CuViewModel getThumbnail")));


        add(Observable.fromIterable(nodesWithoutPreview)
                .zipWith(Observable.interval(GET_THUMBNAIL_THROTTLE_MS, MILLISECONDS),
                        (node, interval) -> node)
                .observeOn(Schedulers.computation())
                .subscribe(node -> {
                    File preview = new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
                    mMegaApi.getPreview(node, preview.getAbsolutePath(), createPreviewRequest);
                }, logErr("CuViewModel getPreview")));

        dayCards.postValue(days);
        monthCards.postValue(months);
        yearCards.postValue(years);

        return nodes;
    }

    private String getSearchDateTitle(long[] filter) {
        if (filter == null) {
            return "";
        }

        if (filter[0] == 1) {
            return ofPattern("d MMM").format(fromEpoch(filter[1] / 1000));
        } else if (filter[0] == 2) {
            if (filter[2] == 1) {
                return ofPattern("MMMM").format(YearMonth.now().minusMonths(1));
            } else if (filter[2] == 2) {
                return String.valueOf(YearMonth.now().getYear() - 1);
            } else {
                return "";
            }
        } else if (filter[0] == 3) {
            DateTimeFormatter formatter = ofPattern("d MMM");
            LocalDate from = fromEpoch(filter[3] / 1000);
            LocalDate to = fromEpoch(filter[4] / 1000);
            return formatter.format(from) + " - " + formatter.format(to);
        } else {
            return "";
        }
    }
}
