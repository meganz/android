package mega.privacy.android.app.components.twemoji.reaction;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import mega.privacy.android.app.components.RtlGridLayoutManager;

import static mega.privacy.android.app.utils.Constants.INVALID_DIMENSION;

public class AutoFitRecyclerView extends RecyclerView {
    private GridLayoutManager manager;
    private int columnWidth = INVALID_DIMENSION;
    private static final int SPAN_COUNT = 4;
    private Context context;
    private AttributeSet attrs;

    public AutoFitRecyclerView(Context context) {
        super(context);
        this.context = context;
        this.attrs = null;
    }

    public AutoFitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
    }

    public AutoFitRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        this.attrs = attrs;
    }

    public void columnWidth(int width){
        columnWidth = width;
    }

    public void initialization(boolean isReverse){
        initialization(context, attrs, isReverse);
    }

    private void initialization(Context context, AttributeSet attrs, boolean isReverse) {
        try{
            if (attrs != null) {
                int[] attrsArray = {
                        android.R.attr.columnWidth,
                };

                TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
                columnWidth = array.getDimensionPixelSize(0, INVALID_DIMENSION);
                array.recycle();
            }

            if (isReverse) {
                manager = new RtlGridLayoutManager(context, SPAN_COUNT, RecyclerView.VERTICAL, false);
            } else {
                manager = new GridLayoutManager(context, SPAN_COUNT, RecyclerView.VERTICAL, false);
            }

            setLayoutManager(manager);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        try{
            if (columnWidth > 0) {
                int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                manager.setSpanCount(spanCount);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}