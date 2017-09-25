package mega.privacy.android.app.components.flowlayoutmanager;


import android.graphics.Point;
import android.support.v7.widget.RecyclerView;


public class LayoutHelper {
    RecyclerView.LayoutManager layoutManager;
    RecyclerView recyclerView;

    public LayoutHelper(RecyclerView.LayoutManager layoutManager, RecyclerView recyclerView) {
        this.layoutManager = layoutManager;
        this.recyclerView = recyclerView;
    }

    public int leftVisibleEdge() {
        return recyclerView.getPaddingLeft();
    }

    public int rightVisibleEdge() {
        return layoutManager.getWidth() - layoutManager.getPaddingRight();
    }

    public int visibleAreaWidth() {
        return rightVisibleEdge() - leftVisibleEdge();
    }

    public int topVisibleEdge() {
        return layoutManager.getPaddingTop();
    }

    public int bottomVisibleEdge() {
        return layoutManager.getHeight() - layoutManager.getPaddingBottom();
    }

    public static boolean hasItemsPerLineLimit(FlowLayoutOptions layoutOptions) {
        return layoutOptions.itemsPerLine > 0;
    }

    public static boolean shouldStartNewline(int x, int childWidth, int leftEdge, int rightEdge, LayoutContext layoutContext) {
        if (hasItemsPerLineLimit(layoutContext.layoutOptions) && layoutContext.currentLineItemCount == layoutContext.layoutOptions.itemsPerLine) {
            return true;
        }
        switch (layoutContext.layoutOptions.alignment) {
            case RIGHT:
                return x - childWidth < leftEdge;
            case LEFT:
            default:
                return x + childWidth > rightEdge;
        }
    }

    public Point layoutStartPoint(LayoutContext layoutContext) {
        switch (layoutContext.layoutOptions.alignment) {
            case RIGHT:
                return new Point(rightVisibleEdge(), topVisibleEdge());
            default:
                return new Point(leftVisibleEdge(), topVisibleEdge());
        }
    }
}