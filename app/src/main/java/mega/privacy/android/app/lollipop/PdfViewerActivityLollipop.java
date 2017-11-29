package mega.privacy.android.app.lollipop;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;

import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class PdfViewerActivityLollipop extends PinActivityLollipop implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener{

    PDFView pdfView;
    Uri uri;
    String pdfFileName;
    int pageNumber = 0;

    public static final String SAMPLE_FILE = "sample.pdf";

    @Override
    public void onCreate (Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null){
            log("intent null");
            finish();
            return;
        }

        uri = intent.getData();
        if (uri == null){
            log("uri null");
            finish();
            return;
        }

        setContentView(R.layout.activity_pdfviewer);

        pdfView = (PDFView) findViewById(R.id.pdfView);

        pdfView.setBackgroundColor(Color.LTGRAY);
        pdfFileName = getFileName(uri);

        pdfView.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .load();

        setTitle(pdfFileName);
    }


    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void onPageError(int page, Throwable t) {
        log("Cannot load page " + page);
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        log("title = " + meta.getTitle());
        log("author = " + meta.getAuthor());
        log("subject = " + meta.getSubject());
        log("keywords = " + meta.getKeywords());
        log("creator = " + meta.getCreator());
        log("producer = " + meta.getProducer());
        log("creationDate = " + meta.getCreationDate());
        log("modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            log(String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    public static void log(String log) {
        Util.log("PdfViewerActivityLollipop", log);
    }
}
