package mega.privacy.android.app.repo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Util.fromEpoch;

public class MegaNodeRepo {

    public static final int CU_TYPE_CAMERA = 0;
    public static final int CU_TYPE_MEDIA = 1;

    private final MegaApiAndroid megaApi;
    private final DatabaseHandler dbHandler;
    private final Context context;

    public MegaNodeRepo(MegaApiAndroid megaApi, DatabaseHandler dbHandler, Context context) {
        this.megaApi = megaApi;
        this.dbHandler = dbHandler;
        this.context = context;
    }

    /**
     * Get children of CU/MU, with the given order, and filter nodes by date (optional).
     *
     * @param type CU_TYPE_CAMERA or CU_TYPE_MEDIA
     * @param orderBy order
     * @param filter search filter
     * filter[0] is the search type:
     * 0 means search for nodes in one day, then filter[1] is the day in millis.
     * 1 means search for nodes in last month (filter[2] is 1), or in last year (filter[2] is 2).
     * 2 means search for nodes between two days, filter[3] and filter[4] are start and end day in
     * millis.
     * @return list of pairs, whose first value is index in parent, second value is the node, and
     * the index of parent is used for FullscreenImageViewer/AudioVideoPlayer
     */
    public List<Pair<Integer, MegaNode>> getCuChildren(int type, int orderBy, long[] filter) {
        long cuHandle = -1;
        MegaPreferences pref = dbHandler.getPreferences();
        if (type == CU_TYPE_CAMERA) {
            if (pref != null && pref.getCamSyncHandle() != null) {
                try {
                    cuHandle = Long.parseLong(pref.getCamSyncHandle());
                } catch (NumberFormatException e) {
                    logError("parse getCamSyncHandle error " + e);
                }
                if (megaApi.getNodeByHandle(cuHandle) == null) {
                    cuHandle = -1;
                }
            }

            if (cuHandle == -1) {
                for (MegaNode node : megaApi.getChildren(megaApi.getRootNode())) {
                    if (node.isFolder() && TextUtils.equals(
                            context.getString(R.string.section_photo_sync),
                            node.getName())) {
                        cuHandle = node.getHandle();
                        dbHandler.setCamSyncHandle(cuHandle);
                        break;
                    }
                }
            }
        } else {
            if (pref != null && pref.getMegaHandleSecondaryFolder() != null) {
                try {
                    cuHandle = Long.parseLong(pref.getMegaHandleSecondaryFolder());
                } catch (NumberFormatException e) {
                    logError("parse MegaHandleSecondaryFolder error " + e);
                }
                if (megaApi.getNodeByHandle(cuHandle) == null) {
                    cuHandle = -1;
                }
            }
        }

        if (cuHandle == -1) {
            return Collections.emptyList();
        }

        List<MegaNode> children = megaApi.getChildren(megaApi.getNodeByHandle(cuHandle), orderBy);
        List<Pair<Integer, MegaNode>> nodes = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            MegaNode node = children.get(i);
            if (node.isFolder()) {
                continue;
            }
            MimeTypeThumbnail mime = MimeTypeThumbnail.typeForName(node.getName());
            if (mime.isImage() || mime.isVideoReproducible()) {
                nodes.add(Pair.create(i, node));
            }
        }

        if (filter == null) {
            return nodes;
        }

        List<Pair<Integer, MegaNode>> result = new ArrayList<>();

        Function<MegaNode, Boolean> filterFunction = null;
        if (filter[0] == 1) {
            LocalDate date = fromEpoch(filter[1] / 1000);
            filterFunction = node -> date.equals(fromEpoch(node.getModificationTime()));
        } else if (filter[0] == 2) {
            if (filter[2] == 1) {
                YearMonth lastMonth = YearMonth.now().minusMonths(1);
                filterFunction =
                        node -> lastMonth.equals(
                                YearMonth.from(fromEpoch(node.getModificationTime())));
            } else if (filter[2] == 2) {
                int lastYear = YearMonth.now().getYear() - 1;
                filterFunction =
                        node -> fromEpoch(node.getModificationTime()).getYear() == lastYear;
            }
        } else if (filter[0] == 3) {
            LocalDate from = fromEpoch(filter[3] / 1000);
            LocalDate to = fromEpoch(filter[4] / 1000);
            filterFunction = node -> {
                LocalDate modifyDate = fromEpoch(node.getModificationTime());
                return !modifyDate.isBefore(from) && !modifyDate.isAfter(to);
            };
        }

        if (filterFunction == null) {
            return result;
        }

        for (Pair<Integer, MegaNode> pair : nodes) {
            if (filterFunction.apply(pair.second)) {
                result.add(pair);
            }
        }

        return result;
    }
}
