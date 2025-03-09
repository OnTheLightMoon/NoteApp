package com.example.wtf2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FoldersListFragment extends Fragment implements FolderAdapter.OnFolderClickListener {

    private RecyclerView recyclerView;
    private FolderAdapter folderAdapter;
    private List<Folder> folderList;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.folders, container, false);

        recyclerView = view.findViewById(R.id.folders_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(folderList, this);
        recyclerView.setAdapter(folderAdapter); // ✅ Устанавливаем адаптер перед загрузкой

        db = AppDatabase.getInstance(getContext());

        //ОТЛАДКА
        if (recyclerView == null) {
            Log.e("DEBUG", "RecyclerView в FoldersFragment == null!");
        } else {
            Log.d("DEBUG", "RecyclerView в FoldersFragment найден!");
        }


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFolders(); // ✅ Загружаем папки после возврата во фрагмент
    }

    /**
     * Загружает список папок из базы данных.
     */
    private void loadFolders() {
        Executors.newSingleThreadExecutor().execute(() -> { // ✅ Запускаем в фоновом потоке (Room требует этого)
            List<Folder> folders = db.folderDao().getAllFolders();

            Log.d("DEBUG", "Количество папок в базе: " + folders.size()); // ✅ Логируем количество папок
            for (Folder folder : folders) {
                Log.d("DEBUG", "Папка: " + folder.getId() + " - " + folder.getName()); // ✅ Проверяем ID и имя папки
            }

            getActivity().runOnUiThread(() -> { // ✅ Обновляем UI в главном потоке
                folderList.clear();
                folderList.addAll(folders);
                folderAdapter.notifyDataSetChanged(); // ✅ Уведомляем адаптер об изменениях

                Log.d("DEBUG", "Обновлено папок в списке: " + folderList.size()); // ✅ Логируем количество папок в UI
            });
        });
    }


    /**
     * Добавляет новую папку в базу данных.
     */
    public void addNewFolder(String folderName) {
        if (folderName.trim().isEmpty()) {
            Toast.makeText(getContext(), "Название папки не может быть пустым", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEBUG", "Перед вставкой папки: " + folderName);

        Executors.newSingleThreadExecutor().execute(() -> {
            db.folderDao().insertFolder(folderName); // 🔥 Используем транзакцию
            Log.d("DEBUG", "Папка успешно вставлена в Room: " + folderName);
            getActivity().runOnUiThread(() -> {
                loadFolders();
                Log.d("DEBUG", "Вызван loadFolders() после вставки");
            });
        });
    }


    /**
     * При нажатии на папку открывается список заметок этой папки.
     */

    @Override
    public void onFolderClick(Folder folder) {
        // Что делать, когда папка нажата
        Log.d("DEBUG", "Открываем папку: " + folder.getName());

        // Открываем NotesListFragment и передаем имя папки
        NotesListFragment notesListFragment = new NotesListFragment();
        Bundle args = new Bundle();
        args.putString("folder_name", folder.getName());
        notesListFragment.setArguments(args);

        // Переходим на экран с заметками из выбранной папки
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, notesListFragment)
                .addToBackStack(null)
                .commit();
    }
}

