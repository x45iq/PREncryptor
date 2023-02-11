package com.purplerat.prencryptor.cryptors;

import static com.purplerat.prencryptor.tools.EncryptorTools.createCipher;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
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

public class FileDecoder implements Runnable{
    private static final int BUFFER_SIZE = 32_768;
    private final Context context;
    private final Uri uri;
    private final String password;
    private final Callback callback;
    public FileDecoder(Context context, Uri uri, String password, Callback callback){
        this.context = context;
        this.uri = uri;
        this.password = password;
        this.callback = callback;
    }
    @Override
    public void run() {
        try {
            File file = decrypt();
            callback.onComplete(file);
        }catch (Exception e){
            callback.onComplete(null);
        }
    }
    private File decrypt() throws Exception{
        File fd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),context.getString(R.string.app_name));
        if(!fd.exists())fd.mkdirs();
        File exportDirectory = new File(fd,"Decrypted");
        if(!exportDirectory.exists())exportDirectory.mkdirs();

        try(PurpleInputStream purpleInputStream = new PurpleInputStream(context.getContentResolver().openInputStream(uri))){
            try(CPurpleInputStream inputStream = new CPurpleInputStream(purpleInputStream,createCipher(password, ByteBuffer.wrap(purpleInputStream.readNBytes(8)).getLong(),Cipher.DECRYPT_MODE))){
                if(!new String(inputStream.readNBytes(4), StandardCharsets.UTF_8).equals("EBPR"))return null;
                short version = ByteBuffer.wrap(inputStream.readNBytes(2)).getShort();
                if(version == 1){
                    int fileNameSize = new BigInteger(inputStream.readNBytes(2)).shortValue();
                    String fileName = new String(inputStream.readNBytes(fileNameSize));
                    File exportFile = new File(exportDirectory, fileName);
                    String formFileName = fileName.substring(0,fileName.lastIndexOf(".")) + "(%s)" + fileName.substring(fileName.lastIndexOf("."));
                    int n = 1;
                    while(exportFile.exists())exportFile = new File(exportDirectory,String.format(formFileName,n++));

                    long contentLength = new BigInteger(inputStream.readNBytes(8)).longValue();
                    long decrypted = 0;
                    try(BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(exportFile))){
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while((len = inputStream.read(buffer))!= -1) {
                            bufferedOutputStream.write(buffer, 0, len);
                            decrypted += len;
                            callback.onProgress(MathUtils.getPercent(decrypted,contentLength));
                        }
                    }
                    MediaScannerConnection.scanFile(context, new String[]{exportFile.getAbsolutePath()}, new String[]{UriUtils.getMimeType(context,UriUtils.getUriFromFile(context,exportFile))}, null);
                    return exportFile;
                }
                return null;
            }
        }
    }

    public interface Callback{
        void onProgress(int progress);
        void onComplete(File file);
    }
}

