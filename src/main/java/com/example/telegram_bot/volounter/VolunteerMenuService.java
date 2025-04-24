package com.example.telegram_bot.volounter;

import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.service.AnimalService;
import com.example.telegram_bot.service.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
public class VolunteerMenuService {
    private static final Logger log = LoggerFactory.getLogger(VolunteerMenuService.class);
    private static final List<String> VOLUNTEER_COMMANDS = Arrays.asList(
            "Что должен делать волонтёр", "Добавить животное в приют", "Вернуться в главное меню", "Вернуться в меню волонтёра"
    );
    private static final String VOLUNTEER_INSTRUCTIONS = "Инструкции:\n" +
            "Нужно каждый день после 21:00 смотреть состояние животного, которого забрал человек.\n" +
            "Если в приюте появляется новое животное, в таком случае надо добавить его в список животных.";
    private static final String ADD_ANIMAL_PROMPT = "Введите имя животного:";

    private static final String AWAITING_ANIMAL_NAME = "AWAITING_ANIMAL_NAME";
    private static final String AWAITING_ANIMAL_TYPE = "AWAITING_ANIMAL_TYPE";
    private static final String AWAITING_ANIMAL_AGE = "AWAITING_ANIMAL_AGE";

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, AnimalData> pendingAnimals = new HashMap<>();
    private final AnimalService animalService;

    public VolunteerMenuService(AnimalService animalService) {
        this.animalService = animalService;
    }

    public boolean isVolunteerCommand(String command) {
        return command != null && VOLUNTEER_COMMANDS.contains(command);
    }

    public boolean isAwaitingInput(long chatId) {
        return userStates.containsKey(chatId);
    }

    public void sendVolunteerMenu(long chatId, TelegramBot bot, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text.isEmpty() ? "Меню волонтёра:" : text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Что должен делать волонтёр");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Добавить животное в приют");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Вернуться в главное меню");
        keyboardRows.add(row3);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки меню волонтёра для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public void handleVolunteerCommand(String command, long chatId, TelegramBot bot) {
        switch (command) {
            case "Что должен делать волонтёр":
                sendResponseWithBackButton(chatId, VOLUNTEER_INSTRUCTIONS, bot);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                break;
            case "Добавить животное в приют":
                userStates.put(chatId, AWAITING_ANIMAL_NAME);
                pendingAnimals.put(chatId, new AnimalData());
                sendResponseWithBackButton(chatId, ADD_ANIMAL_PROMPT, bot);
                break;
            case "Вернуться в главное меню":
                userStates.remove(chatId);
                pendingAnimals.remove(chatId);
                bot.sendMainMenu(chatId, "Выберите действие:");
                break;
            case "Вернуться в меню волонтёра":
                userStates.remove(chatId);
                pendingAnimals.remove(chatId);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда", bot);
        }
    }

    public void handleAnimalInput(String input, long chatId, TelegramBot bot) {
        String state = userStates.get(chatId);
        AnimalData animalData = pendingAnimals.get(chatId);

        if (state == null || animalData == null) {
            log.error("Состояние или данные животного отсутствуют для chatId={}", chatId);
            userStates.remove(chatId);
            pendingAnimals.remove(chatId);
            sendResponseWithBackButton(chatId, "Ошибка. Пожалуйста, начните добавление заново.", bot);
            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
            return;
        }

        if ("Вернуться в меню волонтёра".equals(input)) {
            userStates.remove(chatId);
            pendingAnimals.remove(chatId);
            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
            return;
        }

        switch (state) {
            case AWAITING_ANIMAL_NAME:
                if (input == null || input.trim().isEmpty()) {
                    sendResponseWithBackButton(chatId, "Имя не может быть пустым. Введите имя животного:", bot);
                } else {
                    animalData.setName(input.trim());
                    userStates.put(chatId, AWAITING_ANIMAL_TYPE);
                    sendResponseWithBackButton(chatId, "Введите вид животного (кошка/собака):", bot);
                }
                break;
            case AWAITING_ANIMAL_TYPE:
                if ("кошка".equalsIgnoreCase(input) || "собака".equalsIgnoreCase(input)) {
                    animalData.setType(input.toLowerCase());
                    userStates.put(chatId, AWAITING_ANIMAL_AGE);
                    sendResponseWithBackButton(chatId, "Введите возраст животного (в годах, например, 2):", bot);
                } else {
                    sendResponseWithBackButton(chatId, "Пожалуйста, укажите 'кошка' или 'собака'.", bot);
                }
                break;
            case AWAITING_ANIMAL_AGE:
                try {
                    int age = Integer.parseInt(input);
                    if (age >= 0 && age <= 30) {
                        animalData.setAge(age);
                        AnimalEntity animal = new AnimalEntity();
                        animal.setName(animalData.getName());
                        animal.setType(animalData.getType());
                        animal.setAge(animalData.getAge());
                        log.debug("Создано животное: name={}, type={}, age={}",
                                animal.getName(), animal.getType(), animal.getAge());
                        try {
                            animalService.save(animal);
                            userStates.remove(chatId);
                            pendingAnimals.remove(chatId);
                            String response = String.format("Животное добавлено: %s, %s, %d года.",
                                    animal.getName(), animal.getType(), animal.getAge());
                            sendResponseWithBackButton(chatId, response, bot);
                            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        } catch (Exception e) {
                            sendResponseWithBackButton(chatId, "Ошибка при сохранении животного. Попробуйте снова.", bot);
                            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        }
                    } else {
                        sendResponseWithBackButton(chatId, "Возраст должен быть от 0 до 30 лет.", bot);
                    }
                } catch (NumberFormatException e) {
                    sendResponseWithBackButton(chatId, "Пожалуйста, введите число (например, 2).", bot);
                }
                break;
            default:
                log.error("Неизвестное состояние '{}' для chatId={}", state, chatId);
                userStates.remove(chatId);
                pendingAnimals.remove(chatId);
                sendResponseWithBackButton(chatId, "Ошибка. Пожалуйста, начните добавление заново.", bot);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
        }
    }

    public void sendResponseWithBackButton(long chatId, String text, TelegramBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в меню волонтёра");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    private static class AnimalData {
        private String name;
        private String type;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
