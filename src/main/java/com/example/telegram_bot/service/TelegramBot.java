package com.example.telegram_bot.service;

import com.example.telegram_bot.config.BotConfig;
import com.example.telegram_bot.info_for_shelter.ShelterInfoService;
import com.example.telegram_bot.jpa.Role;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.owner_consultation.ConsultationOwnerService;
import com.example.telegram_bot.report_pet.PetReport;
import com.example.telegram_bot.volounter.VolunteerMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig config;
    private final ShelterInfoService shelterInfoService;
    private final ConsultationOwnerService consultationOwnerService;
    private final PetReport petReport;
    private final UserService userService;
    private final VolunteerMenuService volunteerMenuService;

    public TelegramBot(BotConfig config, ShelterInfoService shelterInfoService, ConsultationOwnerService consultationOwnerService, PetReport petReport, UserService userService, VolunteerMenuService volunteerMenuService) {
        this.config = config;
        this.shelterInfoService = shelterInfoService;
        this.consultationOwnerService = consultationOwnerService;
        this.petReport = petReport;
        this.userService = userService;
        this.volunteerMenuService = volunteerMenuService;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Старт бота"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка в боте: " + e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().hasText() ? update.getMessage().getText() : "";
            boolean hasPhoto = update.getMessage().hasPhoto();

            log.info("Пользователь chatId={} отправил сообщение: text={}, hasPhoto={}", chatId, messageText, hasPhoto);

            // Обработка состояний отчётов
            if (petReport.isAwaitingInput(chatId)) {
                if (messageText.equals("Вернуться в меню отчётов")) {
                    petReport.handleReturnToReportMenu(chatId, this);
                } else {
                    petReport.handleUserInput(chatId, messageText, hasPhoto, this);
                }
                return;
            }

            // Обработка волонтёрского ввода
            if (volunteerMenuService.isAwaitingInput(chatId)) {
                log.debug("Сообщение '{}' для chatId={} передано в handleAnimalInput", messageText, chatId);
                volunteerMenuService.handleAnimalInput(messageText, chatId, this);
                return;
            }

            if (update.getMessage().hasText()) {
                if (volunteerMenuService.isVolunteerCommand(messageText)) {
                    volunteerMenuService.handleVolunteerCommand(messageText, chatId, this);
                    return;
                }

                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "Узнать информацию о приюте":
                        shelterInfoService.sendShelterMenu(chatId, this, shelterInfoService.getShelterInfo());
                        break;
                    case "Как взять животное из приюта?":
                        consultationOwnerService.menu(chatId, this, consultationOwnerService.hello());
                        break;
                    case "Прислать отчет о питомце":
                        petReport.sendReportMenu(chatId, this, petReport.getReportInfo());
                        break;
                    case "Позвать волонтера":
                        sendMessage(chatId, shelterInfoService.getVolunteerContact());
                        break;
                    case "Я волонтёр":
                        if (userService.isAdmin(chatId)) {
                            volunteerMenuService.sendVolunteerMenu(chatId, this, "Меню волонтёра:");
                            log.info("Пользователь chatId={} с ролью ROLE_ADMIN открыл меню волонтёра", chatId);
                        } else {
                            sendMessage(chatId, "Доступ запрещён: только для волонтёров.");
                        }
                        break;
                    default:
                        if (shelterInfoService.isAwaitingContact(chatId)) {
                            shelterInfoService.handleContactInput(messageText, chatId, this);
                        } else if (consultationOwnerService.isAwaitingContact(chatId)) {
                            consultationOwnerService.handleContactInput(messageText, chatId, this);
                        } else if (shelterInfoService.isShelterCommand(messageText)) {
                            shelterInfoService.handleShelterMenuCommand(messageText, chatId, this);
                        } else if (consultationOwnerService.isConsultationCommand(messageText)) {
                            consultationOwnerService.handleConsultationCommand(messageText, chatId, this);
                        } else if (petReport.isReportCommand(messageText)) {
                            petReport.handleReportMenuCommand(messageText, chatId, this);
                        } else {
                            sendMessage(chatId, "Извини, такой команды нету. Попробуй позвать волонтера!");
                        }
                }
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        try {
            UserEntity user = userService.findByChatId(chatId);
            if (user == null) {
                user = new UserEntity();
                user.setChatId(chatId);
                user.setSubscribed(false);
                user.setRole(Role.ROLE_USER);
            }
            user.setName(name);
            userService.save(user);
            log.info("Обновлена/создана запись пользователя: chatId={}, name={}", chatId, name);

            String answer = "Привет, " + name + ", здесь ты можешь выбрать для себя котика или собачку)";
            sendMessage(chatId, answer);
        } catch (Exception e) {
            log.error("Ошибка сохранения пользователя chatId={}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    public void sendMainMenu(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend.isEmpty() ? "Выберите действие:" : textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Узнать информацию о приюте");
        row.add("Как взять животное из приюта?");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Прислать отчет о питомце");
        row.add("Позвать волонтера");
        keyboardRows.add(row);

        if (userService.isAdmin(chatId)) {
            row = new KeyboardRow();
            row.add("Я волонтёр");
            keyboardRows.add(row);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
            log.debug("Отправлено главное меню для chatId={}: {}", chatId, textToSend);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки главного меню для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        sendMainMenu(chatId, textToSend);
    }
}
