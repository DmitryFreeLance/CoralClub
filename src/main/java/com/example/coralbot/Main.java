package com.example.coralbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static String getenvOrDefault(String name, String defaultValue) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public static void main(String[] args) throws Exception {
        String token = getenvOrDefault("BOT_TOKEN", "8254293917:AAHARuZK11OaiVxrZWiz-bnnTGShXGKyZks");
        String username = getenvOrDefault("BOT_USERNAME", "SocialMarketting_Bot");
        String dbPath = getenvOrDefault("DB_PATH", "bot.db");
        String mediaBasePath = getenvOrDefault("MEDIA_BASE_PATH", "media");
        String adminIdsRaw = getenvOrDefault("ADMIN_IDS", "726773708");

        List<Long> adminIds = Arrays.stream(adminIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        Database database = new Database(dbPath);
        database.init();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new CoralClubBot(token, username, database, adminIds, mediaBasePath));
    }
}