package com.purplerat.prencryptor.cryptors;


import static com.purplerat.prencryptor.tools.EncryptorTools.createCipher;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import com.purplerat.prencryptor.R;
import com.purplerat.prencryptor.tools.CPurpleInputStream;
import com.purplerat.prencryptor.tools.MathUtils;
import com.purplerat.prencryptor.tools.PurpleInputStream;
import com.purplerat.prencryptor.tools.RandomString;
import com.purplerat.prencryptor.tools.UriUtils;

import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
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

public class FolderDecoder implements Runnable{
    private final Context context;
    private final Uri uri;
    private final String password;
    private final Callback callback;
    public FolderDecoder(Context context, Uri uri, String password, Callback callback){
        this.context = context;
        this.uri = uri;
        this.password = password;
        this.callback = callback;
    }
    @Override
    public void run() {
        decrypt();
    }
    private void decrypt(){
        File inner = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if(!inner.exists())inner.mkdirs();
        File fold = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),context.getString(R.string.app_name)),"Decrypted");
        if(!fold.exists())fold.mkdirs();
        new FileDecoder(context, uri, password, new FileDecoder.Callback() {
            @Override
            public void onProgress(int progress) {
                callback.onProgress(Integer.min(progress,90));
            }

            @Override
            public void onComplete(File file) {
                if(file==null){
                    callback.onComplete(null);
                    return;
                }
                try{
                    File exportFolder = mkFold(fold,file.getName().substring(0,file.getName().lastIndexOf(".")));
                    ZipUtil.unpack(file,exportFolder);
                    callback.onProgress(100);
                    callback.onComplete(exportFolder);
                }finally {
                    file.delete();
                }
            }
        }).run();
    }
    private File mkFold(File parent,String name){
        File file = new File(parent,name);
        if(!file.exists())return file;
        int n = 0;
        while(file.exists())file = new File(parent,String.format("%s(%s)",name,n++));
        return file;
    }

    public interface Callback{
        void onProgress(int progress);
        void onComplete(File file);
    }
}

