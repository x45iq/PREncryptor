package com.purplerat.prencryptor.cryptors;

import static com.purplerat.prencryptor.tools.EncryptorTools.createCipher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.purplerat.prencryptor.R;
import com.purplerat.prencryptor.tools.MathUtils;
import com.purplerat.prencryptor.tools.RandomString;
import com.purplerat.prencryptor.tools.UriUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class FileEncryptor implements Runnable{
    private static final short VERSION = 1;
    private static final int BUFFER_SIZE = 32_768;
    private final Context context;
    private final Uri uri;
    private final String password;
    private final Callback callback;
    public FileEncryptor(Context context,Uri uri, String password, Callback callback){
        this.context = context;
        this.uri = uri;
        this.password = password;
        this.callback = callback;
    }
    @Override
    public void run() {
        try {
            File file = encrypt(true);
            callback.onComplete(file);
        }catch (Exception e){
            callback.onComplete(null);
        }
    }
    void runAsFolder(){
        try {
            File file = encrypt(false);
            callback.onComplete(file);
        }catch (Exception e){
            callback.onComplete(null);
        }
    }
    private File encrypt(boolean isF) throws Exception{
        final String oldName = UriUtils.getFileName(context,uri);
        if(oldName==null){
            return null;
        }
        final File exportFolder = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),context.getString(R.string.app_name)),"Encrypted");
        if(!exportFolder.exists())exportFolder.mkdirs();

        final File exportFile = createExportFile(exportFolder,oldName.substring(0,oldName.lastIndexOf(".")),isF);
        try(BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(exportFile))){
            long seed = System.currentTimeMillis();
            bufferedOutputStream.write(ByteBuffer.allocate(8).putLong(seed).array(),0,8);
            try(CipherOutputStream cipherOutputStream = new CipherOutputStream(bufferedOutputStream,createCipher(password,seed,Cipher.ENCRYPT_MODE))){
                cipherOutputStream.write("EBPR".getBytes(StandardCharsets.UTF_8),0,4);
                cipherOutputStream.write(ByteBuffer.allocate(2).putShort(VERSION).array(), 0, 2);
                byte[] oldNameBytes = oldName.getBytes(StandardCharsets.UTF_8);
                cipherOutputStream.write(ByteBuffer.allocate(2).putShort(Short.parseShort(oldNameBytes.length+"")).array(),0,2);
                cipherOutputStream.write(oldNameBytes,0,oldNameBytes.length);
                long contentLength = UriUtils.getContentLength(context,uri);
                cipherOutputStream.write(ByteBuffer.allocate(8).putLong(contentLength).array(),0,8);//Content len
                byte[] buffer = new byte[BUFFER_SIZE];
                long encrypted = 0;
                int len;
                try(BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri))) {
                    while ((len = bufferedInputStream.read(buffer)) != -1) {
                        cipherOutputStream.write(buffer, 0, len);
                        encrypted += len;
                        callback.onProgress(MathUtils.getPercent(encrypted,contentLength));
                    }
                }
                MediaScannerConnection.scanFile(context, new String[]{exportFile.getAbsolutePath()}, new String[]{UriUtils.getMimeType(context,UriUtils.getUriFromFile(context,exportFile))}, null);
                return exportFile;
            }
        }
    }
    private File createExportFile(File exportFolder,String name,boolean isF){
        File file = new File(exportFolder,String.format(isF?"%s.ebpr":"%s.febpr",name));
        if(!file.exists())return file;
        int n = 0;
        while(file.exists())file = new File(exportFolder,String.format(isF?"%s(%s).ebpr":"%s(%s).febpr",name,n++));
        return file;
    }
    public interface Callback{
        void onProgress(int progress);
        void onComplete(File file);
    }
}
