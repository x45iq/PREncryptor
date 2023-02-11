package com.purplerat.prencryptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.io.File;


public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ContactViewHolder> {
    private final AdapterCallback adapterCallback;
    public MainAdapter(AdapterCallback adapterCallback){
        this.adapterCallback = adapterCallback;
    }
    private final SortedList<EncryptedFile> files = new SortedList<>(EncryptedFile.class, new SortedList.Callback<EncryptedFile>() {
        @Override
        public int compare(EncryptedFile file, EncryptedFile file2) {
            if(file.equals(file2))return 0;
            return Long.compare(file.getEncryptedTime(),file2.getEncryptedTime());
        }

        @Override
        public boolean areContentsTheSame(EncryptedFile file1, EncryptedFile file2) {
            return file1.equals(file2);
        }

        @Override
        public boolean areItemsTheSame(EncryptedFile file1, EncryptedFile file2) {
            return file1.equals(file2);
        }

        @Override
        public void onChanged(int pos, int count) {
            notifyItemRangeChanged(pos, count);
        }

        @Override
        public void onInserted(int pos, int count) {
            notifyItemRangeInserted(pos, count);
        }

        @Override
        public void onRemoved(int pos, int count) {
            notifyItemRangeRemoved(pos, count);
        }

        @Override
        public void onMoved(int pos, int count) {
            notifyItemMoved(pos, count);
        }
    });

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ContactViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.main_recycle_item, viewGroup, false),adapterCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder contactViewHolder, int i) {
        contactViewHolder.bind(files.get(i));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void putItem(EncryptedFile contact) {
        files.add(contact);
    }

    public void removeItemAt(int index) {
        files.removeItemAt(index);
    }
    public EncryptedFile getItemAt(int index){
        return files.get(index);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;
        private final ImageView imageView;
        private EncryptedFile file;


        public ContactViewHolder(@NonNull View itemView,AdapterCallback adapterCallback) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            imageView = itemView.findViewById(R.id.imageView);
            itemView.setOnClickListener(view -> {
                if(file!=null){
                    adapterCallback.onItemClicked(file);
                }
            });
        }

        public void bind(EncryptedFile file) {
            this.file = file;
            textView.setText(file.getName());
            imageView.setImageResource(file.getType() == EncryptedFile.TYPE.FILE?R.drawable.ic_baseline_insert_drive_file_24:R.drawable.ic_baseline_folder_24);
        }
    }
    public interface AdapterCallback{
        void onItemClicked(EncryptedFile file);
    }
}