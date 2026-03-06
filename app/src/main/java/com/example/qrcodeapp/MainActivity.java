package com.example.qrcodeapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI элементы
    private Button btnScan, btnClear, btnClearHistory;
    private TextView TvResultTitle, TvResultType, TvResultContent, TvResultLink;
    private ImageView IvResultImage;
    private LinearLayout historyContainer;
    private ScrollView scrollView;

    // История сканирований
    private List<ScanHistoryItem> historyList = new ArrayList<>();
    private static final int MAX_HISTORY_ITEMS = 50;

    // Интерфейс для колбэка загрузки изображения
    interface ImageLoadCallback {
        void onImageLoaded(int position);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnScan = findViewById(R.id.btn_scan);
        btnClear = findViewById(R.id.btn_clear);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        TvResultTitle = findViewById(R.id.tv_result_title);
        TvResultType = findViewById(R.id.tv_result_type);
        TvResultContent = findViewById(R.id.tv_result_content);
        TvResultLink = findViewById(R.id.tv_result_link);
        IvResultImage = findViewById(R.id.iv_result_image);
        historyContainer = findViewById(R.id.history_container);
        scrollView = findViewById(R.id.scroll_view);
    }

    private void setupClickListeners() {
        btnScan.setOnClickListener(v -> startQRScanner());
        btnClear.setOnClickListener(v -> clearCurrentResult());
        btnClearHistory.setOnClickListener(v -> clearHistory());
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Наведите камеру на QR-код");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedData = result.getContents();
                    processScannedData(scannedData);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void processScannedData(String data) {
        try {
            // Скрываем все элементы текущего результата
            hideAllResultViews();

            TvResultTitle.setVisibility(View.VISIBLE);

            // Определяем тип данных
            String dataType = determineDataType(data);

            TvResultType.setVisibility(View.VISIBLE);
            //TvResultType.setText("Тип: " + dataType);

            // Обработка формата "URL|Описание"
            if (dataType.equals("Ссылка на изображение с описанием") ||
                    dataType.equals("URL с описанием")) {

                String[] parts = data.split("\\|", 2);
                String imageUrl = parts[0].trim();
                String description = parts[1].trim();

                // Показываем изображение
                if (IvResultImage != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    loadImageFromUrl(imageUrl, position -> {
                        // После загрузки изображения добавляем в историю
                        ScanHistoryItem item = new ScanHistoryItem(
                                data, dataType, description, null, imageUrl);
                        addToHistory(item);
                    });
                }

                // Показываем описание
                if (TvResultContent != null) {
                    TvResultContent.setVisibility(View.VISIBLE);
                    TvResultContent.setText(description);
                    TvResultContent.setGravity(Gravity.CENTER);
                }

                // Показываем ссылку
                if (TvResultLink != null) {
                    TvResultLink.setVisibility(View.VISIBLE);
                    //TvResultLink.setText("Ссылка: " + imageUrl);
                    TvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                        startActivity(browserIntent);
                    });
                }
            }
            // Ссылка на изображение (без описания)
            else if (isImageUrl(data)) {
                if (IvResultImage != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    loadImageFromUrl(data, position -> {
                        ScanHistoryItem item = new ScanHistoryItem(
                                data, dataType, data, null, data);
                        addToHistory(item);
                    });
                }

                if (TvResultLink != null) {
                    TvResultLink.setVisibility(View.VISIBLE);
                    //TvResultLink.setText("Ссылка: " + data);
                    TvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                        startActivity(browserIntent);
                    });
                }
            }
            // Base64 изображение
            else if (isBase64Image(data)) {
                Bitmap bitmap = decodeBase64ToBitmap(data);
                if (bitmap != null && IvResultImage != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    IvResultImage.setImageBitmap(bitmap);

                    ScanHistoryItem item = new ScanHistoryItem(
                            data, dataType, "Base64 изображение", bitmap, null);
                    addToHistory(item);
                } else {
                    if (TvResultContent != null) {
                        TvResultContent.setVisibility(View.VISIBLE);
                        TvResultContent.setText("Ошибка декодирования Base64");
                    }
                }
            }
            // Обычная ссылка
            else if (Patterns.WEB_URL.matcher(data).matches()) {
                if (TvResultLink != null) {
                    TvResultLink.setVisibility(View.VISIBLE);
                    TvResultLink.setText(data);
                    TvResultLink.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                        startActivity(browserIntent);
                    });

                    ScanHistoryItem item = new ScanHistoryItem(
                            data, dataType, data, null, null);
                    addToHistory(item);
                }
            }
            // Текст
            else {
                if (TvResultContent != null) {
                    TvResultContent.setVisibility(View.VISIBLE);
                    TvResultContent.setText(data);
                    TvResultContent.setGravity(Gravity.CENTER);

                    ScanHistoryItem item = new ScanHistoryItem(
                            data, dataType, data, null, null);
                    addToHistory(item);
                }
            }

            // Прокручиваем к результату
            scrollToBottom();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadImageFromUrl(String imageUrl, ImageLoadCallback callback) {
        if (IvResultImage == null) return;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        IvResultImage.setImageDrawable(resource);
                        IvResultImage.setVisibility(View.VISIBLE);

                        if (callback != null) {
                            callback.onImageLoaded(historyList.size());
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        IvResultImage.setImageDrawable(errorDrawable);
                        IvResultImage.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    // ================ МЕТОДЫ ДЛЯ РАБОТЫ С ИСТОРИЕЙ ================

    private void addToHistory(ScanHistoryItem item) {
        // Добавляем в начало списка
        historyList.add(0, item);

        // Ограничиваем размер истории
        if (historyList.size() > MAX_HISTORY_ITEMS) {
            historyList.remove(historyList.size() - 1);
        }

        // Обновляем отображение истории
        updateHistoryDisplay();
    }

    private void updateHistoryDisplay() {
        historyContainer.removeAllViews();

        if (historyList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("История пуста");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 20, 0, 20);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            historyContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < historyList.size(); i++) {
            ScanHistoryItem item = historyList.get(i);
            View historyItemView = getLayoutInflater().inflate(R.layout.history_item, historyContainer, false);

            TextView typeView = historyItemView.findViewById(R.id.history_item_type);
            TextView timeView = historyItemView.findViewById(R.id.history_item_time);
            TextView textView = historyItemView.findViewById(R.id.history_item_text);
            ImageView imageView = historyItemView.findViewById(R.id.history_item_image);

            typeView.setText(item.getDataType());
            timeView.setText(item.getTimestamp());

            // Устанавливаем текст (обрезаем если длинный)
            String displayText = item.getDisplayText();
            if (displayText.length() > 50) {
                displayText = displayText.substring(0, 47) + "...";
            }
            textView.setText(displayText);

            // Если есть изображение
            if (item.hasImage()) {
                imageView.setVisibility(View.VISIBLE);

                if (item.getImageBitmap() != null) {
                    // Base64 изображение
                    imageView.setImageBitmap(item.getImageBitmap());
                } else if (item.getImageUrl() != null) {
                    // URL изображения
                    Glide.with(this)
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.placeholder)
                            .error(R.drawable.error)
                            .centerCrop()
                            .into(imageView);
                }
            }

            // Добавляем обработчик клика на элемент истории
            final int position = i;
            historyItemView.setOnClickListener(v -> restoreFromHistory(position));

            historyContainer.addView(historyItemView);
        }
    }

    private void restoreFromHistory(int position) {
        if (position >= 0 && position < historyList.size()) {
            ScanHistoryItem item = historyList.get(position);

            // Очищаем текущий результат
            clearCurrentResult();

            // Восстанавливаем данные
            TvResultTitle.setVisibility(View.VISIBLE);
            TvResultType.setVisibility(View.VISIBLE);
            //TvResultType.setText("Тип: " + item.getDataType());

            if (item.hasImage()) {
                if (item.getImageBitmap() != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    IvResultImage.setImageBitmap(item.getImageBitmap());
                } else if (item.getImageUrl() != null) {
                    IvResultImage.setVisibility(View.VISIBLE);
                    loadImageFromUrl(item.getImageUrl(), null);
                }
            }

            if (item.getDisplayText() != null && !item.getDisplayText().isEmpty()) {
                TvResultContent.setVisibility(View.VISIBLE);
                TvResultContent.setText(item.getDisplayText());
                TvResultContent.setGravity(Gravity.CENTER);
            }

            if (item.getRawData() != null && Patterns.WEB_URL.matcher(item.getRawData()).matches()) {
                TvResultLink.setVisibility(View.VISIBLE);
                TvResultLink.setText(item.getRawData());
                TvResultLink.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getRawData()));
                    startActivity(browserIntent);
                });
            }

            Toast.makeText(this, "Восстановлено из истории", Toast.LENGTH_SHORT).show();
            scrollToBottom();
        }
    }

    private void clearHistory() {
        historyList.clear();
        updateHistoryDisplay();
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
    }

    // ================ МЕТОДЫ ДЛЯ ОПРЕДЕЛЕНИЯ ТИПОВ ДАННЫХ ================

    private String determineDataType(String data) {
        if (data == null || data.isEmpty()) return "Пусто";

        // Проверяем на формат "URL|Описание"
        if (data.contains("|")) {
            String[] parts = data.split("\\|", 2);
            if (parts.length == 2) {
                String possibleUrl = parts[0].trim();
                if (Patterns.WEB_URL.matcher(possibleUrl).matches()) {
                    if (isImageUrl(possibleUrl)) {
                        return "Ссылка на изображение с описанием";
                    }
                    return "URL с описанием";
                }
            }
        }

        // Проверка на URL
        if (Patterns.WEB_URL.matcher(data).matches()) {
            if (isImageUrl(data)) {
                return "Ссылка на изображение";
            }
            return "URL";
        }

        // Проверка на Email
        if (Patterns.EMAIL_ADDRESS.matcher(data).matches()) {
            return "Email";
        }

        // Проверка на телефон
        if (data.matches("^[+]?[0-9\\s-]{7,}$")) {
            return "Телефон";
        }

        // Проверка на WiFi
        if (data.startsWith("WIFI:")) {
            return "WiFi";
        }

        // Проверка на Base64 изображение
        if (isBase64Image(data)) {
            return "Изображение (Base64)";
        }

        return "Текст";
    }

    private boolean isImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lowerUrl = url.toLowerCase();
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"};

        for (String ext : imageExtensions) {
            if (lowerUrl.contains(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBase64Image(String data) {
        if (data == null || data.length() < 100) return false;

        try {
            String base64Data = data;
            if (data.contains(",")) {
                base64Data = data.split(",")[1];
            }

            if (!base64Data.matches("^[A-Za-z0-9+/=]+$")) {
                return false;
            }

            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

            return options.outWidth > 0 && options.outHeight > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64Data) {
        try {
            String base64Image = base64Data;
            if (base64Data.contains(",")) {
                base64Image = base64Data.split(",")[1];
            }
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void hideAllResultViews() {
        if (TvResultContent != null) TvResultContent.setVisibility(View.GONE);
        if (TvResultLink != null) TvResultLink.setVisibility(View.GONE);
        if (IvResultImage != null) IvResultImage.setVisibility(View.GONE);
        if (TvResultType != null) TvResultType.setVisibility(View.GONE);
    }

    private void clearCurrentResult() {
        hideAllResultViews();
        TvResultTitle.setVisibility(View.GONE);
    }

    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
}