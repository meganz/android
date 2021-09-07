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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import mega.privacy.android.app.utils.ZoomUtil;
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
import static mega.privacy.android.app.utils.Constants.GET_PREVIEW_THROTTLE_MS;
import static mega.privacy.android.app.utils.Constants.GET_THUMBNAIL_THROTTLE_MS;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.PreviewUtils.getPreviewFolder;
import static mega.privacy.android.app.utils.RxUtil.IGNORE;
import static mega.privacy.android.app.utils.RxUtil.logErr;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder;
import static mega.privacy.android.app.utils.Util.fromEpoch;
import static nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC;

class CuViewModel extends BaseRxViewModel {
    private final MegaApiAndroid mMegaApi;
    private final DatabaseHandler mDbHandler;
    private final MegaNodeRepo mRepo;
    private final Context mAppContext;
    private final SortOrderManagement mSortOrderManagement;

    private final MutableLiveData<Boolean> camSyncEnabled = new MutableLiveData<>();
    private final MutableLiveData<List<CUCard>> dayCards = new MutableLiveData<>();
    private final MutableLiveData<List<CUCard>> monthCards = new MutableLiveData<>();
    private final MutableLiveData<List<CUCard>> yearCards = new MutableLiveData<>();
    private final MutableLiveData<List<CuNode>> mCuNodes = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToOpen = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToAnimate = new MutableLiveData<>();
    private final MutableLiveData<String> mActionBarTitle = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mActionMode = new MutableLiveData<>();

    private final Subject<Pair<Integer, CuNode>> mOpenNodeAction = PublishSubject.create();
    private final Subject<Object> mCreatingThumbnailFinished = PublishSubject.create();
    private final Subject<Object> mCreatingPreviewFinished = PublishSubject.create();
    private final Subject<Object> creatingPreviewFinished = PublishSubject.create();

    private final MegaRequestListenerInterface mCreateThumbnailRequest;
    private final MegaRequestListenerInterface mCreatePreviewRequest;
    private final MegaRequestListenerInterface createPreviewRequest;
    private final LongSparseArray<MegaNode> mSelectedNodes = new LongSparseArray<>(5);
    private boolean mSelecting;
    private int mRealNodeCount;
    private boolean enableCUShown;

    private Set<Long> thumbnailHandle = new HashSet<>();
    private Set<Long> previewHandle = new HashSet<>();

    private int mZoom;

    public boolean isEnableCUShown() {
        return enableCUShown;
    }

    public void setEnableCUShown(boolean shown) {
        enableCUShown = shown;
    }

    @Inject
    public CuViewModel(@MegaApi MegaApiAndroid megaApi, DatabaseHandler dbHandler,
                       MegaNodeRepo repo, Context context, SortOrderManagement sortOrderManagement, int zoom) {
        mMegaApi = megaApi;
        mDbHandler = dbHandler;
        mRepo = repo;
        mZoom = zoom;
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

        mCreatePreviewRequest = new BaseListener(mAppContext) {
            @Override
            public void onRequestFinish(@NotNull MegaApiJava api, @NotNull MegaRequest request, @NotNull MegaError e) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    mCreatingPreviewFinished.onNext(true);
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

        loadNodes();
        getCards();

        add(mOpenNodeAction.throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mNodeToOpen::setValue, logErr("openNodeAction")));

        add(mCreatingThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> loadNodes(), logErr("creatingThumbnailFinished")));

