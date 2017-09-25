package mega.privacy.android.app.components.flowlayoutmanager;

public class LayoutContext {
    public FlowLayoutOptions layoutOptions;
    public int currentLineItemCount;

    public static LayoutContext clone(LayoutContext layoutContext) {
        LayoutContext resultContext = new LayoutContext();
        resultContext.currentLineItemCount = layoutContext.currentLineItemCount;
        resultContext.layoutOptions = FlowLayoutOptions.clone(layoutContext.layoutOptions);
        return resultContext;
    }

    public static LayoutContext fromLayoutOptions(FlowLayoutOptions layoutOptions) {
        LayoutContext layoutContext = new LayoutContext();
        layoutContext.layoutOptions = layoutOptions;
        return layoutContext;
    }
}