package com.example.wtf2;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

public class ColorSpinnerAdapter extends ArrayAdapter<String> {
    private final List<String> colors;
    private final String[] colorNames;

    public ColorSpinnerAdapter(@NonNull Context context, List<String> colors, String[] colorNames) {
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

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(colorNames[position]);
        textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Убираем стандартные иконки

        // Добавляем цветной квадрат слева
        View colorSquare = new View(getContext());
        colorSquare.setLayoutParams(new ViewGroup.LayoutParams(24, 24));
        colorSquare.setBackgroundColor(Color.parseColor(colors.get(position)));
        textView.setCompoundDrawablesWithIntrinsicBounds(colorSquare.getBackground(), null, null, null);
        textView.setCompoundDrawablePadding(8);

        return convertView;
    }
}