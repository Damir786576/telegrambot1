package com.example.telegram_bot.report_pet;

import com.example.telegram_bot.service.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PetReport {

    private static final Logger log = LoggerFactory.getLogger(PetReport.class);
    private static final List<String> REPORT_COMMANDS = Arrays.asList(
            "Прислать фото животного",
            "Прислать рацион животного",
            "Самочуствие и привыкание к новому месту",
            "Изменения в поведении",
            "Вернуться в главное меню"
    );

    private static final String PHOTO_ANIMAL = "Здесь ты должен прислать фото своего животного";
    private static final String DIET_ANIMAL = "Здесь ты должен прислать фото рациона своего животного";
    private static final String WELLBEING = "Напиши о том, как чувствует себя животное на новом месте";
    private static final String DEMEANOR = "Если у животного есть изменения в поведении, то напиши об этом здесь";

    private final Map<Long, String> userStates = new HashMap<>();

    public String getReportInfo() {
        return "Привет, в этом разделе ты должен ежедневно отправлять отчёты о том, как живёт животное у тебя дома, пока не пройдёт испытательный срок. " +
                "Каждый день необходимо присылать фото животного, его рацион, а также его самочувствие. Если есть изменения в поведении, напиши об этом.";
    }

    public void sendReportMenu(long chatId, TelegramLongPollingBot bot, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text.isEmpty() ? "Выберите действие:" : text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Прислать фото животного");
        row1.add("Прислать рацион животного");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Самочуствие и привыкание к новому месту");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Изменения в поведении");
        keyboardRows.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Вернуться в главное меню");
        keyboardRows.add(row4);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
            log.debug("Отправлено меню отчётов для chatId={}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки меню отчётов для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public boolean isReportCommand(String command) {
        return REPORT_COMMANDS.contains(command);
    }

    public void handleReportMenuCommand(String command, long chatId, TelegramLongPollingBot bot) {
        if (userStates.containsKey(chatId) && isReportCommand(command)) {
            userStates.remove(chatId);
        }

        switch (command) {
            case "Прислать фото животного":
                userStates.put(chatId, "AWAITING_PHOTO");
                sendResponseWithBackButton(chatId, PHOTO_ANIMAL, bot);
                break;
            case "Прислать рацион животного":
                userStates.put(chatId, "AWAITING_DIET");
                sendResponseWithBackButton(chatId, DIET_ANIMAL, bot);
                break;
            case "Самочуствие и привыкание к новому месту":
                userStates.put(chatId, "AWAITING_WELLBEING");
                sendResponseWithBackButton(chatId, WELLBEING, bot);
                break;
            case "Изменения в поведении":
                userStates.put(chatId, "AWAITING_DEMEANOR");
                sendResponseWithBackButton(chatId, DEMEANOR, bot);
                break;
            case "Вернуться в главное меню":
                userStates.remove(chatId);
                ((TelegramBot) bot).sendMainMenu(chatId, "");
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда. Выберите действие из меню.", bot);
        }
    }

    public void handleUserInput(long chatId, String input, boolean hasPhoto, TelegramLongPollingBot bot) {
        String state = userStates.get(chatId);
        if (state == null) {
            return;
        }

        String response;
        switch (state) {
            case "AWAITING_PHOTO":
            case "AWAITING_DIET":
                if (hasPhoto) {
                    response = state.equals("AWAITING_PHOTO") ?
                            "Фото животного получено. Спасибо! Выберите следующее действие." :
                            "Фото рациона получено. Спасибо! Выберите следующее действие.";
                    userStates.remove(chatId);
                    sendReportMenu(chatId, bot, response);
                } else {
                    response = "Пожалуйста, пришлите фото.";
                    sendResponseWithBackButton(chatId, response, bot);
                }
                break;
            case "AWAITING_WELLBEING":
                response = "Информация о самочувствии сохранена. Спасибо! Выберите следующее действие.";
                userStates.remove(chatId);
                sendReportMenu(chatId, bot, response);
                break;
            case "AWAITING_DEMEANOR":
                response = "Информация о поведении сохранена. Спасибо! Выберите следующее действие.";
                userStates.remove(chatId);
                sendReportMenu(chatId, bot, response);
                break;
            default:
                response = "Произошла ошибка. Попробуйте снова.";
                userStates.remove(chatId);
                sendReportMenu(chatId, bot, response);
        }
    }

    private void sendResponseWithBackButton(long chatId, String text, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в меню отчётов");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
            log.debug("Отправлен ответ с кнопкой возврата для chatId={}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public boolean isAwaitingInput(long chatId) {
        return userStates.containsKey(chatId);
    }

    public void handleReturnToReportMenu(long chatId, TelegramLongPollingBot bot) {
        userStates.remove(chatId);
        sendReportMenu(chatId, bot, "Выберите действие:");
    }
}