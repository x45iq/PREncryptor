package com.purplerat.prencryptor.cryptors;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import com.purplerat.prencryptor.tools.PLog;
import com.purplerat.prencryptor.tools.UriUtils;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FolderEncryptor implements Runnable{
    private static final PLog LOGGER = new PLog(FolderEncryptor.class);
    private final Context context;
    private final Uri uri;
    private final String password;
    private final Callback callback;
    public FolderEncryptor(Context context,Uri uri, String password, Callback callback){
        this.context = context;
        this.uri = uri;
        this.password = password;
        this.callback = callback;
    }
    @Override
    public void run() {
        try {
            encrypt();
        }catch (Exception e){
            LOGGER.error(e);
            callback.onComplete(null);
        }
    }
    private void encrypt() throws Exception{
        File innerFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if(!innerFolder.exists())innerFolder.mkdirs();
        File zipFile = null;
        try{
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                    DocumentsContract.getTreeDocumentId(uri));
            File fold = UriUtils.getFileFromUri(context, docUri);
            if(fold==null)throw new IOException();
            parseFile(DocumentFile.fromFile(fold));
            zipFile = new File(innerFolder,String.format("%s.zip",fold.getName()));
            ZipUtil.pack(fold, zipFile);
            new FileEncryptor(context, UriUtils.getUriFromFile(context, zipFile), password, new FileEncryptor.Callback() {
                @Override
                public void onProgress(int progress) {
                    callback.onProgress(progress);
                }

                @Override
                public void onComplete(File file) {
                    MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{UriUtils.getMimeType(context,UriUtils.getUriFromFile(context,file))}, null);
                    callback.onComplete(file);
                }
            }).runAsFolder();
        }finally {
            if(zipFile!=null&&zipFile.exists())zipFile.delete();
        }
    }
    private void parseFile(DocumentFile documentFile){
        if(documentFile==null)return;
        LOGGER.debug(documentFile.getType());
        if(documentFile.isDirectory()){
            for(DocumentFile child : documentFile.listFiles()){
                parseFile(child);
            }
        }

    }

    public interface Callback{
        void onProgress(int progress);
        void onComplete(File file);
    }
}
