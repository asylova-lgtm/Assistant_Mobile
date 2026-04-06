package com.example.qrcodeapp;

import java.util.HashMap;
import java.util.Map;

public class ChemicalReactions {

    // Внутренний класс для хранения информации о реакции
    public static class Reaction {
        private String reactants;      // реагенты (что вступает в реакцию)
        private String products;       // продукты (что получается)
        private String conditions;     // условия реакции
        private String equation;       // уравнение реакции
        private String description;    // описание реакции

        public Reaction(String reactants, String products, String conditions,
                        String equation, String description) {
            this.reactants = reactants;
            this.products = products;
            this.conditions = conditions;
            this.equation = equation;
            this.description = description;
        }

        // Геттеры
        public String getReactants() { return reactants; }
        public String getProducts() { return products; }
        public String getConditions() { return conditions; }
        public String getEquation() { return equation; }
        public String getDescription() { return description; }
    }

    // База реакций (ключ: "символ1+символ2")
    private static final Map<String, Reaction> reactions = new HashMap<>();

    static {
        // ========== РЕАКЦИИ С ВОДОРОДОМ ==========
        // Водород + Кислород = Вода
        reactions.put("H+O", new Reaction(
                "Водород + Кислород",
                "Вода (H₂O)",
                "Температура: 500-600°C, воспламенение",
                "2H₂ + O₂ → 2H₂O",
                "Водород сгорает в кислороде с образованием воды. Реакция сопровождается выделением большого количества тепла и света. Смесь водорода с кислородом называется 'гремучим газом' и взрывоопасна."
        ));

        reactions.put("O+H", new Reaction(
                "Кислород + Водород",
                "Вода (H₂O)",
                "Температура: 500-600°C, воспламенение",
                "O₂ + 2H₂ → 2H₂O",
                "Кислород поддерживает горение водорода. При поджигании смеси происходит взрыв с образованием воды."
        ));

        // ========== РЕАКЦИИ С КИСЛОРОДОМ ==========
        // Железо + Кислород = Ржавчина
        reactions.put("Fe+O", new Reaction(
                "Железо + Кислород",
                "Оксид железа (ржавчина) Fe₂O₃",
                "Влажный воздух, вода, комнатная температура",
                "4Fe + 3O₂ → 2Fe₂O₃",
                "Железо окисляется в присутствии кислорода и воды, образуя ржавчину. Процесс называется коррозией."
        ));

        // Углерод + Кислород = Углекислый газ
        reactions.put("C+O", new Reaction(
                "Углерод + Кислород",
                "Углекислый газ (CO₂)",
                "Температура: 600-800°C, горение",
                "C + O₂ → CO₂",
                "Углерод сгорает в кислороде, образуя углекислый газ. Это основная реакция горения угля и древесины."
        ));

        // ========== РЕАКЦИИ С ЗОЛОТОМ ==========
        // Золото + Кислород = Нет реакции
        reactions.put("Au+O", new Reaction(
                "Золото + Кислород",
                "Реакция не происходит",
                "Любые условия",
                "Au + O₂ → нет реакции",
                "Золото — благородный металл. Оно не окисляется на воздухе даже при нагревании. Именно поэтому золото не тускнеет и не ржавеет."
        ));

        // ========== РЕАКЦИИ С ЖЕЛЕЗОМ ==========
        // Железо + Сера = Сульфид железа
        reactions.put("Fe+S", new Reaction(
                "Железо + Сера",
                "Сульфид железа (FeS)",
                "Нагревание до 500-600°C",
                "Fe + S → FeS",
                "При нагревании железо реагирует с серой, образуя сульфид железа. Это черное твердое вещество."
        ));

        // ========== РЕАКЦИИ С УГЛЕРОДОМ ==========
        // Углерод + Водород = Метан (требуется катализатор)
        reactions.put("C+H", new Reaction(
                "Углерод + Водород",
                "Метан (CH₄)",
                "Высокая температура, катализатор",
                "C + 2H₂ → CH₄",
                "В промышленности водород реагирует с углеродом при высоком давлении и температуре в присутствии катализатора, образуя метан."
        ));

        // ========== РЕАКЦИИ С СЕРОЙ ==========
        // Сера + Кислород = Сернистый газ
        reactions.put("S+O", new Reaction(
                "Сера + Кислород",
                "Сернистый газ (SO₂)",
                "Температура: 200-300°C, горение",
                "S + O₂ → SO₂",
                "Сера сгорает в кислороде с образованием сернистого газа. При этом выделяется синий цвет пламени."
        ));

        // ========== РЕАКЦИИ С МЕДЬЮ ==========
        // Медь + Кислород = Оксид меди
        reactions.put("Cu+O", new Reaction(
                "Медь + Кислород",
                "Оксид меди (CuO)",
                "Нагревание до 300-400°C",
                "2Cu + O₂ → 2CuO",
                "Медь при нагревании на воздухе покрывается черным налетом оксида меди. Это используется в ювелирном деле для патинирования."
        ));
    }

    /**
     * Получить реакцию между двумя элементами
     * @param symbol1 символ первого элемента (например, "H")
     * @param symbol2 символ второго элемента (например, "O")
     * @return объект Reaction или null, если реакция не найдена
     */
    public static Reaction getReaction(String symbol1, String symbol2) {
        if (symbol1 == null || symbol2 == null) return null;

        // Проверяем оба порядка (H+O и O+H)
        String key1 = symbol1 + "+" + symbol2;
        String key2 = symbol2 + "+" + symbol1;

        if (reactions.containsKey(key1)) {
            return reactions.get(key1);
        }
        if (reactions.containsKey(key2)) {
            return reactions.get(key2);
        }
        return null;
    }

    /**
     * Проверяет, существует ли реакция между двумя элементами
     */
    public static boolean hasReaction(String symbol1, String symbol2) {
        return getReaction(symbol1, symbol2) != null;
    }
}