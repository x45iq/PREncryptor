package com.purplerat.prencryptor;

import static com.purplerat.prencryptor.KeyboardManager.hideKeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.purplerat.prencryptor.databinding.FillPasswordViewBinding;
import com.purplerat.prencryptor.databinding.ProgressViewBinding;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DialogManager {
    @SuppressLint("StaticFieldLeak")
    private static Context currentContext = null;
    private static final AtomicBoolean passwordDialogEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean progressDialogEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean encryptingTypeDialogEnabled = new AtomicBoolean(false);
    private static final Lock lock = new ReentrantLock();
    @SuppressLint("ClickableViewAccessibility")
    public static void createPasswordDialog(Context context, LayoutInflater layoutInflater, passwordConsumer passwordCallback){
        if(contextCheck(context)) {
            lock.lock();
            if (passwordDialogEnabled.get()) return;
            passwordDialogEnabled.set(true);
            lock.unlock();
        }
        final FillPasswordViewBinding passBind = FillPasswordViewBinding.inflate(layoutInflater);
        final TextInputEditText tiet = passBind.passwordField;
        final TextInputLayout til = passBind.passwordBox;
        tiet.setOnTouchListener((view, motionEvent) -> {
            til.setErrorEnabled(false);
            return false;
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.enter_password))
                .setView(passBind.getRoot())
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> til.clearFocus())
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> til.clearFocus())
                .setCancelable(false)
                .setOnDismissListener(dialogInterface -> passwordDialogEnabled.set(false))
                .create();
        dialog.show();
        dialog.getWindow().getDecorView().setOnClickListener(v->{
            hideKeyboard(context,til);
            til.clearFocus();
        });
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{
            hideKeyboard(context,til);
            til.clearFocus();
            final String password = tiet.getText()==null?null:tiet.getText().toString().trim();
            if(password==null||password.trim().isEmpty()){
                til.setError(context.getString(R.string.invalid_password));
                return;
            }
            dialog.dismiss();
            passwordCallback.onComplete(password);
        });
    }
    public static ProgressDialogCallback createProgressDialog(Context context, LayoutInflater layoutInflater,int tittleId){
        if(contextCheck(context)) {
            lock.lock();
            if (progressDialogEnabled.get()) return null;
            progressDialogEnabled.set(true);
            lock.unlock();
        }
        final ProgressViewBinding binding = ProgressViewBinding.inflate(layoutInflater);
        final ProgressBar progressBar = binding.progress;
        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(tittleId)
                .setMessage(context.getString(R.string.do_not_close_the_app))
                .setView(binding.getRoot())
                .setCancelable(false)
                .setOnDismissListener(dialogInterface -> progressDialogEnabled.set(false))
                .show();
        return new ProgressDialogCallback() {
            @Override
            public void onProgress(int progress) {
                progressBar.setProgress(progress);
            }

            @Override
            public void onSecondaryProgress(int progress) {
                progressBar.setSecondaryProgress(progress);
            }

            @Override
            public void dismiss() {
                dialog.dismiss();
            }
        };

    }
    public static void createEncryptingTypeDialog(Context context,String[] values,Runnable[] actions){
        if(contextCheck(context)) {
            lock.lock();
            if (encryptingTypeDialogEnabled.get()) return;
            encryptingTypeDialogEnabled.set(true);
            lock.unlock();
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.choose_encrypting_type))
                .setItems(values, (dialogInterface, i) -> {
                    actions[i].run();
                })
                .setCancelable(true)
                .setOnDismissListener(dialogInterface -> encryptingTypeDialogEnabled.set(false))
                .show();
    }
    private static boolean contextCheck(Context context){
        if(context.equals(currentContext)){
            return true;
        }else{
            currentContext = context;
            return false;
        }
    }

    public interface passwordConsumer{
        void onComplete(String password);
    }
    public interface ProgressDialogCallback{
        void onProgress(int progress);
        void onSecondaryProgress(int progress);
        void dismiss();
    }
}
