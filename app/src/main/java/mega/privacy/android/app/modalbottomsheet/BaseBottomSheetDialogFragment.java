package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.ActivityLauncher;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment implements ActivityLauncher {
    protected static final int HEIGHT_HEADER_RADIO_GROUP = 56;
    protected static final int HEIGHT_HEADER_LARGE = 81;
    protected static final int HEIGHT_HEADER_LOW = 48;
    protected static final int HEIGHT_SEPARATOR = 1;

    protected static final String TYPE_OPTION = "TYPE_OPTION";
    protected static final String TYPE_SEPARATOR = "TYPE_SEPARATOR";

    protected Context context;
    protected MegaApplication app;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;
    private int halfHeightDisplay;
    private int heightHeader;
    protected BottomSheetBehavior mBehavior;

    protected View contentView;
    protected LinearLayout mainLinearLayout;
    protected LinearLayout items_layout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = MegaApplication.getInstance();
        if (app == null || getActivity() == null) {
            logError("MegaApplication or FragmentActivity is null");
            return;
        }

        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        dbH = app.getDbH();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        halfHeightDisplay = outMetrics.heightPixels / 2;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onResume() {
        super.onResume();

        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        // In landscape mode, we need limit the bottom sheet dialog width.
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int maxSize = displayMetrics.heightPixels;
            window.setLayout(maxSize, MATCH_PARENT);
        }

        // But `setLayout` causes navigation buttons almost invisible in light mode,
        // in this case we set navigation bar background with light grey to make
        // navigation buttons visible.
        if (!Util.isDarkMode(requireContext())) {
            // Only set navigation bar elements colour, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010
            window.getDecorView().setSystemUiVisibility(0x00000010);
        }
    }

    /**
     * Sets the initial state of a BottomSheet and its state.
     *
     * @param heightHeader           Height of the header.
     * @param addBottomSheetCallBack True if it should add a BottomSheetCallback, false otherwise.
     */
    protected void setBottomSheetBehavior(int heightHeader, boolean addBottomSheetCallBack) {
        this.heightHeader = heightHeader;
        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());

        if (items_layout != null) {
            mBehavior.setPeekHeight(getPeekHeight());
        }
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        if (addBottomSheetCallBack) {
            addBottomSheetCallBack();
        }
    }

    /**
     * Hides the BottomSheet.
     */
    protected void setStateBottomSheetBehaviorHidden() {
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * Sets the behaviour of a BottomSheet when its state changes.
     */
    private void addBottomSheetCallBack() {
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (isScreenInPortrait(context)) {
                    ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                    if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                        int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                        Rect rectangle = new Rect();
                        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.bottom;
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                        int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                        if (mainLinearLayout != null && mainLinearLayout.getHeight() > maxHeight) {
                            params.height = maxHeight;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    /**
     * Gets the initial height of a BottomSheet.
     * It depends on the number of visible options on it, the display height
     * and current orientation of the used device.
     * The maximum height will be a bit more than half of the screen.
     *
     * @return The initial height of a BottomSheet.
     */
    private int getPeekHeight() {
        int numVisibleOptions = 0;
        int childHeight = 0;
        Map<Integer, String> visibleItems = new HashMap<>();
        int peekHeight = dp2px(heightHeader, outMetrics);
        int heightSeparator = dp2px(HEIGHT_SEPARATOR, outMetrics);

        for (int i = 0; i < items_layout.getChildCount(); i++) {
            View v = items_layout.getChildAt(i);

            if (v != null && v.getVisibility() == VISIBLE) {
                int height = v.getLayoutParams().height;
                childHeight += height;

                if (height == heightSeparator) {
                    //Is separator
                    visibleItems.put(i, TYPE_SEPARATOR);
                } else {
                    //Is visible option
                    numVisibleOptions++;
                    visibleItems.put(i, TYPE_OPTION);
                }
            }
        }

        if ((numVisibleOptions <= 3 && heightHeader == HEIGHT_HEADER_LARGE)
                || (numVisibleOptions <= 4 && heightHeader == HEIGHT_HEADER_LOW)) {
            return peekHeight + childHeight;
        }

        int countVisibleOptions = 0;

        for (Map.Entry<Integer, String> visibleItem : visibleItems.entrySet()) {
            String visibleItemType = visibleItem.getValue();
            int visibleItemPosition = visibleItem.getKey();
            int heightVisiblePosition = items_layout.getChildAt(visibleItemPosition).getLayoutParams().height;

            if (visibleItemType.equals(TYPE_OPTION)) {
                countVisibleOptions++;
            }

            if (peekHeight < halfHeightDisplay
                    || visibleItemType.equals(TYPE_SEPARATOR)
                    || countVisibleOptions == numVisibleOptions) {
                peekHeight += heightVisiblePosition;
            } else {
                return peekHeight + (heightVisiblePosition / 2);
            }
        }

        return peekHeight;
    }

    @Override
    public void launchActivity(@NotNull Intent intent) {
        startActivity(intent);
    }

    @Override
    public void launchActivityForResult(@NotNull Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }
}