        add(mCreatingPreviewFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> loadNodes(), logErr("creatingPreviewFinished")));

        add(creatingPreviewFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> getCards(), logErr("creatingPreviewFinished")));
    }

    public LiveData<List<CUCard>> getDayCardsData() {
        return dayCards;
    }

    public LiveData<List<CUCard>> getMonthCardsData() {
        return monthCards;
    }

    public LiveData<List<CUCard>> getYearCardsData() {
        return yearCards;
    }

    public List<CUCard> getDayCards() {
        return dayCards.getValue();
    }

    public List<CUCard> getMonthCards() {
        return monthCards.getValue();
    }

    public List<CUCard> getYearCards() {
        return yearCards.getValue();
    }

    public boolean isCUEnabled() {
        return camSyncEnabled != null && camSyncEnabled.getValue() != null
                ? camSyncEnabled.getValue()
                : false;
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

    public boolean isSelecting() {
        return mSelecting;
    }

    public void resetOpenedNode() {
        if (mNodeToOpen != null) {
            mNodeToOpen.setValue(Pair.create(INVALID_POSITION, null));
        }
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

    public void loadNodes() {
        add(Single.fromCallable(this::getCuNodes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCuNodes::setValue, logErr("loadCuNodes")));
    }

    /**
     * Gets CU and MU content as CUNode objects.
     * Content means images and videos which are located on CU and MU folders.
     *
     * Also requests needed thumbnails to show in holders if not exist yet.
     *
     * @return The list of CUNode objects.
     */
    private List<CuNode> getCuNodes() {
        List<CuNode> nodes = new ArrayList<>();
        List<MegaNode> nodesWithoutThumbnail = new ArrayList<>();
        LocalDate lastYearDate = null;
        LocalDate lastMonthDate = null;
        LocalDate lastDayDate = null;
        List<Pair<Integer, MegaNode>> realNodes =
                mRepo.getFilteredCuChildrenAsPairs(mSortOrderManagement.getOrderCamera());

        for (Pair<Integer, MegaNode> pair : realNodes) {
            MegaNode node = pair.second;
            File thumbnail = new File(getThumbFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
            LocalDate modifyDate = fromEpoch(node.getModificationTime());
            String dateString = ofPattern("MMMM yyyy").format(modifyDate);
            boolean sameYear = Year.from(LocalDate.now()).equals(Year.from(modifyDate));
            CuNode cuNode = new CuNode(node, pair.first,
                    thumbnail.exists() ? thumbnail : null,
                    isVideoFile(node.getName()) ? CuNode.TYPE_VIDEO : CuNode.TYPE_IMAGE,
                    dateString,
                    mSelectedNodes.containsKey(node.getHandle()));

            if(mZoom == ZoomUtil.ZOOM_OUT_3X) {
                if (lastYearDate == null || !Year.from(lastYearDate).equals(Year.from(modifyDate))) {
                    lastYearDate = modifyDate;
                    String date = ofPattern("yyyy").format(modifyDate);
                    nodes.add(new CuNode(date, new Pair<>(date, "")));
                }
            } else if(mZoom == ZoomUtil.ZOOM_IN_1X) {
                if (lastDayDate == null || lastDayDate.getDayOfYear() != modifyDate.getDayOfYear()) {
                    lastDayDate = modifyDate;
                    String date = ofPattern(sameYear ? "dd MMMM" : "dd MMMM yyyy").format(lastDayDate);
                    nodes.add(new CuNode(date, new Pair<>(date, "")));
                }
                // For zoom in 1X, use preview file as thumbnail to avoid blur.
                cuNode.setmPreview(new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION));
            } else {
                if (lastMonthDate == null
                        || !YearMonth.from(lastMonthDate).equals(YearMonth.from(modifyDate))) {
                    lastMonthDate = modifyDate;
                    nodes.add(new CuNode(dateString, new Pair<>(ofPattern("MMMM").format(modifyDate),
                            sameYear ? "" : ofPattern("yyyy").format(modifyDate))));
                }
            }

            nodes.add(cuNode);

            if (!thumbnail.exists()) {
                nodesWithoutThumbnail.add(node);
            }
        }

        mRealNodeCount = realNodes.size();

        // Fetch thumbnails of nodes in computation thread, fetching each in 50ms interval
        add(Observable.fromIterable(nodesWithoutThumbnail)
                .zipWith(Observable.interval(GET_THUMBNAIL_THROTTLE_MS, MILLISECONDS),
                        (node, interval) -> node)
                .observeOn(Schedulers.computation())
                .subscribe(node -> {
                    if(!thumbnailHandle.contains(node.getHandle())) {
                        thumbnailHandle.add(node.getHandle());

                        File thumbnail =
                                new File(getThumbFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
                        if(!thumbnail.exists()) {
                            mMegaApi.getThumbnail(node, thumbnail.getAbsolutePath(), mCreateThumbnailRequest);
                        }
                    }

                }, logErr("CuViewModel getThumbnail")));

        add(Observable.fromIterable(nodesWithoutThumbnail)
                .zipWith(Observable.interval(GET_THUMBNAIL_THROTTLE_MS, MILLISECONDS),
                        (node, interval) -> node)
                .observeOn(Schedulers.computation())
                .subscribe(node -> {
                    if (!previewHandle.contains(node.getHandle())) {
                        previewHandle.add(node.getHandle());

                        File preview =
                                new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
                        if (!preview.exists()) {
                            mMegaApi.getPreview(node, preview.getAbsolutePath(), mCreatePreviewRequest);
                        }
                    }
                }, logErr("CuViewModel getPreview")));

        return nodes;
    }

    /**
     * Gets three different lists of cards, organizing CU and MU content in three different ways:
     * - Day cards:   Content organized by days.
     * - Month cards: Content organized by months.
     * - Year cards:  Content organized by years.
     *
     * Also requests needed previews to show in cards if not exist yet.
     */
    public void getCards() {
        List<CUCard> days = new ArrayList<>();
        List<CUCard> months = new ArrayList<>();
        List<CUCard> years = new ArrayList<>();
        List<MegaNode> nodesWithoutPreview = new ArrayList<>();
        LocalDate lastDayDate = null;
        LocalDate lastMonthDate = null;
        LocalDate lastYearDate = null;
        List<MegaNode> cardNodes = mRepo.getFilteredCuChildren(ORDER_MODIFICATION_DESC);

        for (MegaNode node : cardNodes) {
            boolean shouldGetPreview = false;
            File preview = new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
            LocalDate modifyDate = fromEpoch(node.getModificationTime());
            String day = ofPattern("dd").format(modifyDate);
            String month = ofPattern("MMMM").format(modifyDate);
            String year = ofPattern("yyyy").format(modifyDate);
            boolean sameYear = Year.from(LocalDate.now()).equals(Year.from(modifyDate));

            if (lastDayDate == null || lastDayDate.getDayOfYear() != modifyDate.getDayOfYear()) {
                shouldGetPreview = true;
                lastDayDate = modifyDate;
                String date = ofPattern(sameYear ? "dd MMMM" : "dd MMMM yyyy").format(lastDayDate);
                days.add(new CUCard(node, preview.exists() ? preview : null, day, month,
                        sameYear ? null : year, date, modifyDate, 0));
            } else if (!days.isEmpty()){
                days.get(days.size() - 1).incrementNumItems();
            }

            if (lastMonthDate == null
                    || !YearMonth.from(lastMonthDate).equals(YearMonth.from(modifyDate))) {
                shouldGetPreview = true;
                lastMonthDate = modifyDate;
                String date = sameYear ? month : ofPattern("MMMM yyyy").format(modifyDate);
                months.add(new CUCard(node, preview.exists() ? preview : null,null, month,
                        sameYear ? null : year, date, modifyDate, 0));
            }

            if (lastYearDate == null || !Year.from(lastYearDate).equals(Year.from(modifyDate))) {
                shouldGetPreview = true;
                lastYearDate = modifyDate;
                years.add(new CUCard(node, preview.exists() ? preview : null,null, null,
                        year, year, modifyDate, 0));
            }

            if (shouldGetPreview && !preview.exists()) {
                nodesWithoutPreview.add(node);
            }
        }

        add(Observable.fromIterable(nodesWithoutPreview)
                .zipWith(Observable.interval(GET_THUMBNAIL_THROTTLE_MS, MILLISECONDS),
                        (node, interval) -> node)
                .observeOn(Schedulers.computation())
                .subscribe(node -> {
                    File preview = new File(getPreviewFolder(mAppContext), node.getBase64Handle() + JPG_EXTENSION);
                    if(!preview.exists()) {
                        mMegaApi.getPreview(node, preview.getAbsolutePath(), createPreviewRequest);
                    }
                }, logErr("CuViewModel getPreview")));

        dayCards.postValue(days);
        monthCards.postValue(months);
        yearCards.postValue(years);
    }

    /**
     * Checks a clicked card, if it is in the provided list and if is in right position.
     *
     * @param position Clicked position in the list.
     * @param handle   Identifier of the card node.
     * @param cards    List of cards to check.
     * @return The checked card if found, null otherwise.
     */
    private CUCard getClickedCard(int position, long handle, List<CUCard> cards) {
        if (cards == null) {
            return null;
        }

        CUCard card = cards.get(position);

        if (handle != card.getNode().getHandle()) {
            card = null;

            for (CUCard c : cards) {
                if (c.getNode().getHandle() == handle) {
                    card = c;
                    break;
                }
            }
        }

        return card;
    }

    /**
     * Checks and gets the clicked day card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked day card.
     * @return The checked day card.
     */
    public CUCard dayClicked(int position, CUCard card) {
        return getClickedCard(position, card.getNode().getHandle(), getDayCards());
    }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    public int monthClicked(int position, CUCard card) {
        CUCard monthCard = getClickedCard(position, card.getNode().getHandle(), getMonthCards());
        if (monthCard == null) {
            return 0;
        }

        List<CUCard> dayCards = getDayCards();
        LocalDate cardLocalDate = monthCard.getLocalDate();
        int cardMonth = cardLocalDate.getMonthValue();
        int cardYear = cardLocalDate.getYear();
        int currentDay = LocalDate.now().getDayOfMonth();
        int dayPosition = 0;

        for (int i = 0; i < dayCards.size(); i++) {
            LocalDate nextLocalDate = dayCards.get(i).getLocalDate();
            int nextDay = nextLocalDate.getDayOfMonth();
            int nextMonth = nextLocalDate.getMonthValue();
            int nextYear = nextLocalDate.getYear();

            if (nextYear == cardYear && nextMonth == cardMonth) {
                if (nextDay <= currentDay) {
                    //Month of year clicked, current day. If not exists, the closest day behind the current.
                    if (i == 0 || nextDay == currentDay
                            || dayCards.get(i - 1).getLocalDate().getMonthValue() != cardMonth) {
                        return i;
                    }

                    int previousDay = dayCards.get(i - 1).getLocalDate().getDayOfMonth();

                    //The closest day to the current
                    return previousDay - currentDay <= currentDay - nextDay ? i - 1 : i;
                } else {
                    //Save the closest day above the current in case there is no day of month behind the current.
                    dayPosition = i;
                }
            }
        }

        return dayPosition;
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked year card.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    public int yearClicked(int position, CUCard card) {
        CUCard yearCard = getClickedCard(position, card.getNode().getHandle(), getYearCards());
        if (yearCard == null) {
            return 0;
        }

        List<CUCard> monthCards = getMonthCards();
        int cardYear = yearCard.getLocalDate().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        for (int i = 0; i < monthCards.size(); i++) {
            LocalDate nextLocalDate = monthCards.get(i).getLocalDate();
            int nextMonth = nextLocalDate.getMonthValue();

            if (nextLocalDate.getYear() == cardYear && nextMonth <= currentMonth) {
                //Year clicked, current month. If not exists, the closest month behind the current.
                if (i == 0 || nextMonth == currentMonth
                        || monthCards.get(i -1).getLocalDate().getYear() != cardYear) {
                    return i;
                }

                long previousMonth = monthCards.get(i - 1).getLocalDate().getMonthValue();

                //The closest month to the current
                return previousMonth - currentMonth <= currentMonth - nextMonth ? i - 1 : i;
            }
        }

        //No month equal or behind the current found, then return the latest month.
        return monthCards.size() - 1;
    }

    public void setmZoom(int mZoom) {
        this.mZoom = mZoom;
    }
}
