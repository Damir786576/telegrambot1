package com.example.telegram_bot.info_for_shelter;

import com.example.telegram_bot.service.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ShelterInfoService {
    private static final Logger log = LoggerFactory.getLogger(ShelterInfoService.class);
    private static final List<String> SHELTER_COMMANDS = Arrays.asList(
            "Адрес и схема проезда", "Расписание работы", "Рассказать о приюте",
            "Контакты охраны", "Правила безопасности", "Оставить контактные данные",
            "Вернуться в главное меню", "Вернуться в меню приюта"
    );

    private String shelterAddress;
    private String shelterHours;
    private String shelterAbout;
    private String shelterSecurity;
    private String shelterSafety;
    private String shelterMap;
    private String shelterVolunteer;

    private final Map<Long, String> userStates = new HashMap<>();
    private final List<String> contacts = new ArrayList<>();

    public String getShelterInfo() {
        return "Привет, в этом разделе ты можешь узнать всё о нашем приюте)))";
    }

    public String getVolunteerContact() {
        return shelterVolunteer;
    }

    public boolean isShelterCommand(String command) {
        return SHELTER_COMMANDS.contains(command);
    }

    public boolean isAwaitingContact(long chatId) {
        return "AWAITING_CONTACT".equals(userStates.get(chatId));
    }

    public void sendShelterMenu(long chatId, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(getShelterInfo());

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Адрес и схема проезда");
        row1.add("Расписание работы");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Рассказать о приюте");
        row2.add("Контакты охраны");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Правила безопасности");
        row3.add("Оставить контактные данные");
        keyboardRows.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Вернуться в главное меню");
        keyboardRows.add(row4);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки меню приюта: " + e.getMessage());
        }
    }

    public void handleShelterMenuCommand(String command, long chatId, TelegramLongPollingBot bot) {
        switch (command) {
            case "Адрес и схема проезда":
                sendResponseWithBackButton(chatId, shelterAddress, bot);
                SendPhoto photo = new SendPhoto();
                photo.setChatId(String.valueOf(chatId));
                photo.setPhoto(new InputFile(shelterMap));
                try {
                    bot.execute(photo);
                } catch (TelegramApiException e) {
                    log.error("Ошибка отправки схемы проезда: " + e.getMessage());
                }
                break;
            case "Расписание работы":
                sendResponseWithBackButton(chatId, shelterHours, bot);
                break;
            case "Рассказать о приюте":
                sendResponseWithBackButton(chatId, shelterAbout, bot);
                break;
            case "Контакты охраны":
                sendResponseWithBackButton(chatId, shelterSecurity, bot);
                break;
            case "Правила безопасности":
                sendResponseWithBackButton(chatId, shelterSafety, bot);
                break;
            case "Оставить контактные данные":
                userStates.put(chatId, "AWAITING_CONTACT");
                sendResponseWithBackButton(chatId, "Введите номер телефона в формате +7-9**-***-****-**", bot);
                break;
            case "Вернуться в главное меню":
                try {
                    ((TelegramBot) bot).sendMainMenu(chatId, "");
                } catch (Exception e) {
                    log.error("Ошибка возврата в главное меню: " + e.getMessage());
                }
                break;
            case "Вернуться в меню приюта":
                sendShelterMenu(chatId, bot);
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда", bot);
        }
    }

    public void handleContactInput(String input, long chatId, TelegramLongPollingBot bot) {
        Pattern pattern = Pattern.compile("\\+7-9\\d{2}-\\d{3}-\\d{4}-\\d{2}");
        if (pattern.matcher(input).matches()) {
            contacts.add(input);
            userStates.remove(chatId);
            sendResponseWithBackButton(chatId, "Контакты сохранены: " + input, bot);
        } else {
            sendResponseWithBackButton(chatId, "Неверный формат. Введите номер в формате +7-9**-***-****-**", bot);
        }
    }

    private void sendResponseWithBackButton(long chatId, String text, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в меню приюта");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа: " + e.getMessage());
        }
    }
}
