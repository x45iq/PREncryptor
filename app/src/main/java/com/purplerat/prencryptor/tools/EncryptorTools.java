package com.purplerat.prencryptor.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptorTools {
    public static boolean isPasswordRight(Context context, Uri uri, String password){
        try(PurpleInputStream inputStream = new PurpleInputStream(context.getContentResolver().openInputStream(uri))){
            try(CPurpleInputStream cPurpleInputStream = new CPurpleInputStream(inputStream,createCipher(password, ByteBuffer.wrap(inputStream.readNBytes(8)).getLong(),Cipher.DECRYPT_MODE))){
                return new String(cPurpleInputStream.readNBytes(4), StandardCharsets.UTF_8).equals("EBPR");
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public static boolean isPasswordRight(File file, String password){
        try(PurpleInputStream inputStream = new PurpleInputStream(new FileInputStream(file))){
            try(CPurpleInputStream cPurpleInputStream = new CPurpleInputStream(inputStream,createCipher(password, ByteBuffer.wrap(inputStream.readNBytes(8)).getLong(),Cipher.DECRYPT_MODE))){
                return new String(cPurpleInputStream.readNBytes(4), StandardCharsets.UTF_8).equals("EBPR");
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public static Cipher createCipher(String password,long seed,int cipherMode) throws Exception {
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
        cipher.init(cipherMode, getKeyFromPassword(password,new RandomString(100, new Random(seed)).nextString()));
        return cipher;
    }
    private static SecretKey getKeyFromPassword(String password, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
