package com.example.lab13Blur;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.Nullable;

public class ImageRunnable implements Runnable
{
    //Исходное изображение
    Bitmap bmp;

    //Итоговое изображение
    Bitmap res;

    //Размеры изображения
    int height;
    int width;

    //Размер ядра свертки
    int sizeCore;

    //Позиции начала и конца обработки по X
    int x0;
    int x1;

    @Nullable float [][] matrix;
    float sumMatrix;
    int matrixWidth;

    boolean isBoxBlur;

    @Override
    public void run()
    {
        if (isBoxBlur)
        {
            int squareCore = sizeCore * sizeCore;

            for (int y = 0; y < height; y++)
            {
                for (int x = x0; x < x1; x++)
                {
                    float red = 0;
                    float green = 0;
                    float blue = 0;

                    for (int coreY = 0; coreY < sizeCore; coreY++)
                    {
                        for (int coreX = 0; coreX < sizeCore; coreX++)
                        {
                            int px = coreX + x - sizeCore / 2;
                            int py = coreX + y - sizeCore / 2;

                            px = Math.min(Math.max(px, 0), width - 1);
                            py = Math.min(Math.max(py, 0), height - 1);

                            int c = bmp.getPixel(px, py);

                            red += Color.red(c);
                            green += Color.green(c);
                            blue += Color.blue(c);
                        }
                    }

                    red /= squareCore;
                    green /= squareCore;
                    blue /= squareCore;

                    res.setPixel(x, y, Color.rgb((int)red, (int)green, (int)blue));

                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            }
        }
        else
        {
            for (int y = 0; y < height; y++)
            {
                for (int x = x0; x < x1; x++)
                {
                    float red = 0;
                    float green = 0;
                    float blue = 0;

                    float matrixValue = 0;

                    for (int coreY = -sizeCore; coreY <= sizeCore; coreY++)
                    {
                        for (int coreX = -sizeCore; coreX <= sizeCore; coreX++)
                        {
                            int px = coreX + x;
                            int py = coreX + y;

                            px = Math.min(Math.max(px, 0), width - 1);
                            py = Math.min(Math.max(py, 0), height - 1);

                            int c = bmp.getPixel(px, py);

                            matrixValue = matrix[coreX + sizeCore][coreY + sizeCore];

                            red += Color.red(c) * matrixValue;
                            green += Color.green(c) * matrixValue;
                            blue += Color.blue(c) * matrixValue;
                        }
                    }

                    res.setPixel(x, y, Color.rgb((int)red, (int)green, (int)blue));

                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            }
        }
    }
}
