package com.example.telegram_bot.volounter;

import com.example.telegram_bot.jpa.*;
import com.example.telegram_bot.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VolunteerMenuService {
    private static final Logger log = LoggerFactory.getLogger(VolunteerMenuService.class);
    private static final List<String> VOLUNTEER_COMMANDS = Arrays.asList(
            "Что должен делать волонтёр", "Добавить животное в приют", "Все пользователи",
            "Управление испытательным сроком", "Вернуться в главное меню", "Вернуться в меню волонтёра"
    );
    private static final String VOLUNTEER_INSTRUCTIONS = "Инструкции:\n" +
            "Нужно каждый день после 21:00 смотреть состояние животного, которого забрал человек.\n" +
            "Если в приюте появляется новое животное, в таком случае надо добавить его в список животных.";
    private static final String ADD_ANIMAL_PROMPT = "Введите имя животного:";
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private static final String AWAITING_ANIMAL_NAME = "AWAITING_ANIMAL_NAME";
    private static final String AWAITING_ANIMAL_TYPE = "AWAITING_ANIMAL_TYPE";
    private static final String AWAITING_ANIMAL_AGE = "AWAITING_ANIMAL_AGE";
    private static final String AWAITING_TRIAL_ADOPTION_ID = "AWAITING_TRIAL_ADOPTION_ID";
    private static final String AWAITING_TRIAL_DECISION = "AWAITING_TRIAL_DECISION";

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, AnimalData> pendingAnimals = new HashMap<>();
    private final Map<Long, Long> pendingTrialAdoption = new HashMap<>();
    private final AnimalService animalService;
    private final UserService userService;
    private final AdoptionService adoptionService;
    private final ReportService reportService; // Добавляем ReportService

    public VolunteerMenuService(AnimalService animalService, UserService userService, AdoptionService adoptionService, ReportService reportService) {
        this.animalService = animalService;
        this.userService = userService;
        this.adoptionService = adoptionService;
        this.reportService = reportService;
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
        row3.add("Все пользователи");
        keyboardRows.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Управление испытательным сроком");
        keyboardRows.add(row4);

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Вернуться в главное меню");
        row5.add("Вернуться в меню волонтёра");
        keyboardRows.add(row5);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
            log.info("Отправлено меню волонтёра для chatId={}", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки меню волонтёра для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public void handleVolunteerCommand(String command, long chatId, TelegramBot bot) {
        log.debug("Обработка команды волонтёра: '{}' для chatId={}", command, chatId);
        if (!userService.isAdmin(chatId)) {
            sendResponseWithBackButton(chatId, "Доступ запрещён: только для волонтёров.", bot);
            log.warn("Пользователь chatId={} не имеет прав администратора", chatId);
            return;
        }

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
            case "Все пользователи":
                try {
                    List<UserEntity> users = userService.findAll();
                    if (users.isEmpty()) {
                        sendResponseWithBackButton(chatId, "📋 Пользователи не найдены.", bot);
                        sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        log.info("Список пользователей пуст для chatId={}", chatId);
                        return;
                    }

                    StringBuilder response = new StringBuilder("📋 Список пользователей:\n\n");
                    for (int i = 0; i < users.size(); i++) {
                        UserEntity user = users.get(i);
                        response.append(String.format("%d. Имя: %s\n", i + 1, user.getName() != null ? user.getName() : "Неизвестно"));
                        response.append(String.format("   Chat ID: %d\n", user.getChatId()));
                        response.append(String.format("   Роль: %s\n", user.getRole()));
                        response.append(String.format("   Телефон: %s\n", user.getPhone() != null ? user.getPhone() : "не указан"));
                        List<AdoptionEntity> adoptions = adoptionService.findByUser(user);
                        if (adoptions.isEmpty()) {
                            response.append("   Усыновлённые животные: отсутствуют\n");
                        } else {
                            response.append("   Усыновлённые животные:\n");
                            for (AdoptionEntity adoption : adoptions) {
                                AnimalEntity animal = adoption.getAnimal();
                                response.append(String.format("     - %s %s (ID: %d, дата: %s, статус: %s)\n",
                                        animal.getType(), animal.getName(), animal.getId(),
                                        adoption.getAdoptionDate(), adoption.getTrialStatus()));
                            }
                        }
                        if (i < users.size() - 1) {
                            response.append("\n");
                        }

                        if (response.length() > MAX_MESSAGE_LENGTH) {
                            sendResponseWithBackButton(chatId, response.substring(0, MAX_MESSAGE_LENGTH), bot);
                            response = new StringBuilder(response.substring(MAX_MESSAGE_LENGTH));
                        }
                    }
                    if (!response.isEmpty()) {
                        sendResponseWithBackButton(chatId, response.toString(), bot);
                    }
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                    log.info("Отправлен список пользователей для chatId={}", chatId);
                } catch (Exception e) {
                    log.error("Ошибка при получении списка пользователей для chatId={}: {}", chatId, e.getMessage(), e);
                    sendResponseWithBackButton(chatId, "Ошибка при загрузке списка пользователей. Попробуйте снова.", bot);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                }
                break;
            case "Управление испытательным сроком":
                try {
                    String adoptionsList = adoptionService.getAllAdoptionsString();
                    if (adoptionsList.equals("🐾 Нет усыновлений в системе.")) {
                        sendResponseWithBackButton(chatId, adoptionsList, bot);
                        sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        log.info("Нет усыновлений для chatId={}", chatId);
                        return;
                    }
                    sendResponseWithBackButton(chatId, adoptionsList, bot);
                    userStates.put(chatId, AWAITING_TRIAL_ADOPTION_ID);
                    sendResponseWithBackButton(chatId, "Введите ID усыновления для управления испытательным сроком:", bot);
                    log.info("Запрошено управление испытательным сроком для chatId={}", chatId);
                } catch (Exception e) {
                    log.error("Ошибка при получении списка усыновлений для chatId={}: {}", chatId, e.getMessage(), e);
                    sendResponseWithBackButton(chatId, "Ошибка при загрузке списка усыновлений. Попробуйте снова.", bot);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                }
                break;
            case "Вернуться в главное меню":
                userStates.remove(chatId);
                pendingAnimals.remove(chatId);
                pendingTrialAdoption.remove(chatId);
                bot.sendMainMenu(chatId, "Выберите действие:");
                break;
            case "Вернуться в меню волонтёра":
                userStates.remove(chatId);
                pendingAnimals.remove(chatId);
                pendingTrialAdoption.remove(chatId);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда", bot);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
        }
    }

    @Transactional
    public void handleAnimalInput(String input, long chatId, TelegramBot bot) {
        String state = userStates.get(chatId);
        AnimalData animalData;

        if (state == null) {
            log.error("Состояние отсутствует для chatId={}", chatId);
            sendResponseWithBackButton(chatId, "Ошибка. Пожалуйста, начните заново.", bot);
            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
            return;
        }

        log.debug("Обработка ввода: '{}' в состоянии '{}' для chatId={}", input, state, chatId);

        if ("Вернуться в меню волонтёра".equals(input)) {
            userStates.remove(chatId);
            pendingAnimals.remove(chatId);
            pendingTrialAdoption.remove(chatId);
            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
            return;
        }

        switch (state) {
            case AWAITING_ANIMAL_NAME:
                animalData = pendingAnimals.get(chatId);
                if (animalData == null) {
                    log.error("Данные животного отсутствуют для chatId={}", chatId);
                    userStates.remove(chatId);
                    pendingAnimals.remove(chatId);
                    sendResponseWithBackButton(chatId, "Ошибка. Начните добавление заново.", bot);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                    return;
                }
                if (input == null || input.trim().isEmpty()) {
                    sendResponseWithBackButton(chatId, "Имя не может быть пустым. Введите имя животного:", bot);
                } else {
                    animalData.setName(input.trim());
                    userStates.put(chatId, AWAITING_ANIMAL_TYPE);
                    sendResponseWithBackButton(chatId, "Введите вид животного (кошка/собака):", bot);
                }
                break;
            case AWAITING_ANIMAL_TYPE:
                animalData = pendingAnimals.get(chatId);
                if ("кошка".equalsIgnoreCase(input) || "собака".equalsIgnoreCase(input)) {
                    animalData.setType(input.toLowerCase());
                    userStates.put(chatId, AWAITING_ANIMAL_AGE);
                    sendResponseWithBackButton(chatId, "Введите возраст животного (в годах, например, 2):", bot);
                } else {
                    sendResponseWithBackButton(chatId, "Пожалуйста, укажите 'кошка' или 'собака'.", bot);
                }
                break;
            case AWAITING_ANIMAL_AGE:
                animalData = pendingAnimals.get(chatId);
                try {
                    int age = Integer.parseInt(input);
                    if (age >= 0 && age <= 30) {
                        animalData.setAge(age);
                        AnimalEntity animal = new AnimalEntity();
                        animal.setName(animalData.getName());
                        animal.setType(animalData.getType());
                        animal.setAge(animalData.getAge());
                        try {
                            animalService.save(animal);
                            userStates.remove(chatId);
                            pendingAnimals.remove(chatId);
                            String response = String.format("Животное добавлено: %s, %s, %d года.",
                                    animal.getName(), animal.getType(), animal.getAge());
                            sendResponseWithBackButton(chatId, response, bot);
                            sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        } catch (Exception e) {
                            log.error("Ошибка при сохранении животного для chatId={}: {}", chatId, e.getMessage(), e);
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
            case AWAITING_TRIAL_ADOPTION_ID:
                try {
                    long adoptionId = Long.parseLong(input);
                    AdoptionEntity adoption = adoptionService.findById(adoptionId);
                    if (adoption == null) {
                        sendResponseWithBackButton(chatId, "Усыновление с ID " + adoptionId + " не найдено.", bot);
                        sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                        return;
                    }
                    pendingTrialAdoption.put(chatId, adoptionId);
                    userStates.put(chatId, AWAITING_TRIAL_DECISION);
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("Выберите решение для усыновления ID " + adoptionId + ":");
                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> keyboardRows = new ArrayList<>();
                    KeyboardRow row1 = new KeyboardRow();
                    row1.add("Пройден");
                    row1.add("Продлить на 14 дней");
                    KeyboardRow row2 = new KeyboardRow();
                    row2.add("Продлить на 30 дней");
                    row2.add("Не пройден");
                    KeyboardRow row3 = new KeyboardRow();
                    row3.add("Вернуться в меню волонтёра");
                    keyboardRows.add(row1);
                    keyboardRows.add(row2);
                    keyboardRows.add(row3);
                    keyboardMarkup.setKeyboard(keyboardRows);
                    keyboardMarkup.setResizeKeyboard(true);
                    message.setReplyMarkup(keyboardMarkup);
                    try {
                        bot.execute(message);
                        log.info("Отправлены варианты решения для усыновления ID={} для chatId={}", adoptionId, chatId);
                    } catch (TelegramApiException e) {
                        log.error("Ошибка отправки вариантов решения для chatId={}: {}", chatId, e.getMessage(), e);
                        sendResponseWithBackButton(chatId, "Ошибка отправки вариантов решения. Попробуйте снова.", bot);
                    }
                } catch (NumberFormatException e) {
                    sendResponseWithBackButton(chatId, "Пожалуйста, введите корректный ID усыновления.", bot);
                }
                break;
            case AWAITING_TRIAL_DECISION:
                Long adoptionId = pendingTrialAdoption.get(chatId);
                if (adoptionId == null) {
                    log.error("ID усыновления отсутствует для chatId={}", chatId);
                    sendResponseWithBackButton(chatId, "Ошибка. Начните заново.", bot);
                    userStates.remove(chatId);
                    pendingTrialAdoption.remove(chatId);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                    return;
                }
                AdoptionEntity adoption = adoptionService.findById(adoptionId);
                if (adoption == null) {
                    log.error("Усыновление ID={} не найдено для chatId={}", adoptionId, chatId);
                    sendResponseWithBackButton(chatId, "Усыновление не найдено.", bot);
                    userStates.remove(chatId);
                    pendingTrialAdoption.remove(chatId);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                    return;
                }
                try {
                    switch (input) {
                        case "Пройден":
                            log.debug("Обработка статуса 'Пройден' для усыновления ID={}", adoptionId);
                            adoption.setTrialStatus(TrialStatus.PASSED);
                            adoptionService.save(adoption);
                            log.info("Установлен статус PASSED для усыновления ID={}", adoptionId);
                            AnimalEntity animal = adoption.getAnimal();
                            if (animal != null) {
                                try {
                                    // Удаляем связанные отчёты
                                    reportService.deleteByAdoption(adoption);
                                    log.info("Удалены отчёты для усыновления ID={}", adoptionId);
                                    // Удаляем усыновление
                                    adoptionService.delete(adoption);
                                    log.info("Усыновление ID={} удалено из базы данных", adoptionId);
                                    // Удаляем животное
                                    animalService.delete(animal);
                                    log.info("Животное ID={} удалено из базы данных", animal.getId());
                                } catch (Exception e) {
                                    log.error("Ошибка удаления отчётов, усыновления ID={} или животного ID={}: {}",
                                            adoptionId, animal.getId(), e.getMessage(), e);
                                    throw new RuntimeException("Не удалось удалить отчёты, усыновление или животное", e);
                                }
                            } else {
                                log.error("Животное отсутствует для усыновления ID={}", adoptionId);
                                sendResponseWithBackButton(chatId, "Ошибка: животное не найдено.", bot);
                                break;
                            }
                            sendResponseWithBackButton(chatId, "Испытательный срок пройден. Животное остаётся у хозяина.", bot);
                            UserEntity user = adoption.getUser();
                            if (user != null) {
                                try {
                                    sendResponseWithBackButton(user.getChatId(),
                                            String.format("Поздравляем! Испытательный срок для %s %s пройден. Животное теперь ваше! 🎉",
                                                    animal.getType(), animal.getName()),
                                            bot);
                                    log.info("Отправлено уведомление пользователю chatId={} для усыновления ID={}",
                                            user.getChatId(), adoptionId);
                                } catch (Exception e) {
                                    log.error("Ошибка отправки уведомления пользователю chatId={} для усыновления ID={}: {}",
                                            user.getChatId(), adoptionId, e.getMessage(), e);
                                }
                            } else {
                                log.error("Пользователь отсутствует для усыновления ID={}", adoptionId);
                                sendResponseWithBackButton(chatId, "Ошибка: пользователь не найден.", bot);
                            }
                            break;
                        case "Продлить на 14 дней":
                            adoption.setTrialStatus(TrialStatus.EXTENDED);
                            adoption.setTrialEndDate(adoption.getTrialEndDate() != null ? adoption.getTrialEndDate().plusDays(14) : LocalDate.now().plusDays(14));
                            adoptionService.save(adoption);
                            sendResponseWithBackButton(chatId, "Испытательный срок продлён на 14 дней.", bot);
                            if (adoption.getUser() != null) {
                                sendResponseWithBackButton(adoption.getUser().getChatId(),
                                        String.format("Испытательный срок для %s %s продлён на 14 дней. Продолжайте отправлять отчёты.",
                                                adoption.getAnimal().getType(), adoption.getAnimal().getName()),
                                        bot);
                            } else {
                                log.error("Пользователь отсутствует для усыновления ID={}", adoptionId);
                            }
                            break;
                        case "Продлить на 30 дней":
                            adoption.setTrialStatus(TrialStatus.EXTENDED);
                            adoption.setTrialEndDate(adoption.getTrialEndDate() != null ? adoption.getTrialEndDate().plusDays(30) : LocalDate.now().plusDays(30));
                            adoptionService.save(adoption);
                            sendResponseWithBackButton(chatId, "Испытательный срок продлён на 30 дней.", bot);
                            if (adoption.getUser() != null) {
                                sendResponseWithBackButton(adoption.getUser().getChatId(),
                                        String.format("Испытательный срок для %s %s продлён на 30 дней. Продолжайте отправлять отчёты.",
                                                adoption.getAnimal().getType(), adoption.getAnimal().getName()),
                                        bot);
                            } else {
                                log.error("Пользователь отсутствует для усыновления ID={}", adoptionId);
                            }
                            break;
                        case "Не пройден":
                            adoption.setTrialStatus(TrialStatus.FAILED);
                            adoptionService.save(adoption);
                            try {
                                // Удаляем связанные отчёты
                                reportService.deleteByAdoption(adoption);
                                log.info("Удалены отчёты для усыновления ID={}", adoptionId);
                                // Удаляем усыновление
                                adoptionService.delete(adoption);
                                log.info("Усыновление ID={} удалено из базы данных", adoptionId);
                            } catch (Exception e) {
                                log.error("Ошибка удаления отчётов или усыновления ID={}: {}", adoptionId, e.getMessage(), e);
                                throw new RuntimeException("Не удалось удалить отчёты или усыновление", e);
                            }
                            sendResponseWithBackButton(chatId, "Испытательный срок не пройден. Животное возвращается в приют.", bot);
                            if (adoption.getUser() != null) {
                                sendResponseWithBackButton(adoption.getUser().getChatId(),
                                        String.format("К сожалению, испытательный срок для %s %s не пройден. Пожалуйста, верните животное в приют.",
                                                adoption.getAnimal().getType(), adoption.getAnimal().getName()),
                                        bot);
                            } else {
                                log.error("Пользователь отсутствует для усыновления ID={}", adoptionId);
                            }
                            break;
                        default:
                            sendResponseWithBackButton(chatId, "Пожалуйста, выберите одно из предложенных решений.", bot);
                            return;
                    }
                    userStates.remove(chatId);
                    pendingTrialAdoption.remove(chatId);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                } catch (Exception e) {
                    log.error("Ошибка обработки решения для усыновления ID={} для chatId={}: {}", adoptionId, chatId, e.getMessage(), e);
                    sendResponseWithBackButton(chatId, "Ошибка при обработке решения. Попробуйте снова.", bot);
                    sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
                }
                break;
            default:
                log.error("Неизвестное состояние '{}' для chatId={}", state, chatId);
                sendResponseWithBackButton(chatId, "Неизвестное состояние. Начните заново.", bot);
                sendVolunteerMenu(chatId, bot, "Меню волонтёра:");
        }
    }

    private void sendResponseWithBackButton(long chatId, String text, TelegramBot bot) {
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
            log.info("Отправлено сообщение для chatId={}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения для chatId={}: {}", chatId, e.getMessage(), e);
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