package com.purplerat.prencryptor;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class EncryptedFile implements Serializable {
    public enum TYPE {FILE,FOLDER}
    private final DocumentFile documentFile;
    private final TYPE type;
    private final long encryptedTime;
    public EncryptedFile(DocumentFile documentFile, TYPE type, long encryptedTime) {
        this.documentFile = documentFile;
        this.type = type;
        this.encryptedTime = encryptedTime;
    }

    public String getName() {
        if(documentFile.getName()==null)return null;
        return documentFile.getName().substring(0,documentFile.getName().lastIndexOf("."));
    }

    public DocumentFile getFile() {
        return documentFile;
    }

    public TYPE getType() {
        return type;
    }

    public long getEncryptedTime() {
        return encryptedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedFile)) return false;
        EncryptedFile that = (EncryptedFile) o;
        return Objects.equals(getName(), that.getName()) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), type);
    }
}
