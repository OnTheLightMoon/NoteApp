package com.example.wtf2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Устанавливаем макет главной активности

        // Инициализация кнопки "Добавить"
        FloatingActionButton fab = findViewById(R.id.add_btn);

        // Получаем TabLayout
        TabLayout tabLayout = findViewById(R.id.tabs);
        View searchView = findViewById(R.id.search);
        View filterButton = findViewById(R.id.imageButton);

        // ✅ Загружаем NotesListFragment при первом запуске
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotesListFragment())
                    .commit();
        }

        // Устанавливаем обработчик нажатия на кнопку
        fab.setOnClickListener(v -> {
            int selectedTabPosition = tabLayout.getSelectedTabPosition(); // Получаем индекс текущей вкладки

            if (selectedTabPosition == 0) { // Если выбрана первая вкладка ("Все")
                Intent intent = new Intent(MainActivity.this, NoteEditor.class);
                startActivity(intent);
            } else if (selectedTabPosition == 1) { // Если выбрана вторая вкладка ("Папки")
                showPopupDialog(); // Вызываем метод для отображения всплывающего окна
            }
        });

        //ОТЛАДКА
//        if (savedInstanceState == null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new NotesListFragment()) // 👈 Загружаем фрагмент "Все"
//                    .commit();
//        }


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selectedFragment;
                if (tab.getPosition() == 1) { // "Папки"
                    selectedFragment = new FoldersListFragment();
                    searchView.setVisibility(View.GONE);
                    filterButton.setVisibility(View.GONE);
                } else {
                    selectedFragment = new NotesListFragment();
                    searchView.setVisibility(View.VISIBLE);
                    filterButton.setVisibility(View.VISIBLE);
                }
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });


    }

    // Метод для отображения всплывающего окна добавления папки
    private void showPopupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создать новую папку");
        builder.setMessage("Введите название новой папки:");

        EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (folderName.isEmpty()) {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("DEBUG", "Добавляем папку: " + folderName); // ✅ Проверяем, вызывается ли метод

            // ✅ Получаем FoldersFragment через FragmentManager
            FoldersListFragment foldersFragment = (FoldersListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (foldersFragment != null) {
                foldersFragment.addNewFolder(folderName);
            }else {
                Log.e("DEBUG", "FoldersFragment == null! Переключаем вкладку вручную.");
                switchToFoldersListFragment(folderName);
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void switchToFoldersListFragment(String folderName) {
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.getTabAt(1).select(); // ✅ Переключаемся на вкладку "Папки"

        new Handler().postDelayed(() -> {
            FoldersListFragment foldersFragment = (FoldersListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (foldersFragment != null) {
                foldersFragment.addNewFolder(folderName);
            } else {
                Log.e("DEBUG", "Ошибка: FoldersFragment всё ещё null после переключения!");
            }
        }, 300); // ✅ Ждём 300 мс, чтобы фрагмент успел загрузиться
    }




}