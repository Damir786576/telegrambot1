package com.example.telegram_bot.service;

import com.example.telegram_bot.config.BotConfig;
import com.example.telegram_bot.info_for_shelter.ShelterInfoService;
import com.example.telegram_bot.owner_consultation.ConsultationOwnerService;
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

    public TelegramBot(BotConfig config, ShelterInfoService shelterInfoService, ConsultationOwnerService consultationOwnerService) {
        this.config = config;
        this.shelterInfoService = shelterInfoService;
        this.consultationOwnerService = consultationOwnerService;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Старт бота"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка в боте: " + e.getMessage());
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            log.info("User chatId={} sent message: {}", chatId, messageText);

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "Узнать информацию о приюте":
                    shelterInfoService.sendShelterMenu(chatId, this, shelterInfoService.getShelterInfo());
                    break;
                case "Как взять животное из приюта?":
                    consultationOwnerService.menu(chatId,this, consultationOwnerService.hello());
                    break;
                case "Прислать отчет о питомце":
                    sendMessage(chatId, "Пока не реализовано. Обратитесь к волонтеру!");
                    break;
                case "Позвать волонтера":
                    sendMessage(chatId, shelterInfoService.getVolunteerContact());
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
                    } else {
                        sendMessage(chatId, "Извини, такой команды нету. Попробуй позвать волонтера!");
                    }
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ", здесь ты можешь выбрать для себя котика или собачку)";
        sendMessage(chatId, answer);
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

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        sendMainMenu(chatId, textToSend);
    }
}
