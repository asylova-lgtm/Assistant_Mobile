package com.example.qrcodeapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
    private static final String LOCAL_PREFIX = "local://";
    // UI элементы
    private Button btnScan, btnClear, btnClearHistory, btnShowDescription;
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
        btnShowDescription = findViewById(R.id.btn_show_description);

    }

    private void setupClickListeners() {
        btnScan.setOnClickListener(v -> startQRScanner());
        btnClear.setOnClickListener(v -> clearCurrentResult());
        btnClearHistory.setOnClickListener(v -> clearHistory());
        if (btnShowDescription != null) {
            btnShowDescription.setOnClickListener(v -> showLastScanDescription());
        }
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
        Log.d("QR_Scan", "Полученные данные: " + data);
        Log.d("QR_Scan", "Длина: " + data.length());
        try {
            // Скрываем все элементы текущего результата
            hideAllResultViews();

            TvResultTitle.setVisibility(View.VISIBLE);
            // ⭐ НОВОЕ: Проверка на локальную картинку
            if (data.startsWith(LOCAL_PREFIX)) {
                // Парсим данные
                String[] parsed = parseLocalImageData(data);
                String imageName = parsed[0];
                String description = parsed[1];

                // Показываем картинку
                boolean imageLoaded = loadLocalImage(imageName);

                // Если есть описание - показываем
                if (description != null && !description.isEmpty()) {
                    if (TvResultContent != null) {
                        TvResultContent.setVisibility(View.VISIBLE);
                        TvResultContent.setText(description);
                        TvResultContent.setGravity(Gravity.CENTER);
                    }

                    // ⭐ Проверяем, является ли описание химическим элементом
                    if (isChemicalElement(description)) {
                        TvResultType.setText("Тип: Химический элемент (локальная картинка)");

                        String symbol = getChemicalSymbolFromName(description);
                        if (symbol != null) {
                            ChemicalData.ChemicalElement element = ChemicalData.getElement(symbol);
                            if (element != null) {
                                // Показываем диалог с информацией об элементе
                                new android.os.Handler().postDelayed(() -> showElementDialog(element), 500);
                            }
                        }
                    } else {
                        TvResultType.setText("Тип: Локальное изображение с описанием");
                    }
                } else {
                    TvResultType.setText("Тип: Локальное изображение");
                }

                // Сохраняем в историю
                String displayText = description != null ? description : imageName;
                ScanHistoryItem item = new ScanHistoryItem(
                        data,
                        TvResultType.getText().toString(),
                        displayText,
                        null,
                        null
                );
                addToHistory(item);

                scrollToBottom();
                return; // Выходим, чтобы не обрабатывать дальше
            }

            // Определяем тип данных
            String dataType = determineDataType(data);
            Log.d("QR_Scan", "Тип: " + dataType);

            TvResultType.setVisibility(View.VISIBLE);
            //TvResultType.setText("Тип: " + dataType);



            // Обработка формата "URL|Описание"
            if (dataType.equals("Ссылка на изображение с описанием") ||
                    dataType.equals("URL с описанием") ||
                    dataType.equals("Химический элемент с изображением")) {
                Log.d("QR_Scan", "Вошли в блок URL|Описание: ");

                String[] parts = data.split("\\|", 2);
                String imageUrl = parts[0].trim();
                String description = parts[1].trim();

                Log.d("QR_Scan", "URL: " + imageUrl);
                Log.d("QR_Scan", "Описание: " + description);

                // Показываем описание
                if (TvResultContent != null) {
                    Log.d("QR_Scan", "Описание в TvResult");
                    TvResultContent.setVisibility(View.VISIBLE);
                    TvResultContent.setText(description);
                    TvResultContent.setGravity(Gravity.CENTER);
                } else{
                    Log.d("QR_Scan", "Описание == null! ");
                }

                // ⭐ 2. ПРОВЕРЯЕМ, ЯВЛЯЕТСЯ ЛИ ОПИСАНИЕ ХИМИЧЕСКИМ ЭЛЕМЕНТОМ
                boolean isChem = isChemicalElement(description);
                Log.d("QR_Scan", "Является химическим элементом? " + isChem);

                if (isChem) {
                    Log.d("QR_Scan", "Это химический элемент!");
                    TvResultType.setText("Тип: Химический элемент");

                    // Получаем символ элемента из описания
                    String symbol = getChemicalSymbolFromName(description);
                    Log.d("QR_Scan", "Символ из описания: " + symbol);

                    if (symbol != null) {
                        ChemicalData.ChemicalElement element = ChemicalData.getElement(symbol);
                        Log.d("QR_Scan", "Найден элемент в ChemicalData: " + (element != null ? element.getName() : "null"));

                        if (element != null) {
                            Log.d("QR_Scan", "Показываем диалог с элементом");
                            //new android.os.Handler().postDelayed(() -> showElementDialog(element), 300);
                        } else {
                            Log.e("QR_Scan", "Элемент не найден в ChemicalData для символа: " + symbol);
                        }
                    } else {
                        Log.e("QR_Scan", "Не удалось получить символ из описания: " + description);
                    }
                }

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

                // ⭐ Если это химический элемент - показываем диалог
                if (dataType.equals("Химический элемент с изображением")) {
                    String symbol = getChemicalSymbolFromName(description);
                    if (symbol != null) {
                        ChemicalData.ChemicalElement element = ChemicalData.getElement(symbol);
                        if (element != null) {
                            // Показываем диалог с задержкой, чтобы сначала загрузилась картинка
                            IvResultImage.postDelayed(() -> showElementDialog(element), 500);
                        }
                    }
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
        // Добавляем обработчик долгого нажатия на текущий результат
        LinearLayout resultContainer = findViewById(R.id.current_result_container);
        if (resultContainer != null) {
            resultContainer.setOnLongClickListener(v -> {
                if (!historyList.isEmpty()) {
                    // Показываем диалог с последним отсканированным элементом
                    showDescriptionDialog(historyList.get(0));
                }
                return true;
            });
        }
    }

    private void showElementDialog(ChemicalData.ChemicalElement element) {
        try {
            Log.d("QR_DEBUG", "=== showElementDialog вызван ===");
            Log.d("QR_DEBUG", "Элемент: " + element.getName());

            // Создаем диалог
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Загружаем layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_discription, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            // Находим элементы диалога
            TextView titleView = dialogView.findViewById(R.id.dialog_title);
            TextView typeView = dialogView.findViewById(R.id.dialog_type);
            TextView timeView = dialogView.findViewById(R.id.dialog_time);
            TextView descriptionView = dialogView.findViewById(R.id.dialog_description);
            TextView linkView = dialogView.findViewById(R.id.dialog_link);
            Button closeButton = dialogView.findViewById(R.id.dialog_close);

            // Проверяем, что все элементы найдены
            Log.d("QR_DEBUG", "titleView: " + (titleView != null ? "найден" : "null"));
            Log.d("QR_DEBUG", "typeView: " + (typeView != null ? "найден" : "null"));
            Log.d("QR_DEBUG", "timeView: " + (timeView != null ? "найден" : "null"));
            Log.d("QR_DEBUG", "descriptionView: " + (descriptionView != null ? "найден" : "null"));
            Log.d("QR_DEBUG", "closeButton: " + (closeButton != null ? "найден" : "null"));

            if (titleView == null || typeView == null || timeView == null ||
                    descriptionView == null || closeButton == null) {
                Log.e("QR_DEBUG", "Один из элементов диалога не найден!");
                Toast.makeText(this, "Ошибка: не найден элемент диалога", Toast.LENGTH_SHORT).show();
                return;
            }

            // Формируем информацию
            String title = "Химический элемент: " + element.getName();
            String typeInfo = "Символ: " + element.getSymbol() + " | Атомный номер: " + element.getAtomicNumber();
            String groupInfo = "Группа: " + element.getGroup();

            String fullDescription = "ОПИСАНИЕ:\n" + element.getDescription() + "\n\n═════════════════\n" +
                    "ПРИМЕНЕНИЕ:\n" + element.getUses();

            // Устанавливаем данные
            titleView.setText(title);
            typeView.setText(typeInfo);
            timeView.setText(groupInfo);
            descriptionView.setText(fullDescription);
            descriptionView.setTextSize(16);
            descriptionView.setPadding(16, 16, 16, 16);

            // Скрываем ссылку (для химических элементов она не нужна)
            if (linkView != null) {
                linkView.setVisibility(View.GONE);
            }

            // Кнопка закрытия
            closeButton.setOnClickListener(v -> {
                Log.d("QR_DEBUG", "Диалог закрыт");
                dialog.dismiss();
            });

            // Показываем диалог
            dialog.show();
            Log.d("QR_DEBUG", "Диалог показан");

        } catch (Exception e) {
            Log.e("QR_DEBUG", "Ошибка в showElementDialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при открытии диалога: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private boolean loadLocalImage(String imageName){
        try{
            String resourceName = imageName;
            if(resourceName.contains(".")){
                resourceName = resourceName.substring(0, resourceName.lastIndexOf("."));
            }
            int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());

            if (resourceId!=0){
                //если найдена - показать
                IvResultImage.setVisibility(View.VISIBLE);
                IvResultImage.setImageResource(resourceId);
                return true;
            } else{
                Toast.makeText(this, "Изображение не найдено: " + imageName, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private String[] parseLocalImageData(String data){
        String withoutPrefix = data.substring(LOCAL_PREFIX.length());
        if (withoutPrefix.contains("|")){
            String[] parts = withoutPrefix.split("\\|", 2);
            return new String[] { parts[0].trim(), parts[1].trim() };
        } else {
            return new String[] { withoutPrefix.trim(), null };
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

            //typeView.setText(item.getDataType());
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

            // Добавляем обработчик ДОЛГОГО нажатия - показать описание
            historyItemView.setOnLongClickListener(v -> {
                ScanHistoryItem item2 = historyList.get(position);
                showDescriptionDialog(item2);
                return true; // true = событие обработано
            });

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

    private boolean isChemicalElement(String text) {
        // Словарь химических элементов на русском и английском
        String[] chemicalKeywords = {
                "водород", "гелий", "литий", "углерод", "кислород", "железо",
                "золото", "серебро", "медь", "цинк", "свинец", "ртуть", "алюминий",
                "натрий", "калий", "магний", "кальций", "кремний", "фосфор", "сера",
                "хлор", "аргон", "азот", "фтор", "бром", "йод",
                // Английские названия
                "hydrogen", "helium", "lithium", "carbon", "oxygen", "iron",
                "gold", "silver", "copper", "zinc", "lead", "mercury", "aluminum",
                "sodium", "potassium", "magnesium", "calcium", "silicon", "phosphorus", "sulfur",
                "chlorine", "argon", "nitrogen", "fluorine", "bromine", "iodine"
        };

        String lowerText = text.toLowerCase();
        for (String keyword : chemicalKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Метод для получения символа элемента из названия
    private String getChemicalSymbolFromName(String name) {
        java.util.Map<String, String> nameToSymbol = new java.util.HashMap<>();
        nameToSymbol.put("водород", "H");
        nameToSymbol.put("гелий", "He");
        nameToSymbol.put("литий", "Li");
        nameToSymbol.put("углерод", "C");
        nameToSymbol.put("кислород", "O");
        nameToSymbol.put("железо", "Fe");
        nameToSymbol.put("золото", "Au");
        nameToSymbol.put("серебро", "Ag");
        nameToSymbol.put("медь", "Cu");
        nameToSymbol.put("алюминий", "Al");
        nameToSymbol.put("кремний", "Si");
        nameToSymbol.put("фосфор", "P");
        nameToSymbol.put("сера", "S");
        nameToSymbol.put("хлор", "Cl");
        nameToSymbol.put("кальций", "Ca");
        nameToSymbol.put("натрий", "Na");
        nameToSymbol.put("калий", "K");
        nameToSymbol.put("магний", "Mg");

        // Английские
        nameToSymbol.put("hydrogen", "H");
        nameToSymbol.put("helium", "He");
        nameToSymbol.put("lithium", "Li");
        nameToSymbol.put("carbon", "C");
        nameToSymbol.put("oxygen", "O");
        nameToSymbol.put("iron", "Fe");
        nameToSymbol.put("gold", "Au");
        nameToSymbol.put("silver", "Ag");
        nameToSymbol.put("copper", "Cu");
        nameToSymbol.put("aluminum", "Al");

        String lowerName = name.toLowerCase();
        for (java.util.Map.Entry<String, String> entry : nameToSymbol.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }


    // ================ МЕТОДЫ ДЛЯ ОПРЕДЕЛЕНИЯ ТИПОВ ДАННЫХ ================

    private String determineDataType(String data) {
        if (data == null || data.isEmpty()) return "Пусто";

        // Проверяем на формат "URL|Описание"
        if (data.contains("|")) {
            String[] parts = data.split("\\|", 2);
            if (parts.length == 2) {
                String possibleUrl = parts[0].trim();
                String description = parts[1].trim().toLowerCase();

                if (Patterns.WEB_URL.matcher(possibleUrl).matches()) {
                    // ⭐ Проверяем, не химический ли это элемент по описанию
                    if (isChemicalElement(description)) {
                        return "Химический элемент с изображением";
                    }

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
    private void showDescriptionDialog(ScanHistoryItem item) {
        try {
            Log.d("QR_DEBUG", "=== showDescriptionDialog (долгое нажатие) ===");
            Log.d("QR_DEBUG", "Тип: " + item.getDataType());
            Log.d("QR_DEBUG", "RawData: " + item.getRawData());

            // Получаем сырые данные
            String rawData = item.getRawData();

            // Проверяем, есть ли формат "URL|Описание"
            if (rawData != null && rawData.contains("|")) {
                String[] parts = rawData.split("\\|", 2);
                String description = parts[1].trim();

                Log.d("QR_DEBUG", "Описание из QR-кода: " + description);

                // Проверяем, является ли описание химическим элементом
                if (isChemicalElement(description)) {
                    Log.d("QR_DEBUG", "Это химический элемент!");
                    String symbol = getChemicalSymbolFromName(description);

                    if (symbol != null) {
                        ChemicalData.ChemicalElement element = ChemicalData.getElement(symbol);
                        if (element != null) {
                            Log.d("QR_DEBUG", "Найден элемент: " + element.getName());
                            // Открываем диалог с полной информацией о химическом элементе
                            showElementDialog(element);
                            return;
                        }
                    }
                } else {
                    // Не химический элемент - показываем обычный диалог с описанием
                    Log.d("QR_DEBUG", "Не химический элемент, показываем обычный диалог");
                    showSimpleDescriptionDialog(description, item.getDataType(), item.getTimestamp());
                    return;
                }
            }

            // Если не подошло под формат "URL|Описание"
            Log.d("QR_DEBUG", "Показываем обычный диалог с displayText");
            showSimpleDescriptionDialog(item.getDisplayText(), item.getDataType(), item.getTimestamp());

        } catch (Exception e) {
            Log.e("QR_DEBUG", "Ошибка в showDescriptionDialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при открытии диалога", Toast.LENGTH_SHORT).show();
        }
    }
    private void showLastScanDescription() {
            Log.d("QR_DEBUG", "=== showLastScanDescription ВЫЗВАН ===");

            if (historyList.isEmpty()) {
                Log.d("QR_DEBUG", "История пуста");
                Toast.makeText(this, "Нет отсканированных данных", Toast.LENGTH_SHORT).show();
                return;
            }

            ScanHistoryItem lastItem = historyList.get(0);
            String rawData = lastItem.getRawData();
            String dataType = lastItem.getDataType();

            Log.d("QR_DEBUG", "Последний элемент: тип=" + dataType);
            Log.d("QR_DEBUG", "Сырые данные: " + rawData);

            // Проверяем формат "URL|Описание"
            if (rawData != null && rawData.contains("|")) {
                String[] parts = rawData.split("\\|", 2);
                String description = parts[1].trim();

                Log.d("QR_DEBUG", "Описание из QR-кода: " + description);

                // Проверяем, является ли описание химическим элементом
                if (isChemicalElement(description)) {
                    Log.d("QR_DEBUG", "Это химический элемент!");
                    String symbol = getChemicalSymbolFromName(description);
                    Log.d("QR_DEBUG", "Символ: " + symbol);

                    if (symbol != null) {
                        ChemicalData.ChemicalElement element = ChemicalData.getElement(symbol);
                        if (element != null) {
                            Log.d("QR_DEBUG", "Найден элемент: " + element.getName());
                            showElementDialog(element);
                            return;
                        } else {
                            Log.e("QR_DEBUG", "Элемент не найден для символа: " + symbol);
                        }
                    }
                } else {
                    Log.d("QR_DEBUG", "Не химический элемент, показываем обычный диалог");
                    showSimpleDescriptionDialog(description, dataType, lastItem.getTimestamp());
                    return;
                }
            }

            // Если не подошло ни под один вариант
            Log.d("QR_DEBUG", "Не удалось определить описание");
            Toast.makeText(this, "Нет описания для этого элемента", Toast.LENGTH_SHORT).show();
        }
    private void showSimpleDescriptionDialog(String text, String type, String time) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_discription, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            TextView titleView = dialogView.findViewById(R.id.dialog_title);
            TextView typeView = dialogView.findViewById(R.id.dialog_type);
            TextView timeView = dialogView.findViewById(R.id.dialog_time);
            TextView descriptionView = dialogView.findViewById(R.id.dialog_description);
            Button closeButton = dialogView.findViewById(R.id.dialog_close);

            titleView.setText("Описание");
            typeView.setText("Тип: " + type);
            timeView.setText("Время: " + time);
            descriptionView.setText(text);

            closeButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();

            Log.d("QR_DEBUG", "Обычный диалог показан");

        } catch (Exception e) {
            Log.e("QR_DEBUG", "Ошибка: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при открытии диалога", Toast.LENGTH_SHORT).show();
        }
    }
}