package com.example.coralbot;

import com.example.coralbot.Content.Step;
import com.example.coralbot.Content.StepType;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CoralClubBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final Database database;
    private final List<Long> adminIds;
    private final String mediaBasePath;

    // Тексты кнопок (с эмодзи)
    private static final String BTN_SOCIAL_MARKETING = "📊 СОЦИАЛЬНЫЙ МАРКЕТИНГ";
    private static final String BTN_IDEAL_MODEL = "🏗 ИДЕАЛЬНАЯ МОДЕЛЬ БИЗНЕСА";
    private static final String BTN_WHY_CORAL = "💎 Почему Coral Club";
    private static final String BTN_AFTER_4_12 = "⏳ ЧЕРЕЗ 4–12 месяцев";
    private static final String BTN_FIN_RESULTS = "💰 РЕЗУЛЬТАТЫ ПАРТНЁРОВ";
    private static final String BTN_FOR_NETWORKERS = "🚀 ДЛЯ СЕТЕВИКОВ";
    private static final String BTN_FAQ = "❓ ЧАСТО ЗАДАВАЕМЫЕ ВОПРОСЫ";
    private static final String BTN_HOW_TO_START = "🧭 С ЧЕГО НАЧАТЬ?";
    private static final String BTN_REGISTRATION = "📝 РЕГИСТРАЦИЯ (VIP карта)";
    private static final String BTN_TG_CHANNEL = "📢 Мой Telegram канал";
    private static final String BTN_CONTACT = "☎️ Связаться со мной";

    private static final String BTN_ADMIN_PANEL = "🛠 Админ-панель";

    private static final String CALLBACK_BACK_TO_MENU = "BACK_TO_MENU";

    // callback-ключи для всех инлайн-кнопок меню
    private static final String CB_SOCIAL_MARKETING = "CB_SOCIAL_MARKETING";
    private static final String CB_IDEAL_MODEL = "CB_IDEAL_MODEL";
    private static final String CB_WHY_CORAL = "CB_WHY_CORAL";
    private static final String CB_AFTER_4_12 = "CB_AFTER_4_12";
    private static final String CB_FIN_RESULTS = "CB_FIN_RESULTS";
    private static final String CB_FOR_NETWORKERS = "CB_FOR_NETWORKERS";
    private static final String CB_FAQ = "CB_FAQ";
    private static final String CB_HOW_TO_START = "CB_HOW_TO_START";
    private static final String CB_REGISTRATION = "CB_REGISTRATION";
    private static final String CB_TG_CHANNEL = "CB_TG_CHANNEL";
    private static final String CB_CONTACT = "CB_CONTACT";
    private static final String CB_ADMIN_PANEL = "CB_ADMIN_PANEL";

    public CoralClubBot(String botToken,
                        String botUsername,
                        Database database,
                        List<Long> adminIds,
                        String mediaBasePath) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.database = database;
        this.adminIds = adminIds;
        this.mediaBasePath = mediaBasePath;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private boolean isAdmin(long chatId) {
        return adminIds.contains(chatId);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStartMenu(long chatId) throws TelegramApiException {
        // 1) Стартовое видео с кэшированием через mediaKey
        Step startVideoStep = new Step(
                StepType.VIDEO,
                "start_video_1",   // ключ для кэша
                "video/1.MP4",     // путь к файлу относительно MEDIA_BASE_PATH
                null               // без подписи
        );
        // false = это не последнее сообщение сценария, не добавляем "Вернуться в меню"
        sendVideo(chatId, startVideoStep, false);

        // 2) Приветственный текст + ГЛАВНОЕ МЕНЮ (инлайн-кнопки)
        SendMessage intro = new SendMessage();
        intro.setChatId(Long.toString(chatId));
        intro.setText(Content.START_INTRO_TEXT);  // твой большой текст
        intro.setParseMode(ParseMode.HTML);
        intro.setReplyMarkup(mainMenuInlineKeyboard(chatId));
        execute(intro);
    }

    private void handleCallback(CallbackQuery callbackQuery) throws TelegramApiException, InterruptedException {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        switch (data) {
            case CALLBACK_BACK_TO_MENU -> sendStartMenu(chatId);

            case CB_SOCIAL_MARKETING -> runScenario(chatId, Content.socialMarketingScenario());
            case CB_IDEAL_MODEL -> runScenario(chatId, Content.idealBusinessModelScenario());
            case CB_WHY_CORAL -> runScenario(chatId, Content.whyCoralClubScenario());
            case CB_AFTER_4_12 -> runScenario(chatId, Content.after4to12MonthsScenario());
            case CB_FIN_RESULTS -> runScenario(chatId, Content.partnersFinancialResultsScenario());
            case CB_FOR_NETWORKERS -> runScenario(chatId, Content.forNetworkersScenario());
            case CB_FAQ -> runScenario(chatId, Content.faqScenario());
            case CB_HOW_TO_START -> runScenario(chatId, Content.howToStartScenario());
            case CB_REGISTRATION -> runScenario(chatId, Content.registrationScenario());
            case CB_TG_CHANNEL -> runScenario(chatId, Content.telegramChannelScenario());
            case CB_CONTACT -> runScenario(chatId, Content.contactMeScenario());

            case CB_ADMIN_PANEL -> handleAdminPanel(chatId);
        }
    }

    private void handleMessage(Message message) throws TelegramApiException, InterruptedException {
        if (!message.hasText()) {
            return;
        }

        long chatId = message.getChatId();
        String text = message.getText().trim();

        // регистрируем пользователя (если новый)
        database.insertUserIfNotExists(
                chatId,
                message.getFrom() != null ? message.getFrom().getUserName() : null,
                message.getFrom() != null ? message.getFrom().getFirstName() : null,
                message.getFrom() != null ? message.getFrom().getLastName() : null
        );

        if (text.startsWith("/start")) {
            handleStart(chatId, message);
            return;
        }

        if (text.startsWith("/all")) {
            handleAll(chatId);
            return;
        }

        if (text.startsWith("/send")) {
            handleSend(chatId, text);
            return;
        }

        // на любой другой текст просто показываем меню
        sendMainMenu(chatId, "Главное меню ⬇️");
    }

    private void handleStart(long chatId, Message message) throws TelegramApiException {
        // Регистрируем пользователя, если его ещё нет
        database.insertUserIfNotExists(
                chatId,
                message.getFrom() != null ? message.getFrom().getUserName() : null,
                message.getFrom() != null ? message.getFrom().getFirstName() : null,
                message.getFrom() != null ? message.getFrom().getLastName() : null
        );

        // Всегда показываем стартовый экран (видео + текст + меню)
        sendStartMenu(chatId);
    }

    private void runScenario(long chatId, List<Step> steps, boolean withDelay) throws TelegramApiException, InterruptedException {
        if (steps == null || steps.isEmpty()) {
            sendSimpleText(chatId, "Контент пока не настроен 🙂");
            return;
        }

        boolean first = true;
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            boolean last = (i == steps.size() - 1);

            if (!first && withDelay) {
                Thread.sleep(3000); // 3 секунды между сообщениями сценария
            }
            first = false;

            switch (step.type()) {
                case TEXT -> sendTextInChunks(chatId, step.text(), last, true);
                case PHOTO -> sendPhoto(chatId, step, last);
                case VIDEO -> sendVideo(chatId, step, last);
                case AUDIO -> sendAudio(chatId, step, last);
            }
        }
    }

    // -------------------- Админка --------------------

    private void handleAdminPanel(long chatId) throws TelegramApiException {
        if (!isAdmin(chatId)) {
            sendSimpleText(chatId, "❌ У вас нет доступа к админ-панели.");
            return;
        }

        String text = """
🛠 <b>Админ-панель</b>

Доступные команды:
/send Текст рассылки — мгновенная рассылка всем пользователям
/all — количество зарегистрированных пользователей
""";
        sendSimpleText(chatId, text);
    }

    private void handleAll(long chatId) throws TelegramApiException {
        if (!isAdmin(chatId)) {
            sendSimpleText(chatId, "❌ Команда доступна только админам.");
            return;
        }
        long count = database.countUsers();
        sendSimpleText(chatId, "👥 Всего пользователей: <b>" + count + "</b>");
    }

    private void handleSend(long chatId, String fullText) throws TelegramApiException {
        if (!isAdmin(chatId)) {
            sendSimpleText(chatId, "❌ Команда доступна только админам.");
            return;
        }

        String[] parts = fullText.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendSimpleText(chatId, "Использование: <code>/send текст рассылки</code>");
            return;
        }

        String broadcast = parts[1].trim();
        List<Long> allChats = database.getAllChatIds();
        int ok = 0;
        int fail = 0;

        for (Long uid : allChats) {
            try {
                sendTextInChunks(uid, broadcast, false, false);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }

        sendSimpleText(chatId, "✅ Отправлено: " + ok + "\n⚠️ Ошибок: " + fail);
    }

    // -------------------- Основная логика сценариев --------------------

    /**
     * Запустить сценарий: каждое сообщение (кроме первого) — через 3 секунды.
     * У последнего сообщения — кнопка "Вернуться в меню".
     */
    private void runScenario(long chatId, List<Step> steps) throws TelegramApiException, InterruptedException {
        if (steps == null || steps.isEmpty()) {
            sendSimpleText(chatId, "Контент пока не настроен 🙂");
            return;
        }

        boolean first = true;
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            boolean last = (i == steps.size() - 1);

            if (!first) {
                Thread.sleep(3000); // 3 секунды между сообщениями сценария
            }
            first = false;

            switch (step.type()) {
                case TEXT -> sendTextInChunks(chatId, step.text(), last, true);
                case PHOTO -> sendPhoto(chatId, step, last);
                case VIDEO -> sendVideo(chatId, step, last);
                case AUDIO -> sendAudio(chatId, step, last);
            }
        }
    }

    // -------------------- Отправка текстов (с разбиением) --------------------

    /**
     * Разбиваем длинный текст на части (до ~3900 символов), чтобы не упереться в лимит Telegram (4096).
     * Если last == true — у последней части добавляем кнопку "Вернуться в меню".
     * addBackButton == true — прикреплять ли inline-кнопку к последней части.
     */
    private void sendTextInChunks(long chatId, String text, boolean last, boolean addBackButton) throws TelegramApiException {
        if (text == null || text.isBlank()) {
            return;
        }

        List<String> chunks = splitText(text, 3900);

        for (int i = 0; i < chunks.size(); i++) {
            String part = chunks.get(i);
            boolean isLastChunk = (i == chunks.size() - 1);

            SendMessage msg = new SendMessage();
            msg.setChatId(Long.toString(chatId));
            msg.setText(part);
            msg.setParseMode(ParseMode.HTML);

            if (last && isLastChunk && addBackButton) {
                msg.setReplyMarkup(backToMenuInlineKeyboard());
            }

            execute(msg);
        }
    }

    private List<String> splitText(String text, int maxLen) {
        List<String> parts = new ArrayList<>();
        String remaining = text;

        while (remaining.length() > maxLen) {
            int splitAt = remaining.lastIndexOf("\n", maxLen);
            if (splitAt <= 0) {
                splitAt = maxLen;
            }
            parts.add(remaining.substring(0, splitAt));
            remaining = remaining.substring(splitAt).trim();
        }

        if (!remaining.isBlank()) {
            parts.add(remaining);
        }

        return parts;
    }

    private void sendSimpleText(long chatId, String text) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(Long.toString(chatId));
        msg.setText(text);
        msg.setParseMode(ParseMode.HTML);
        execute(msg);
    }

    // -------------------- Отправка с кэшированием медиа --------------------

    private void sendPhoto(long chatId, Step step, boolean last) throws TelegramApiException {
        if (step.relativePath() == null) return;

        SendPhoto request = new SendPhoto();
        request.setChatId(Long.toString(chatId));
        request.setParseMode(ParseMode.HTML);

        String fileId = step.mediaKey() != null ? database.getMediaFileId(step.mediaKey()) : null;

        if (fileId != null) {
            request.setPhoto(new InputFile(fileId));
        } else {
            File file = new File(mediaBasePath, step.relativePath());
            request.setPhoto(new InputFile(file));
        }

        if (step.text() != null && !step.text().isBlank()) {
            request.setCaption(step.text());
        }

        if (last) {
            request.setReplyMarkup(backToMenuInlineKeyboard());
        }

        Message response = execute(request);

        if (fileId == null && step.mediaKey() != null && response != null
                && response.getPhoto() != null && !response.getPhoto().isEmpty()) {
            String newFileId = response.getPhoto()
                    .get(response.getPhoto().size() - 1)
                    .getFileId();
            database.saveMediaFileId(step.mediaKey(), "PHOTO", newFileId);
        }
    }

    private void sendVideo(long chatId, Step step, boolean last) throws TelegramApiException {
        if (step.relativePath() == null) return;

        SendVideo request = new SendVideo();
        request.setChatId(Long.toString(chatId));
        request.setParseMode(ParseMode.HTML);

        String fileId = step.mediaKey() != null ? database.getMediaFileId(step.mediaKey()) : null;

        if (fileId != null) {
            request.setVideo(new InputFile(fileId));
        } else {
            File file = new File(mediaBasePath, step.relativePath());
            request.setVideo(new InputFile(file));
        }

        if (step.text() != null && !step.text().isBlank()) {
            request.setCaption(step.text());
        }

        if (last) {
            request.setReplyMarkup(backToMenuInlineKeyboard());
        }

        Message response = execute(request);

        if (fileId == null && step.mediaKey() != null && response != null && response.getVideo() != null) {
            String newFileId = response.getVideo().getFileId();
            database.saveMediaFileId(step.mediaKey(), "VIDEO", newFileId);
        }
    }

    private void sendAudio(long chatId, Step step, boolean last) throws TelegramApiException {
        if (step.relativePath() == null) return;

        SendAudio request = new SendAudio();
        request.setChatId(Long.toString(chatId));
        request.setParseMode(ParseMode.HTML);

        String fileId = step.mediaKey() != null ? database.getMediaFileId(step.mediaKey()) : null;

        if (fileId != null) {
            request.setAudio(new InputFile(fileId));
        } else {
            File file = new File(mediaBasePath, step.relativePath());
            request.setAudio(new InputFile(file));
        }

        if (step.text() != null && !step.text().isBlank()) {
            request.setCaption(step.text());
        }

        if (last) {
            request.setReplyMarkup(backToMenuInlineKeyboard());
        }

        Message response = execute(request);

        if (fileId == null && step.mediaKey() != null && response != null && response.getAudio() != null) {
            String newFileId = response.getAudio().getFileId();
            database.saveMediaFileId(step.mediaKey(), "AUDIO", newFileId);
        }
    }

    // -------------------- Клавиатуры (ИНЛАЙН, 1 колонка) --------------------

    private InlineKeyboardButton inlineButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }

    private InlineKeyboardMarkup mainMenuInlineKeyboard(long chatId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(BTN_SOCIAL_MARKETING, CB_SOCIAL_MARKETING)));
        rows.add(List.of(inlineButton(BTN_IDEAL_MODEL, CB_IDEAL_MODEL)));
        rows.add(List.of(inlineButton(BTN_WHY_CORAL, CB_WHY_CORAL)));
        rows.add(List.of(inlineButton(BTN_AFTER_4_12, CB_AFTER_4_12)));
        rows.add(List.of(inlineButton(BTN_FIN_RESULTS, CB_FIN_RESULTS)));
        rows.add(List.of(inlineButton(BTN_FOR_NETWORKERS, CB_FOR_NETWORKERS)));
        rows.add(List.of(inlineButton(BTN_FAQ, CB_FAQ)));
        rows.add(List.of(inlineButton(BTN_HOW_TO_START, CB_HOW_TO_START)));
        rows.add(List.of(inlineButton(BTN_REGISTRATION, CB_REGISTRATION)));
        rows.add(List.of(inlineButton(BTN_TG_CHANNEL, CB_TG_CHANNEL)));
        rows.add(List.of(inlineButton(BTN_CONTACT, CB_CONTACT)));

        // если админ — последняя строка: админ-панель
        if (isAdmin(chatId)) {
            rows.add(List.of(inlineButton(BTN_ADMIN_PANEL, CB_ADMIN_PANEL)));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup backToMenuInlineKeyboard() {
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Вернуться в меню");
        backButton.setCallbackData(CALLBACK_BACK_TO_MENU);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(backButton);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private void sendMainMenu(long chatId, String text) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(Long.toString(chatId));
        msg.setText(text);
        msg.setParseMode(ParseMode.HTML);
        msg.setReplyMarkup(mainMenuInlineKeyboard(chatId));
        execute(msg);
    }
}