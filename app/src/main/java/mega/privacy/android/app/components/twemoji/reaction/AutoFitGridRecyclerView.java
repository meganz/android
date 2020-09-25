package mega.privacy.android.app.components.twemoji.reaction;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;

public class AutoFitGridRecyclerView extends RecyclerView {
    private GridLayoutManager manager;
    private int columnWidth = INVALID_POSITION;

    public AutoFitGridRecyclerView(Context context) {
        super(context);
        initialization(context, null);
    }

    public AutoFitGridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialization(context, attrs);
    }

    public AutoFitGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialization(context, attrs);
    }

    public void columnWidth(int width){
        columnWidth = width;
    }

    private void initialization(Context context, AttributeSet attrs) {
        try{
            if (attrs != null) {
                int[] attrsArray = {
                        android.R.attr.columnWidth
                };
                TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
                columnWidth = array.getDimensionPixelSize(0, INVALID_POSITION);
                array.recycle();
            }
            manager = new GridLayoutManager(context, 4, RecyclerView.VERTICAL, false);
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