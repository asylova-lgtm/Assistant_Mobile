package com.example.qrcodeapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private Button btnScan;
    private TextView tvResultContent, tvResultLink, tvResultTitle, tvResultType;
    private ImageView ivResultImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация элементов
        btnScan = findViewById(R.id.btn_scan);
        tvResultContent = findViewById(R.id.tv_result_content);
        tvResultLink = findViewById(R.id.tv_result_link);
        tvResultType = findViewById(R.id.tv_result_type);
        tvResultTitle = findViewById(R.id.tv_result_title);
        ivResultImage = findViewById(R.id.iv_result_image);

        // Обработчик нажатия на кнопку сканирования
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQRScanner();
            }
        });
    }

    // Метод для запуска сканера QR-кода
    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Наведите камеру на QR-код");
        integrator.setCameraId(0); // Использовать заднюю камеру
        integrator.setBeepEnabled(true); // Звуковой сигнал при сканировании
        integrator.setBarcodeImageEnabled(true); // Сохранять изображение QR-кода
        integrator.initiateScan();
    }

    // Обработка результата сканирования
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // Сканирование отменено
                Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show();
            } else {
                // Получен результат сканирования
                String scannedData = result.getContents();
                processScannedData(scannedData);
            }
        }
    }

    // Метод для обработки и отображения отсканированных данных
    private void processScannedData(String data) {
        try {
            // Скрываем все элементы
            hideAllResultViews();

            if (tvResultTitle != null) {
                tvResultTitle.setVisibility(View.VISIBLE);
            }

            // Определяем тип данных
            String dataType = determineDataType(data);

            if (tvResultType != null) {
                tvResultType.setVisibility(View.VISIBLE);
                //tvResultType.setText("Тип: " + dataType);
            }

            // ️ НОВОЕ: Обработка формата "URL|Описание"
            if (dataType.equals("Ссылка на изображение с описанием") ||
                    dataType.equals("URL с описанием")) {

                // Разделяем строку
                String[] parts = data.split("\\|", 2);
                String imageUrl = parts[0].trim();
                String description = parts[1].trim();

                // Показываем изображение
                if (ivResultImage != null) {
                    ivResultImage.setVisibility(View.VISIBLE);
                    loadImageFromUrl(imageUrl);
                }

                // Показываем описание
                if (tvResultContent != null) {
                    tvResultContent.setVisibility(View.VISIBLE);
                    tvResultContent.setText(description);
                    tvResultContent.setGravity(Gravity.CENTER); // Центрируем текст
                }

                // Показываем ссылку (опционально)
                /*if (tvResultLink != null) {
                    tvResultLink.setVisibility(View.VISIBLE);
                    tvResultLink.setText("Ссылка: " + imageUrl);
                    tvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                        startActivity(browserIntent);
                    });
                }*/
            }
            // 1. ССЫЛКА НА ИЗОБРАЖЕНИЕ (без описания)
            else if (isImageUrl(data)) {
                if (ivResultImage != null) {
                    ivResultImage.setVisibility(View.VISIBLE);
                    loadImageFromUrl(data);
                }

                if (tvResultLink != null) {
                    tvResultLink.setVisibility(View.VISIBLE);
                    //tvResultLink.setText("Ссылка: " + data);
                    tvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                        startActivity(browserIntent);
                    });
                }
            }
            /*// 2. BASE64 ИЗОБРАЖЕНИЕ
            else if (isBase64Image(data)) {
                Bitmap bitmap = decodeBase64ToBitmap(data);
                if (bitmap != null && IvResultImage != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    IvResultImage.setImageBitmap(bitmap);
                } else {
                    if (TvResultContent != null) {
                        TvResultContent.setVisibility(View.VISIBLE);
                        TvResultContent.setText("Ошибка декодирования Base64");
                    }
                }
            }*/
            // 3. ОБЫЧНАЯ ССЫЛКА
            else if (Patterns.WEB_URL.matcher(data).matches()) {
                if (tvResultLink != null) {
                    tvResultLink.setVisibility(View.VISIBLE);
                    tvResultLink.setText(data);
                    tvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                        startActivity(browserIntent);
                    });
                }
            }
            // 4. ТЕКСТ
            else {
                if (tvResultContent != null) {
                    tvResultContent.setVisibility(View.VISIBLE);
                    tvResultContent.setText(data);
                    tvResultContent.setGravity(Gravity.CENTER); // Центрируем текст
                }
            }

            // Прокручиваем к результату
            scrollToBottom();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void scrollToBottom() {
        ScrollView scrollView = findViewById(R.id.scroll_view);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private boolean isImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        // Приводим к нижнему регистру для проверки
        String lowerUrl = url.toLowerCase();

        // Список расширений изображений
        String[] imageExtensions = {
                ".jpg", ".jpeg", ".png", ".gif", ".bmp",
                ".webp", ".svg", ".ico", ".tiff"
        };

        // Проверяем, заканчивается ли ссылка на расширение картинки
        for (String ext : imageExtensions) {
            if (lowerUrl.contains(ext)) {
                return true;
            }
        }

        /*// Дополнительная проверка: может ссылка содержит "image" в названии
        if (lowerUrl.contains("image") || lowerUrl.contains("img") || lowerUrl.contains("photo")) {
            return true;
        }

        // Проверка на популярные хостинги изображений
        String[] imageHostings = {
                "instagram.com", "imgur.com", "flickr.com",
                "cloudfront.net", "amazonaws.com"
        };

        for (String hosting : imageHostings) {
            if (lowerUrl.contains(hosting)) {
                return true;
            }
        }*/

        return false;
    }

    /**
     * Проверяет, является ли строка Base64 изображением
     * @param data строка для проверки
     * @return true если это Base64 картинка
     */
    /*private boolean isBase64Image(String data) {
        if (data == null || data.length() < 100) return false; // Base64 картинка обычно длинная

        try {
            // Убираем префикс если есть (data:image/png;base64,)
            String base64Data = data;
            if (data.contains(",")) {
                base64Data = data.split(",")[1];
            }

            // Проверяем, что строка содержит только допустимые символы Base64
            if (!base64Data.matches("^[A-Za-z0-9+/=]+$")) {
                return false;
            }

            // Пробуем декодировать
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);

            // Проверяем, что декодированные байты могут быть изображением
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // Не создавать Bitmap, только узнать размеры
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

            // Если есть ширина и высота - это изображение
            return options.outWidth > 0 && options.outHeight > 0;

        } catch (IllegalArgumentException e) {
            // Ошибка декодирования Base64
            return false;
        } catch (Exception e) {
            return false;
        }
    }*/


    // Метод для определения типа данных
    private String determineDataType(String data) {
        if (data == null || data.isEmpty()) return "Пусто";

        //  НОВОЕ: Проверяем на формат "URL|Описание"
        if (data.contains("|")) {
            String[] parts = data.split("\\|", 2); // Разделяем на 2 части
            if (parts.length == 2) {
                String possibleUrl = parts[0].trim();
                // Проверяем, является ли первая часть URL
                if (Patterns.WEB_URL.matcher(possibleUrl).matches()) {
                    // Дополнительно проверяем, может это ссылка на картинку
                    if (isImageUrl(possibleUrl)) {
                        return "Ссылка на изображение с описанием";
                    }
                    return "URL с описанием";
                }
            }
        }

        // Проверка на URL
        if (Patterns.WEB_URL.matcher(data).matches()) {
            // Проверяем, может это ссылка на картинку
            if (isImageUrl(data)) {
                return "Ссылка на изображение";
            }
            return "URL";
        }

        // Проверка на Email
        if (Patterns.EMAIL_ADDRESS.matcher(data).matches()) {
            return "Email";
        }

        /*// Проверка на Base64 изображение
        if (isBase64Image(data)) {
            return "Изображение (Base64)";
        }*/

        return "Текст";
    }


    // Метод для декодирования Base64 в Bitmap
    private Bitmap decodeBase64ToBitmap(String base64Data) {
        try {
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void loadImageFromUrl(String imageUrl) {
        // Проверяем, что ImageView существует
        if (ivResultImage == null) return;

        try {
            Glide.with(this)
                    .load(imageUrl)                    // URL картинки
                    .placeholder(R.drawable.placeholder) // что показывать пока грузится
                    .error(R.drawable.error)           // что показывать при ошибке
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // кэширование
                    .centerCrop()                       // как масштабировать
                    .into(ivResultImage);               // куда загружать
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
        }
    }


    // Скрыть все элементы результатов
    private void hideAllResultViews() {
        tvResultContent.setVisibility(View.GONE);
        tvResultLink.setVisibility(View.GONE);
        ivResultImage.setVisibility(View.GONE);
        tvResultType.setVisibility(View.GONE);

        // Убираем клик-слушатели
        tvResultContent.setOnClickListener(null);
        tvResultLink.setOnClickListener(null);
    }
}
