package com.example.telegram_bot.owner_consultation;

import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.service.AnimalService;
import com.example.telegram_bot.service.TelegramBot;
import com.example.telegram_bot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ConsultationOwnerService {
    private static final Logger log = LoggerFactory.getLogger(ConsultationOwnerService.class);
    private static final List<String> CONSULTATIONOWNER_COMMANDS = Arrays.asList(
            "Список всех животных", "Правила знакомства с животным", "Список необходимых документов",
            "Рекомендации по транспортировке", "Рекомендации по обустройству дома для котика или щенка",
            "Рекомендации по обустройству дома для взрослого животного",
            "Рекомендации по обустройству дома для животного с ограниченными возможностями",
            "Советы кинолога", "Проверенные кинологи",
            "Причины не дать животное", "Записать контактные данные",
            "Позвать волонтера", "Вернуться в главное меню", "Вернуться в меню консультации"
    );

    private static final String ANIMAL_RULE = "Тут типо правила как забрать котенка из приюта";
    private static final String DOCUMENTS = "Необходимые документы: паспорт, договор с приютом.";
    private static final String TRANSPORT = "Рекомендации: используйте переноску, избегайте стресса.";
    private static final String HOME_KITTEN = "Для котика/щенка: лоток, миски, игрушки.";
    private static final String HOME_ADULT = "Для взрослого: место для отдыха, сбалансированное питание.";
    private static final String HOME_DISABLED = "Для животного с ограничениями: доступность, специальные приспособления.";
    private static final String CYNOLOGIST_ADVICE = "Советы кинолога: регулярные прогулки, обучение командам.";
    private static final String CYNOLOGIST_RECOMMEND = "Проверенные кинологи: Иван (+7-900-111-2222), Анна (+7-900-333-4444).";
    private static final String REASONS_DENY = "Причины отказа: неподходящие условия, отсутствие опыта.";
    private static final String VOLUNTEER = "Связываем вас с волонтером: +7-900-987-6543";

    private final Map<Long, String> userStates = new HashMap<>();
    private final UserService userService;
    private final AnimalService animalService;

    public ConsultationOwnerService(UserService userService, AnimalService animalService) {
        this.userService = userService;
        this.animalService = animalService;
    }

    public String hello() {
        return "Привет, в этом разделе ты можешь ознакомиться с животными, а также узнать как с ним взаимодействовать)))";
    }

    public boolean isConsultationCommand(String command) {
        return CONSULTATIONOWNER_COMMANDS.contains(command);
    }

    public boolean isAwaitingContact(long chatId) {
        return "AWAITING_CONTACT".equals(userStates.get(chatId));
    }

    public void menu(long chatId, TelegramLongPollingBot bot, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text.isEmpty() ? "Выберите действие:" : text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Список всех животных");
        row1.add("Правила знакомства с животным");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Список необходимых документов");
        row2.add("Рекомендации по транспортировке");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Рекомендации по обустройству дома для котика или щенка");
        keyboardRows.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Рекомендации по обустройству дома для взрослого животного");
        keyboardRows.add(row4);

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Рекомендации по обустройству дома для животного с ограниченными возможностями");
        keyboardRows.add(row5);

        KeyboardRow row6 = new KeyboardRow();
        row6.add("Советы кинолога");
        row6.add("Проверенные кинологи");
        keyboardRows.add(row6);

        KeyboardRow row7 = new KeyboardRow();
        row7.add("Причины не дать животное");
        row7.add("Записать контактные данные");
        keyboardRows.add(row7);

        KeyboardRow row8 = new KeyboardRow();
        row8.add("Позвать волонтера");
        row8.add("Вернуться в главное меню");
        keyboardRows.add(row8);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
            log.debug("Отправлено меню консультации для chatId={}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки меню консультации для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public void handleConsultationCommand(String command, long chatId, TelegramLongPollingBot bot) {
        log.info("Пользователь chatId={} выбрал команду: {}", chatId, command);
        switch (command) {
            case "Список всех животных":
                Iterable<AnimalEntity> animals = animalService.findAll();
                String response;
                if (!animals.iterator().hasNext()) {
                    response = "🐾 Животные не найдены.";
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    StringBuilder sb = new StringBuilder("🐾 Список животных в приюте:\n\n");
                    List<AnimalEntity> animalList = StreamSupport.stream(animals.spliterator(), false)
                            .collect(Collectors.toList());
                    for (int i = 0; i < animalList.size(); i++) {
                        AnimalEntity animal = animalList.get(i);
                        sb.append(String.format("%d. %s %s\n", i + 1, animal.getType(), animal.getName()));
                        sb.append(String.format("   ID: %d\n", animal.getId()));
                        sb.append(String.format("   Возраст: %d года\n", animal.getAge()));
                        sb.append(String.format("   Добавлено: %s\n", animal.getCreatedAt().format(formatter)));
                        if (i < animalList.size() - 1) {
                            sb.append("\n");
                        }
                    }
                    response = sb.toString();
                }
                sendResponseWithBackButton(chatId, response, bot);
                log.info("Отправлен список животных для chatId={}: {}", chatId, response);
                break;
            case "Правила знакомства с животным":
                sendResponseWithBackButton(chatId, ANIMAL_RULE, bot);
                break;
            case "Список необходимых документов":
                sendResponseWithBackButton(chatId, DOCUMENTS, bot);
                break;
            case "Рекомендации по транспортировке":
                sendResponseWithBackButton(chatId, TRANSPORT, bot);
                break;
            case "Рекомендации по обустройству дома для котика или щенка":
                sendResponseWithBackButton(chatId, HOME_KITTEN, bot);
                break;
            case "Рекомендации по обустройству дома для взрослого животного":
                sendResponseWithBackButton(chatId, HOME_ADULT, bot);
                break;
            case "Рекомендации по обустройству дома для животного с ограниченными возможностями":
                sendResponseWithBackButton(chatId, HOME_DISABLED, bot);
                break;
            case "Советы кинолога":
                sendResponseWithBackButton(chatId, CYNOLOGIST_ADVICE, bot);
                break;
            case "Проверенные кинологи":
                sendResponseWithBackButton(chatId, CYNOLOGIST_RECOMMEND, bot);
                break;
            case "Причины не дать животное":
                sendResponseWithBackButton(chatId, REASONS_DENY, bot);
                break;
            case "Записать контактные данные":
                userStates.put(chatId, "AWAITING_CONTACT");
                sendResponseWithBackButton(chatId, "Введите номер телефона, например: +79123456789, 79123456789, +7-912-345-6789 или 7 912 345 6789", bot);
                break;
            case "Позвать волонтера":
                sendResponseWithBackButton(chatId, VOLUNTEER, bot);
                break;
            case "Вернуться в главное меню":
                userStates.remove(chatId);
                try {
                    ((TelegramBot) bot).sendMainMenu(chatId, "");
                } catch (Exception e) {
                    log.error("Ошибка возврата в главное меню: {}", e.getMessage(), e);
                }
                break;
            case "Вернуться в меню консультации":
                userStates.remove(chatId);
                menu(chatId, bot, "");
                break;
            default:
                sendResponseWithBackButton(chatId, "Неизвестная команда", bot);
        }
    }

    public void handleContactInput(String input, long chatId, TelegramLongPollingBot bot) {
        if ("Вернуться в меню консультации".equals(input)) {
            userStates.remove(chatId);
            menu(chatId, bot, "");
            return;
        }

        Pattern pattern = Pattern.compile("\\+?7[- ]?9[- ]?\\d{3}[- ]?\\d{4}[- ]?\\d{2}");
        if (pattern.matcher(input).matches()) {
            UserEntity user = userService.findByChatId(chatId);
            if (user == null) {
                user = new UserEntity();
                user.setChatId(chatId);
                user.setSubscribed(false);
            }
            user.setPhone(input);
            userService.save(user);
            userStates.remove(chatId);
            sendResponseWithBackButton(chatId, "Контакты сохранены: " + input, bot);
            log.info("Сохранены контакты для chatId={}: {}", chatId, input);
        } else {
            sendResponseWithBackButton(chatId, "Неверный формат. Введите номер телефона, например: +79123456789, 79123456789, +7-912-345-6789 или 7 912 345 6789", bot);
        }
    }

    private void sendResponseWithBackButton(long chatId, String text, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться в меню консультации");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
            log.debug("Отправлен ответ для chatId={}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }
}
