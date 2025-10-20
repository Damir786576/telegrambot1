package com.example.telegram_bot.report_pet;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.ReportEntity;
import com.example.telegram_bot.jpa.TrialStatus;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.service.AdoptionService;
import com.example.telegram_bot.service.ReportService;
import com.example.telegram_bot.service.TelegramBot;
import com.example.telegram_bot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
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
    private final AdoptionService adoptionService;
    private final ReportService reportService;
    private final UserService userService;

    public PetReport(AdoptionService adoptionService, ReportService reportService, UserService userService) {
        this.adoptionService = adoptionService;
        this.reportService = reportService;
        this.userService = userService;
    }

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
        log.info("Обработка команды отчёта: command='{}', chatId={}", command, chatId);
        if (userStates.containsKey(chatId) && isReportCommand(command)) {
            userStates.remove(chatId);
        }

        switch (command) {
            case "Прислать фото животного":
                userStates.put(chatId, "AWAITING_PHOTO");
                log.info("Установлено состояние AWAITING_PHOTO для chatId={}", chatId);
                sendResponseWithBackButton(chatId, PHOTO_ANIMAL, bot);
                break;
            case "Прислать рацион животного":
                userStates.put(chatId, "AWAITING_DIET");
                log.info("Установлено состояние AWAITING_DIET для chatId={}", chatId);
                sendResponseWithBackButton(chatId, DIET_ANIMAL, bot);
                break;
            case "Самочуствие и привыкание к новому месту":
                userStates.put(chatId, "AWAITING_WELLBEING");
                log.info("Установлено состояние AWAITING_WELLBEING для chatId={}", chatId);
                sendResponseWithBackButton(chatId, WELLBEING, bot);
                break;
            case "Изменения в поведении":
                userStates.put(chatId, "AWAITING_DEMEANOR");
                log.info("Установлено состояние AWAITING_DEMEANOR для chatId={}", chatId);
                sendResponseWithBackButton(chatId, DEMEANOR, bot);
                break;
            case "Вернуться в главное меню":
                userStates.remove(chatId);
                log.info("Сброшено состояние для chatId={}", chatId);
                ((TelegramBot) bot).sendMainMenu(chatId, "");
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда. Выберите действие из меню.", bot);
        }
    }

    public void handleUserInput(long chatId, Update update, TelegramLongPollingBot bot) {
        String state = userStates.get(chatId);
        log.info("handleUserInput вызван для chatId={}, состояние={}", chatId, state);
        if (state == null) {
            log.warn("Состояние отсутствует для chatId={}", chatId);
            sendResponseWithBackButton(chatId, "Ошибка. Начните заново.", bot);
            return;
        }

        if (!update.hasMessage()) {
            log.warn("Сообщение отсутствует в Update для chatId={}", chatId);
            sendResponseWithBackButton(chatId, "Ошибка. Пожалуйста, отправьте сообщение или фото.", bot);
            return;
        }

        boolean hasPhoto = update.getMessage().hasPhoto();
        String input = update.getMessage().hasText() ? update.getMessage().getText() : "";
        log.info("Сообщение для chatId={}: hasPhoto={}, text='{}'", chatId, hasPhoto, input);

        UserEntity user = userService.findByChatId(chatId);
        if (user == null) {
            userStates.remove(chatId);
            sendResponseWithBackButton(chatId, "Пользователь не найден. Зарегистрируйтесь.", bot);
            log.warn("Пользователь не найден для chatId={}", chatId);
            return;
        }

        List<AdoptionEntity> adoptions = adoptionService.findByUser(user);
        log.info("Найдено {} усыnovлений для chatId={}", adoptions.size(), chatId);
        adoptions.forEach(adoption -> log.info("Усыновление ID={}, trial_status={}", adoption.getId(), adoption.getTrialStatus()));
        AdoptionEntity activeAdoption = adoptions.stream()
                .filter(adoption -> adoption.getTrialStatus() == TrialStatus.IN_PROGRESS || adoption.getTrialStatus() == TrialStatus.EXTENDED)
                .findFirst()
                .orElse(null);

        if (activeAdoption == null) {
            userStates.remove(chatId);
            sendResponseWithBackButton(chatId, "У вас нет активных усыновлений для отправки отчётов.", bot);
            log.warn("Активное усыновление не найдено для chatId={}", chatId);
            return;
        }

        UserEntity volunteer = userService.findAll().stream()
                .filter(u -> u.getRole() == com.example.telegram_bot.jpa.Role.ROLE_ADMIN)
                .findFirst()
                .orElse(null);
        log.info("Волонтёр найден: {}", volunteer != null ? "chatId=" + volunteer.getChatId() : "не найден");

        String response;
        ReportEntity report = new ReportEntity();
        report.setAdoption(activeAdoption);
        report.setReportDate(LocalDateTime.now());

        switch (state) {
            case "AWAITING_PHOTO":
            case "AWAITING_DIET":
                log.info("Обработка фото для состояния {}, chatId={}", state, chatId);
                if (hasPhoto) {
                    List<PhotoSize> photos = update.getMessage().getPhoto();
                    String fileId = photos.stream()
                            .max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize()))
                            .map(PhotoSize::getFileId)
                            .orElse(null);
                    log.info("Получен fileId={} для chatId={}", fileId, chatId);
                    if (fileId != null) {
                        report.setPhotoUrl(fileId);
                        report.setContent(state.equals("AWAITING_PHOTO") ? "Фото животного" : "Фото рациона");
                        report.setReportType(state.equals("AWAITING_PHOTO") ? "PHOTO" : "DIET"); // Устанавливаем report_type
                        reportService.save(report);
                        response = state.equals("AWAITING_PHOTO") ?
                                "Фото животного получено и сохранено. Спасибо! Выберите следующее действие." :
                                "Фото рациона получено и сохранено. Спасибо! Выберите следующее действие.";
                        log.info("Сохранён отчёт с фото для усыновления ID={} для chatId={}", activeAdoption.getId(), chatId);

                        if (volunteer != null) {
                            try {
                                SendPhoto sendPhoto = new SendPhoto();
                                sendPhoto.setChatId(String.valueOf(volunteer.getChatId()));
                                sendPhoto.setPhoto(new InputFile(fileId));
                                sendPhoto.setCaption(String.format("Отчёт от %s (Chat ID: %d) для %s %s: %s",
                                        user.getName() != null ? user.getName() : "Неизвестно",
                                        chatId,
                                        activeAdoption.getAnimal().getType(),
                                        activeAdoption.getAnimal().getName(),
                                        state.equals("AWAITING_PHOTO") ? "Фото животного" : "Фото рациона"));
                                bot.execute(sendPhoto);
                                log.info("Фото отправлено волонтёру chatId={} для усыновления ID={}", volunteer.getChatId(), activeAdoption.getId());
                            } catch (TelegramApiException e) {
                                log.error("Ошибка отправки фото волонтёру chatId={} для усыновления ID={}: {}",
                                        volunteer.getChatId(), activeAdoption.getId(), e.getMessage(), e);
                            }
                        } else {
                            log.warn("Волонтёр (ROLE_ADMIN) не найден для отправки отчёта от chatId={}", chatId);
                        }

                        userStates.remove(chatId);
                        sendReportMenu(chatId, bot, response);
                    } else {
                        response = "Ошибка получения фотографии. Пожалуйста, попробуйте снова.";
                        sendResponseWithBackButton(chatId, response, bot);
                    }
                } else {
                    response = "Пожалуйста, пришлите фото.";
                    sendResponseWithBackButton(chatId, response, bot);
                }
                break;
            case "AWAITING_WELLBEING":
                log.info("Обработка текста (самочувствие) для chatId={}", chatId);
                if (!input.isEmpty()) {
                    report.setContent("Самочувствие: " + input);
                    report.setReportType("WELLBEING"); // Устанавливаем report_type
                    reportService.save(report);
                    response = "Информация о самочувствии сохранена. Спасибо! Выберите следующее действие.";
                    log.info("Сохранён текстовый отчёт (самочувствие) для усыновления ID={} для chatId={}", activeAdoption.getId(), chatId);

                    if (volunteer != null) {
                        try {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(String.valueOf(volunteer.getChatId()));
                            sendMessage.setText(String.format("Отчёт от %s (Chat ID: %d) для %s %s: Самочувствие: %s",
                                    user.getName() != null ? user.getName() : "Неизвестно",
                                    chatId,
                                    activeAdoption.getAnimal().getType(),
                                    activeAdoption.getAnimal().getName(),
                                    input));
                            bot.execute(sendMessage);
                            log.info("Текстовый отчёт отправлен волонтёру chatId={} для усыновления ID={}", volunteer.getChatId(), activeAdoption.getId());
                        } catch (TelegramApiException e) {
                            log.error("Ошибка отправки текстового отчёта волонтёру chatId={} для усыновления ID={}: {}",
                                    volunteer.getChatId(), activeAdoption.getId(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("Волонтёр (ROLE_ADMIN) не найден для отправки отчёта от chatId={}", chatId);
                    }

                    userStates.remove(chatId);
                    sendReportMenu(chatId, bot, response);
                } else {
                    response = "Пожалуйста, напишите текст о самочувствии.";
                    sendResponseWithBackButton(chatId, response, bot);
                }
                break;
            case "AWAITING_DEMEANOR":
                log.info("Обработка текста (поведение) для chatId={}", chatId);
                if (!input.isEmpty()) {
                    report.setContent("Поведение: " + input);
                    report.setReportType("DEMEANOR"); // Устанавливаем report_type
                    reportService.save(report);
                    response = "Информация о поведении сохранена. Спасибо! Выберите следующее действие.";
                    log.info("Сохранён текстовый отчёт (поведение) для усыновления ID={} для chatId={}", activeAdoption.getId(), chatId);

                    if (volunteer != null) {
                        try {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(String.valueOf(volunteer.getChatId()));
                            sendMessage.setText(String.format("Отчёт от %s (Chat ID: %d) для %s %s: Поведение: %s",
                                    user.getName() != null ? user.getName() : "Неизвестно",
                                    chatId,
                                    activeAdoption.getAnimal().getType(),
                                    activeAdoption.getAnimal().getName(),
                                    input));
                            bot.execute(sendMessage);
                            log.info("Текстовый отчёт отправлен волонтёру chatId={} для усыновления ID={}", volunteer.getChatId(), activeAdoption.getId());
                        } catch (TelegramApiException e) {
                            log.error("Ошибка отправки текстового отчёта волонтёру chatId={} для усыновления ID={}: {}",
                                    volunteer.getChatId(), activeAdoption.getId(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("Волонтёр (ROLE_ADMIN) не найден для отправки отчёта от chatId={}", chatId);
                    }

                    userStates.remove(chatId);
                    sendReportMenu(chatId, bot, response);
                } else {
                    response = "Пожалуйста, напишите текст о поведении.";
                    sendResponseWithBackButton(chatId, response, bot);
                }
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
        boolean awaiting = userStates.containsKey(chatId);
        log.info("Проверка isAwaitingInput для chatId={}: {}", chatId, awaiting);
        return awaiting;
    }

    public void handleReturnToReportMenu(long chatId, TelegramLongPollingBot bot) {
        userStates.remove(chatId);
        log.info("Сброшено состояние для chatId={}", chatId);
        sendReportMenu(chatId, bot, "Выберите действие:");
    }
}
