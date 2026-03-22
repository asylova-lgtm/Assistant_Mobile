package com.example.qrcodeapp;

import java.util.HashMap;
import java.util.Map;

public class ChemicalData {

    // База данных химических элементов
    private static final Map<String, ChemicalElement> elements = new HashMap<>();

    static {
        // Заполняем базу
        elements.put("H", new ChemicalElement(
                "Водород",
                "H",
                1,
                "Неметалл",
                "Самый легкий элемент, бесцветный газ без вкуса и запаха.",
                "Используется в производстве аммиака, ракетном топливе."
        ));

        elements.put("He", new ChemicalElement(
                "Гелий",
                "He",
                2,
                "Благородный газ",
                "Инертный газ без цвета и запаха.",
                "Заполнение воздушных шаров, охлаждение в МРТ."
        ));

        elements.put("Li", new ChemicalElement(
                "Литий",
                "Li",
                3,
                "Щелочной металл",
                "Самый легкий из всех металлов.",
                "Производство аккумуляторов."
        ));

        elements.put("C", new ChemicalElement(
                "Углерод",
                "C",
                6,
                "Неметалл",
                "Основа органической жизни.",
                "Топливо, сталь, алмазы."
        ));

        elements.put("O", new ChemicalElement(
                "Кислород",
                "O",
                8,
                "Неметалл",
                "Необходим для дыхания всех живых организмов.",
                "Медицина, металлургия."
        ));

        elements.put("Fe", new ChemicalElement(
                "Железо",
                "Fe",
                26,
                "Переходный металл",
                "Ковкий металл серебристо-белого цвета.",
                "Строительство, машиностроение."
        ));

        elements.put("Au", new ChemicalElement(
                "Золото",
                "Au",
                79,
                "Благородный металл",
                "Желтый драгоценный металл. Не окисляется.",
                "Ювелирные изделия, электроника."
        ));

        elements.put("Ag", new ChemicalElement(
                "Серебро",
                "Ag",
                47,
                "Благородный металл",
                "Обладает лучшей электропроводностью.",
                "Электроника, ювелирные изделия."
        ));

        // Добавь другие элементы по необходимости
    }

    // Получить элемент по символу
    public static ChemicalElement getElement(String symbol) {
        if (symbol == null) return null;
        return elements.get(symbol);
    }

    // Проверить, существует ли элемент
    public static boolean exists(String symbol) {
        if (symbol == null) return false;
        return elements.containsKey(symbol);
    }

    // Внутренний класс для хранения информации об элементе
    public static class ChemicalElement {
        private String name;
        private String symbol;
        private int atomicNumber;
        private String group;
        private String description;
        private String uses;

        public ChemicalElement(String name, String symbol, int atomicNumber,
                               String group, String description, String uses) {
            this.name = name;
            this.symbol = symbol;
            this.atomicNumber = atomicNumber;
            this.group = group;
            this.description = description;
            this.uses = uses;
        }

        // Геттеры
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public int getAtomicNumber() { return atomicNumber; }
        public String getGroup() { return group; }
        public String getDescription() { return description; }
        public String getUses() { return uses; }
    }
}