package com.purplerat.prencryptor;

import static com.purplerat.prencryptor.DialogManager.createEncryptingTypeDialog;
import static com.purplerat.prencryptor.DialogManager.createPasswordDialog;
import static com.purplerat.prencryptor.DialogManager.createProgressDialog;
import static com.purplerat.prencryptor.tools.EncryptorTools.isPasswordRight;
import static com.purplerat.prencryptor.tools.UriUtils.getMimeType;
import static com.purplerat.prencryptor.tools.UriUtils.getUriFromFile;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.purplerat.prencryptor.cryptors.FileDecoder;
import com.purplerat.prencryptor.cryptors.FileEncryptor;
import com.purplerat.prencryptor.cryptors.FolderDecoder;
import com.purplerat.prencryptor.cryptors.FolderEncryptor;
import com.purplerat.prencryptor.databinding.ActivityMainBinding;
import com.purplerat.prencryptor.tools.PLog;
import com.purplerat.prencryptor.tools.UriUtils;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final PLog LOGGER = new PLog(MainActivity.class);
    private ActivityMainBinding binding;
    private MainAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        RecyclerView mainRecycle = binding.mainRecycle;
        FloatingActionButton floatingButton = binding.floatingButton;
        adapter = new MainAdapter(file -> {
            switch (file.getType()){
                case FILE:
                    decryptFile(file.getFile().getUri());
                    break;
                case FOLDER:
                    makeSnackBar(getString(R.string.will_be_soon),Snackbar.LENGTH_SHORT);
                    //decryptFolder(file.getFile().getUri());
                    break;
            }
        });
        mainRecycle.setAdapter(adapter);
        mainRecycle.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));

        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.file_deleting)
                        .setMessage(R.string.are_you_sure_delete)
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            adapter.getItemAt(viewHolder.getAdapterPosition()).getFile().delete();
                            adapter.removeItemAt(viewHolder.getAdapterPosition());
                        })
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> adapter.notifyItemChanged(viewHolder.getAdapterPosition()))
                        .setCancelable(false)
                        .show();
            }
        });
        ith.attachToRecyclerView(mainRecycle);

        floatingButton.setOnClickListener(v->
                createEncryptingTypeDialog(
                this,
                new String[]{getString(R.string.file), getString(R.string.folder)},
                new Runnable[]{()->fileLauncher.launch("*/*"),()->{
                    makeSnackBar(getString(R.string.will_be_soon),Snackbar.LENGTH_SHORT);
                    //folderLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                }}
                )
        );
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            askForReadPermission();
        }
        refreshList();
        if(getIntent()!=null && getIntent().getAction().equals(Intent.ACTION_VIEW)){
            Uri uri = getIntent().getData();
            if(uri!=null) {
                String name = UriUtils.getFileName(this, uri);
                if (name.endsWith(".ebpr")) {
                    decryptFile(uri);
                } else if (name.endsWith(".febpr")) {
                    makeSnackBar(getString(R.string.will_be_soon),Snackbar.LENGTH_SHORT);
                    //decryptFolder(uri);
                } else {
                    makeSnackBar(getString(R.string.unsupported_file), Snackbar.LENGTH_SHORT);
                }
                getIntent().setData(null);
                getIntent().setAction(null);
            }
        }


    }
    private void refreshList(){
        DocumentFile folder = DocumentFile.fromFile(new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), getString(R.string.app_name)),"Encrypted"));
        if(folder.isDirectory() && folder.exists()){
            for(DocumentFile file : Objects.requireNonNull(folder.listFiles())){
                if(file.getName()!=null){
                    if(file.getName().endsWith(".ebpr")){
                        addItemToList(new EncryptedFile(file, EncryptedFile.TYPE.FILE,file.lastModified()));
                    }else if(file.getName().endsWith(".febpr")){
                        addItemToList(new EncryptedFile(file, EncryptedFile.TYPE.FOLDER,file.lastModified()));
                    }
                }
            }
        }


    }
    private void askForReadPermission(){
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.allow_permission))
                .setMessage(getString(R.string.app_func_is_not_available))
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE))
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> askForReadPermission())
                .setCancelable(false)
                .show();
    }
    private void addItemToList(EncryptedFile encryptedFile){
        if(adapter!=null){
            adapter.putItem(encryptedFile);
        }
    }
    private void decryptFile(Uri uri){
        createPasswordDialog(this,getLayoutInflater(),password -> {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(R.layout.checking_dialog_view)
                    .setCancelable(false)
                    .show();
            new Thread(()->{
                if(isPasswordRight(this,uri,password)){
                    runOnUiThread(()->{
                        dialog.dismiss();
                        decryptFile(uri,password);
                    });
                }else{
                    dialog.dismiss();
                    runOnUiThread(()->makeSnackBar(getString(R.string.password_is_wrong),Snackbar.LENGTH_SHORT));
                }
            }).start();
        });
    }
    private void decryptFile(Uri uri,String password){
        DialogManager.ProgressDialogCallback callback = createProgressDialog(this,getLayoutInflater(),R.string.decrypting_in_progress);
        if(callback==null)return;
        new Thread(new FileDecoder(this, uri, password, new FileDecoder.Callback() {
            @Override
            public void onProgress(int progress) {
                callback.onProgress(progress);
            }

            @Override
            public void onComplete(File file) {
                callback.dismiss();
                runOnUiThread(()->{
                    if(file !=null){
                        makeSnackBar(getString(R.string.successfully_decrypted),Snackbar.LENGTH_LONG,R.string.open,(v)->{
                            Intent intent = new Intent(Intent.ACTION_SEND)
                                    .putExtra(Intent.EXTRA_STREAM,getUriFromFile(MainActivity.this,file))
                                    .setType(getMimeType(MainActivity.this,getUriFromFile(MainActivity.this,file)));
                            startActivity(Intent.createChooser(intent, null));

                        });
                    }else{
                        makeSnackBar(getString(R.string.failed_to_decrypt_file),Snackbar.LENGTH_LONG);
                    }
                });
            }
        })).start();
    }
    private void encryptFile(Uri uri){
        createPasswordDialog(this,getLayoutInflater(),(str)-> encryptFile(uri,str));
    }
    private void encryptFile(Uri uri,String password){
        DialogManager.ProgressDialogCallback callback = createProgressDialog(this,getLayoutInflater(),R.string.encrypting_in_progress);
        if(callback==null)return;
        new Thread(new FileEncryptor(this, uri, password, new FileEncryptor.Callback() {
            @Override
            public void onProgress(int progress) {
                callback.onProgress(progress);
            }

            @Override
            public void onComplete(File file) {
                callback.dismiss();
                runOnUiThread(()->{
                    if(file !=null){
                        addItemToList(new EncryptedFile(DocumentFile.fromFile(file), EncryptedFile.TYPE.FILE,file.lastModified()));
                        makeSnackBar(getString(R.string.successfully_encrypted), Snackbar.LENGTH_LONG, R.string.share, view -> {
                            Intent intent = new Intent(Intent.ACTION_SEND)
                                    .putExtra(Intent.EXTRA_STREAM,getUriFromFile(MainActivity.this,file))
                                    .setType(getMimeType(MainActivity.this,getUriFromFile(MainActivity.this,file)));
                            startActivity(Intent.createChooser(intent, null));
                        });
                    }else{
                        makeSnackBar(getString(R.string.failed_to_encrypt_file),Snackbar.LENGTH_LONG);
                    }
                });
            }
        })).start();
    }
    private void encryptFolder(Uri uri){
        createPasswordDialog(this,getLayoutInflater(),(str)-> encryptFolder(uri,str));
    }
    private void encryptFolder(Uri uri,String password){
        DialogManager.ProgressDialogCallback callback = createProgressDialog(this,getLayoutInflater(),R.string.encrypting_in_progress);
        if(callback==null)return;
        new Thread(new FolderEncryptor(this, uri, password, new FolderEncryptor.Callback() {
            @Override
            public void onProgress(int progress) {
                callback.onProgress(progress);
            }

            @Override
            public void onComplete(File file) {
                callback.dismiss();
                runOnUiThread(()->{
                    if(file !=null){
                        addItemToList(new EncryptedFile(DocumentFile.fromFile(file), EncryptedFile.TYPE.FOLDER,file.lastModified()));
                        makeSnackBar(getString(R.string.successfully_encrypted),Snackbar.LENGTH_LONG);
                    }else{
                        makeSnackBar(getString(R.string.failed_to_encrypt_file),Snackbar.LENGTH_LONG);
                    }
                });
            }
        })).start();
    }
    private void decryptFolder(Uri uri){
        createPasswordDialog(this,getLayoutInflater(),password -> {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(R.layout.checking_dialog_view)
                    .setCancelable(false)
                    .show();
            new Thread(()->{
                if(isPasswordRight(this,uri,password)){
                    runOnUiThread(()->{
                        dialog.dismiss();
                        decryptFolder(uri,password);
                    });
                }else{
                    dialog.dismiss();
                    runOnUiThread(()->makeSnackBar(getString(R.string.password_is_wrong),Snackbar.LENGTH_SHORT));
                }
            }).start();
        });
    }
    private void decryptFolder(Uri uri,String password){
        DialogManager.ProgressDialogCallback callback = createProgressDialog(this,getLayoutInflater(),R.string.decrypting_in_progress);
        if(callback==null)return;
        new Thread(new FolderDecoder(this, uri, password, new FolderDecoder.Callback() {
            @Override
            public void onProgress(int progress) {
                callback.onProgress(progress);
            }

            @Override
            public void onComplete(File file) {
                callback.dismiss();
                runOnUiThread(()->{
                    if(file !=null){
                        makeSnackBar(getString(R.string.successfully_decrypted),Snackbar.LENGTH_LONG);
                    }else{
                        makeSnackBar(getString(R.string.failed_to_decrypt_file),Snackbar.LENGTH_LONG);
                    }
                });
            }
        })).start();
    }
    ActivityResultLauncher<String> fileLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if(uri!=null){
                    encryptFile(uri);
                }
    });
    ActivityResultLauncher<Intent> folderLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData()!=null) {
                    Uri uri = result.getData().getData();
                    encryptFolder(uri);
                }
            });

    private void makeSnackBar(String text,int duration){
        if(binding!=null) {
            Snackbar.make(binding.getRoot(), text, duration)
                    .show();
        }
    }
    private void makeSnackBar(String text, int duration, int resId,View.OnClickListener onClickListener){
        if(binding!=null) {
            Snackbar.make(binding.getRoot(), text, duration)
                    .setAction(resId,onClickListener)
                    .show();
        }
    }
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    askForReadPermission();
                }
            });
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}