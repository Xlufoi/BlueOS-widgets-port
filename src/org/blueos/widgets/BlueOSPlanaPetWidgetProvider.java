package org.blueos.widgets;

public class BlueOSPlanaPetWidgetProvider extends BlueOSAronaPetWidgetProvider {

    private static final String[] PLANA_GREETINGS_RU = {
            "Сэнсэй, отчёты проверены. Жду ваших указаний.",
            "Системы Shittim Chest в штатном режиме.",
            "Сэнсэй, не перегружайте себя работой.",
            "Арона спит... Я присмотрю за вами, Сэнсэй.",
            "С возвращением, Сэнсэй. Все данные сохранены.",
            "Готова к выполнению тактических задач.",
    };

    private static final String[] PLANA_GREETINGS_EN = {
            "Sensei, reports verified. Awaiting your instructions.",
            "Shittim Chest systems operating normally.",
            "Sensei, please do not overwork yourself.",
            "Arona is asleep... I will look after you, Sensei.",
            "Welcome back, Sensei. All data is backed up.",
            "Ready for tactical mission support.",
    };

    @Override
    protected int getLayoutId() {
        return R.layout.blueos_plana_widget;
    }

    @Override
    protected int getStickerRes() {
        return R.drawable.blueos_plana_sticker;
    }

    @Override
    protected String getCharacterName(boolean isRu) {
        return isRu ? "Плана · Schale OS" : "Plana · Schale OS";
    }

    @Override
    protected String getNightLine(boolean isRu) {
        return isRu ? "Сэнсэй, глубокая ночь. Рекомендую начать фазу отдыха."
                    : "Sensei, it is late at night. I recommend entering rest phase.";
    }

    @Override
    protected String[] getGreetings(boolean isRu) {
        return isRu ? PLANA_GREETINGS_RU : PLANA_GREETINGS_EN;
    }
}
