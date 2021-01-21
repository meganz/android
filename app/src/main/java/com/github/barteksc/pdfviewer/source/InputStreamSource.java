/*
 * Copyright (C) 2016 Bartosz Schiller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.source;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.github.barteksc.pdfviewer.util.Util;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import mega.privacy.android.app.utils.CacheFolderManager;

public class InputStreamSource implements DocumentSource {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private InputStream inputStream;
    private final String tmpFileName;

    public InputStreamSource(InputStream inputStream, String tmpFileName) {
        this.inputStream = inputStream;
        this.tmpFileName = tmpFileName;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password)
            throws IOException {
        File tmpFolder =
                CacheFolderManager.getCacheFolder(context, CacheFolderManager.TEMPORAL_FOLDER);
        if (tmpFolder == null || tmpFileName == null) {
            // fail to create tmp folder, or no tmpFileName provided, fallback to old behavior
            return core.newDocument(Util.toByteArray(inputStream), password);
        }

        File tmpFile = new File(tmpFolder, tmpFileName + ".pdf");
        FileOutputStream outputStream = new FileOutputStream(tmpFile);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while (EOF != (bytes = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, bytes);
        }
        outputStream.close();
        inputStream.close();

        ParcelFileDescriptor fd =
                context.getContentResolver().openFileDescriptor(Uri.fromFile(tmpFile), "r");
        return core.newDocument(fd, password);
    }
}
