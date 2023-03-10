package com.example.lab13Blur;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener
{
    Spinner sp_CountThreads;
    SeekBar seekBar_SizeCore;
    TextView tv_SizeCore;
    TextView tv_Time;
    SurfaceView sv;
    RadioButton rb_BoxBlur;
    RadioButton rb_GaussianBlur;

    String txt_SizeCore = "Size core = ";
    String txt_Time = "Time: ";

    int sizeCore;
    int countThreads;

    float second;
    float sumMatrix;
    float[][] matrix;

    boolean isBoxBlur;

    Bitmap res;

    Thread[] t;

    Date dateBefore;
    Date dateAfter;

    //Получение времени прошедшего с момента запуска потоков обработки до их завершения
    public void GetTime()
    {
        //Получение текущей даты
        dateAfter = new Date();

        //Вычисление прошедшего времени в секундах
        second = dateAfter.getTime() - dateBefore.getTime();
        second = second / 1000;

        //Вывод информации на экран
        tv_Time.setText(txt_Time + String.format("%.2f", second) + "s");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp_CountThreads = findViewById(R.id.sp_CountThreads);
        rb_BoxBlur = findViewById(R.id.rb_BoxBlur);
        rb_GaussianBlur = findViewById(R.id.rb_GaussianBlur);

        seekBar_SizeCore = findViewById(R.id.seekBar_SizeCore);
        seekBar_SizeCore.setMin(3);
        seekBar_SizeCore.setOnSeekBarChangeListener(this);
        sizeCore = 3;

        tv_SizeCore = findViewById(R.id.tv_seekBar);
        tv_SizeCore.setText(txt_SizeCore + seekBar_SizeCore.getProgress());

        tv_Time = findViewById(R.id.tv_Time);

        sv = findViewById(R.id.surfaceView);
    }

    //Обработка изображения
    public void ImageProcessing(View view)
    {
        //Проверка на незавершенные потоки
        if (t != null)
        {
            for (int i = 0; i < t.length; i++)
            {
                //Если есть незавершенные потоки, то прерываем их
                if (!t[i].isInterrupted())
                {
                    t[i].interrupt();
                }
            }
        }

        //Исходное изображение
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.game);

        int width = bmp.getWidth();
        int height = bmp.getHeight();

        //Получение пустого изображения по размерам исходного
        res = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        countThreads = (int)sp_CountThreads.getSelectedItemId() + 1;

        t = new Thread[countThreads];
        ImageRunnable[] ir = new ImageRunnable[countThreads];

        //Ширина изображения, которая будет обрабатыватсья одним потоком
        int coef = width/countThreads;

        //Проверка на выбранный режим размытия
        if (rb_BoxBlur.isChecked())
        {
            isBoxBlur = true;
            matrix = new float[0][0];
        }
        else
        {
            isBoxBlur = false;

            float sigma = sizeCore / 2;

            int matrixWidth = (2 * sizeCore) + 1;

            matrix = new float[matrixWidth][matrixWidth];
            sumMatrix = 0;

            for (int x = -sizeCore; x <= sizeCore; x++)
            {
                for (int y = -sizeCore; y <= sizeCore; y++)
                {
                    float degree = (-(x * x + y * y)) / (2 * sigma * sigma);
                    float matrixValue = (float) (Math.pow(Math.E, degree) / (2 * Math.PI * sigma * sigma));

                    matrix[x + sizeCore][y + sizeCore] = matrixValue;
                    sumMatrix += matrixValue;
                }
            }

            for (int x = 0; x < matrixWidth; x++)
            {
                for (int y = 0; y < matrixWidth; y++)
                {
                    matrix[x][y] /= sumMatrix;
                }
            }
        }

        //Постоянная перерисовка картинки
        Runnable svInvalidate = () ->
        {
            while (true)
            {
                sv.invalidate();
                if (Thread.currentThread().isInterrupted())
                {
                    return;
                }
            }
        };

        Thread t_svInvalidate = new Thread(svInvalidate);
        t_svInvalidate.setName("Invalidate");
        t_svInvalidate.start();

        //Запуск потоков обработки
        Runnable startThreads = () ->
        {
            for (int i = 0; i < countThreads; i++)
            {
                ir[i] = new ImageRunnable();
                ir[i].bmp = bmp;
                ir[i].res = res;
                ir[i].height = height;
                ir[i].width = width;
                ir[i].sizeCore = sizeCore;
                ir[i].x0 = i * coef;
                ir[i].x1 = ir[i].x0 + coef;
                ir[i].isBoxBlur = isBoxBlur;
                ir[i].matrix = matrix;
                ir[i].sumMatrix = sumMatrix;
                ir[i].matrixWidth = matrix.length;

                t[i] = new Thread(ir[i]);
                t[i].setName("Processing thread number " + i);
                t[i].start();
            }

            //Ожидание окончания всех потоков
            for (int i = 0; i < countThreads; i++)
            {
                try
                {
                    t[i].join();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            t_svInvalidate.interrupt();

            GetTime();
        };

        //Время перед началом обработки изображения
        dateBefore = new Date();

        Thread thread = new Thread(startThreads);
        thread.setName("Start threads processing");
        thread.start();

        //Получение обработанного изображения и его установка
        Drawable d = new BitmapDrawable(getResources(), res);
        sv.setForeground(d);
    }

    //Остановка всех потоков
    public void StopProcessing(View v)
    {
        for (int i = 0; i < countThreads; i++)
        {
            if (!t[i].isInterrupted())
            {
                t[i].interrupt();
            }
        }

        GetTime();
    }

    //Получение размера ядра свертки
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        sizeCore = seekBar.getProgress();
        tv_SizeCore.setText(txt_SizeCore + sizeCore);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {

    }
}