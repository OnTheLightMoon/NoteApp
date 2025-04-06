package com.example.wtf2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Адаптер для Spinner, отображающий цвета с их названиями и цветными квадратами.
 */
public class ColorSpinnerAdapter extends ArrayAdapter<String> {
    private static final int COLOR_SQUARE_SIZE = 24; // Размер цветного квадрата в dp
    private static final int DRAWABLE_PADDING = 8;   // Отступ между квадратом и текстом в dp

    private final List<String> colors;    // Список цветовых кодов (например, "#FFFFFF")
    private final String[] colorNames;    // Названия цветов

    /**
     * Конструктор адаптера.
     * @param context Контекст приложения
     * @param colors Список цветовых кодов
     * @param colorNames Массив названий цветов
     */
    public ColorSpinnerAdapter(@NonNull Context context, @NonNull List<String> colors, @NonNull String[] colorNames) {
        super(context, android.R.layout.simple_spinner_item, colors);
        this.colors = colors;
        this.colorNames = colorNames;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    /**
     * Создает или обновляет представление для элемента Spinner.
     * @param position Позиция элемента
     * @param convertView Переиспользуемое представление
     * @param parent Родительский контейнер
     * @return Готовое представление
     */
    private View createView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_spinner_dropdown_item, parent, false);
            holder = new ViewHolder();
            holder.textView = convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Проверка на корректность данных
        if (position < 0 || position >= colors.size() || position >= colorNames.length) {
            holder.textView.setText("Ошибка данных");
            return convertView;
        }

        // Настройка текста и цвета
        holder.textView.setText(colorNames[position]);
        holder.textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        holder.textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Очистка старых drawable

        // Создание цветного квадрата
        ColorDrawable colorSquare = new ColorDrawable(Color.parseColor(colors.get(position)));
        colorSquare.setBounds(0, 0, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE);
        holder.textView.setCompoundDrawables(colorSquare, null, null, null);
        holder.textView.setCompoundDrawablePadding(DRAWABLE_PADDING);

        return convertView;
    }

    /**
     * ViewHolder для хранения элементов представления.
     */
    private static class ViewHolder {
        TextView textView;
    }
}