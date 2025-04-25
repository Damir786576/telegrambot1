package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.jpa.TrialStatus;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.repository.AdoptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdoptionService {
    private static final Logger log = LoggerFactory.getLogger(AdoptionService.class);
    private final AdoptionRepository repo;
    private final AnimalService animalService;
    private final UserService userService;
    private final Map<Long, String> userStates = new HashMap<>();
    private static final String AWAITING_ANIMAL_ID = "AWAITING_ANIMAL_ID";

    public AdoptionService(AdoptionRepository repo, AnimalService animalService, UserService userService) {
        this.repo = repo;
        this.animalService = animalService;
        this.userService = userService;
    }

    public void save(AdoptionEntity entity) {
        repo.save(entity);
        log.info("Сохранено усыновление ID={}", entity.getId());
    }

    public void delete(AdoptionEntity entity) {
        if (entity != null) {
            repo.delete(entity);
            log.info("Удалено усыновление ID={}", entity.getId());
        } else {
            log.warn("Попытка удаления null усыновления");
        }
    }

    public AdoptionEntity findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<AdoptionEntity> findByUser(UserEntity user) {
        List<AdoptionEntity> adoptions = repo.findByUser(user);
        log.info("Найдено {} усыновлений для пользователя chatId={}", adoptions.size(), user.getChatId());
        return adoptions;
    }

    public List<AdoptionEntity> findAllAdoptions() {
        List<AdoptionEntity> adoptions = repo.findAll();
        log.info("Найдено {} усыновлений в системе", adoptions.size());
        return adoptions;
    }

    public String getAllAdoptionsString() {
        List<AdoptionEntity> adoptions = findAllAdoptions();
        if (adoptions.isEmpty()) {
            log.info("Нет усыновлений в системе");
            return "🐾 Нет усыновлений в системе.";
        }

        StringBuilder response = new StringBuilder("🐾 Список всех усыновлений:\n\n");
        for (int i = 0; i < adoptions.size(); i++) {
            AdoptionEntity adoption = adoptions.get(i);
            AnimalEntity animal = adoption.getAnimal();
            UserEntity user = adoption.getUser();
            response.append(String.format("%d. ID усыновления: %d\n", i + 1, adoption.getId()));
            response.append(String.format("   Животное: %s %s (ID: %d)\n", animal.getType(), animal.getName(), animal.getId()));
            response.append(String.format("   Пользователь: %s (Chat ID: %d)\n", user.getName() != null ? user.getName() : "Неизвестно", user.getChatId()));
            response.append(String.format("   Дата усыновления: %s\n", adoption.getAdoptionDate().toString()));
            response.append(String.format("   Статус: %s\n", adoption.getTrialStatus()));
            if (i < adoptions.size() - 1) {
                response.append("\n");
            }
        }
        log.info("Сформирован список всех усыновлений, размер: {}", adoptions.size());
        return response.toString();
    }

    public void startAdoptionProcess(long chatId, TelegramBot bot) {
        log.debug("Начало startAdoptionProcess для chatId={}", chatId);
        String response = animalService.getAnimalListString();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(response);
        try {
            bot.execute(message);
            log.info("Отправлен список животных для усыновления для chatId={}", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки списка животных для chatId={}: {}", chatId, e.getMessage(), e);
            return;
        }

        log.debug("Установка состояния AWAITING_ANIMAL_ID для chatId={}", chatId);
        userStates.put(chatId, AWAITING_ANIMAL_ID);
        SendMessage prompt = new SendMessage();
        prompt.setChatId(String.valueOf(chatId));
        prompt.setText("Для того чтобы взять нужное животное, напишите его ID:");
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в главное меню");
        row.add("Отмена");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        prompt.setReplyMarkup(keyboardMarkup);
        try {
            bot.execute(prompt);
            log.info("Запрошен ID животного для chatId={}", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки запроса ID для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public boolean isAwaitingInput(long chatId) {
        return userStates.containsKey(chatId);
    }

    public void handleAdoptionInput(String input, long chatId, TelegramBot bot) {
        if (!AWAITING_ANIMAL_ID.equals(userStates.get(chatId))) {
            log.error("Состояние отсутствует или неверное для chatId={}", chatId);
            sendResponseWithBackButton(chatId, "Ошибка. Начните процесс усыновления заново.", bot);
            userStates.remove(chatId);
            return;
        }

        if ("Вернуться в главное меню".equals(input) || "Отмена".equals(input)) {
            userStates.remove(chatId);
            bot.sendMainMenu(chatId, "Выберите действие:");
            log.info("Пользователь chatId={} {} процесс усыновления", chatId, input.equals("Отмена") ? "отменил" : "вернулся в главное меню");
            return;
        }

        try {
            long animalId = Long.parseLong(input);
            AnimalEntity animal = animalService.findById(animalId);
            if (animal == null) {
                sendResponseWithBackButton(chatId, "Животное с ID " + animalId + " не найдено.", bot);
                log.warn("Животное с ID={} не найдено для chatId={}", animalId, chatId);
                return;
            }

            AdoptionEntity adoption = repo.findByAnimal(animal);
            if (adoption != null && (adoption.getTrialStatus() == TrialStatus.IN_PROGRESS || adoption.getTrialStatus() == TrialStatus.EXTENDED)) {
                sendResponseWithBackButton(chatId, String.format("Животное %s %s (ID: %d) уже усыновлено.", animal.getType(), animal.getName(), animalId), bot);
                log.warn("Животное ID={} уже усыновлено (статус: {}) для chatId={}", animalId, adoption.getTrialStatus(), chatId);
                userStates.remove(chatId);
                return;
            }

            UserEntity user = userService.findByChatId(chatId);
            if (user == null) {
                sendResponseWithBackButton(chatId, "Пользователь не найден. Зарегистрируйтесь.", bot);
                log.warn("Пользователь не найден для chatId={}", chatId);
                userStates.remove(chatId);
                return;
            }

            AdoptionEntity newAdoption = new AdoptionEntity();
            newAdoption.setUser(user);
            newAdoption.setAnimal(animal);
            newAdoption.setAdoptionDate(LocalDate.now());
            newAdoption.setShelterId(1L);
            newAdoption.setTrialStatus(TrialStatus.IN_PROGRESS);
            newAdoption.setTrialEndDate(LocalDate.now().plusDays(30));
            repo.save(newAdoption);

            String response = String.format("Поздравляю с усыновлением! 🎉 Вы взяли %s %s (ID: %d)! Спасибо за усыновление!",
                    animal.getType(), animal.getName(), animalId);
            sendResponseWithBackButton(chatId, response, bot);
            userStates.remove(chatId);

            try {
                Thread.sleep(1000);
                bot.sendMainMenu(chatId, "Выберите действие:");
            } catch (InterruptedException e) {
                log.error("Ошибка задержки для chatId={}: {}", chatId, e.getMessage(), e);
            }

            log.info("Усыновление сохранено: userId={}, animalId={} для chatId={}", user.getId(), animalId, chatId);
        } catch (NumberFormatException e) {
            sendResponseWithBackButton(chatId, "Пожалуйста, введите корректный ID (число).", bot);
            log.warn("Некорректный ID '{}' для chatId={}", input, chatId);
        }
    }

    public void showUserAdoptions(long chatId, TelegramBot bot) {
        log.debug("Начало showUserAdoptions для chatId={}", chatId);
        UserEntity user = userService.findByChatId(chatId);
        if (user == null) {
            sendResponseWithBackButton(chatId, "Пользователь не найден. Зарегистрируйтесь.", bot);
            log.warn("Пользователь не найден для chatId={}", chatId);
            return;
        }

        List<AdoptionEntity> adoptions = repo.findByUser(user);
        if (adoptions.isEmpty()) {
            sendResponseWithBackButton(chatId, "🐾 Вы ещё не усыновили животных.", bot);
            log.info("Усыновления не найдены для chatId={}", chatId);
            return;
        }

        StringBuilder response = new StringBuilder("🐾 Ваши усыновлённые животные:\n\n");
        for (AdoptionEntity adoption : adoptions) {
            AnimalEntity animal = adoption.getAnimal();
            response.append(String.format("%s %s (ID: %d)\n", animal.getType(), animal.getName(), animal.getId()));
            response.append(String.format("   Дата усыновления: %s\n", adoption.getAdoptionDate().toString()));
            response.append("\n");
        }
        sendResponseWithBackButton(chatId, response.toString(), bot);
        log.info("Отправлен список усыновлений для chatId={}", chatId);
    }

    private void sendResponseWithBackButton(long chatId, String text, TelegramBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в главное меню");
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
}