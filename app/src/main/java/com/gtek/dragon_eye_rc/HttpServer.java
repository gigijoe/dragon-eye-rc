package com.gtek.dragon_eye_rc;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import com.gtek.dragon_eye_rc.NanoFileUpload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by wangmingxing on 2017/6/20.
 */

/*
* https://github.com/mason-Wang/android-httpserver
*/

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private NanoFileUpload mFileUpload;
    private OnStatusUpdateListener mStatusUpdateListener;
    private Context mContext;

    interface OnStatusUpdateListener {
        void onUploadingProgressUpdate(int progress);
        void onUploadingFile(File file, boolean done);
        void onDownloadingFile(boolean done);
    }

    class DownloadResponse extends Response {
        DownloadResponse(InputStream stream) throws IOException {
            super(Response.Status.OK,
                    "application/octet-stream",
                    stream,
                    stream.available());
        }

        @Override
        protected void send(OutputStream outputStream) {
            super.send(outputStream);
            if (mStatusUpdateListener != null) {
                mStatusUpdateListener.onDownloadingFile(true);
            }
        }
    }

    public HttpServer(Context context, int port) {
        super(port);
        mContext = context;
        mFileUpload = new NanoFileUpload(new DiskFileItemFactory());
        mFileUpload.setProgressListener(new ProgressListener() {
            int progress = 0;
            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {
                //Log.d(TAG, pBytesRead + " bytes has been read, totol " + pContentLength + " bytes");
                if (mStatusUpdateListener != null) {
                    int p = (int) (pBytesRead * 100 / pContentLength);
                    if (p != progress) {
                        progress = p;
                        mStatusUpdateListener.onUploadingProgressUpdate(progress);
                    }
                }
            }
        });
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (NanoFileUpload.isMultipartContent(session)) {
            return newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED, NanoHTTPD.MIME_HTML, "Upload is NOT support !!!");
        } else {
            String[] s = uri.split("/");

            if(s.length < 1) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "File NOT found !!!");
            }

            String filename = s[s.length - 1];
            // serve file download
            Response response = null;

            if(TextUtils.equals(filename, "firmware.img")) {
                if (mStatusUpdateListener != null) {
                    mStatusUpdateListener.onDownloadingFile(false);
                }

                try {
                    InputStream is = mContext.getResources().openRawResource(R.raw.firmware);
                    response = new DownloadResponse(is);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response != null) {
                    response.addHeader(
                            "Content-Disposition", "attachment; filename=" + filename);
                    return response;
                } else {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "Internal error !!!");
                }
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "File NOT found !!!");
            }
        }
    }

    public void setOnStatusUpdateListener(OnStatusUpdateListener listener) {
        mStatusUpdateListener = listener;
    }
}