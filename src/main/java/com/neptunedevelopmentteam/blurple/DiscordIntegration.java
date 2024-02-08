package com.neptunedevelopmentteam.blurple;

import net.minecraft.server.MinecraftServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import java.net.MalformedURLException;
import java.net.URL;

public class DiscordIntegration {

    private static DiscordApi api;
    private static boolean initialized = false;
    private static MinecraftServer server;
    private static ServerTextChannel chat_text_channel;
    private static ServerTextChannel console_text_channel;
    private static IncomingWebhook chat_webhook;

    public static void init(MinecraftServer minecraftServer) {
        Blurple.LOGGER.info("Setting up Blurple");
        if (Blurple.CONFIG.BOT_TOKEN.equals("PUT YOUR BOT TOKEN HERE")) {
            Blurple.LOGGER.error("INVALID BOT TOKEN SET IN CONFIG!");
            return;
        }
        Blurple.LOGGER.info("Logging into discord...");
        server = minecraftServer;
        api = new DiscordApiBuilder()
                .setToken(Blurple.CONFIG.BOT_TOKEN)
                .addIntents(Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS)
                .login()
                .join();
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(Blurple.CONFIG.ACTIVITY_MESSAGE);
        Blurple.LOGGER.info("Logged into discord");
        BlurpleSlashCommands.init(api);
        setupTextChannels();
        setupWebhooks();
        initialized = true;
        Blurple.LOGGER.info("Blurple is now initialized");
    }

    public static void stop() {
        server = null;
        api.disconnect().join();
        api = null;
        BlurpleSlashCommands.stop();
        initialized = false;
    }

    public static URL getPlayerImageFromUUID(String uuid) {
        try {
            return new URL("https://mc-heads.net/head/" + uuid +  "/right.png");
        } catch (MalformedURLException e) {
            Blurple.LOGGER.warn("Failed to get player image from UUID: " + uuid);
            return null;
        }
    }

    public static void setupWebhooks() {
        if (chat_webhook != null) return;
        Blurple.LOGGER.info("Setting up webhooks");
        if (chat_text_channel == null || console_text_channel == null) {
            Blurple.LOGGER.warn("Text channels aren't setup!");
            Blurple.LOGGER.warn("Webhook setup cancelled");
            return;
        }
        if (!Blurple.CONFIG.WEBHOOK_LINK.isEmpty()) {
            // Don't need to check if it's present as the function does it for us
            chat_webhook = getWebhookFromUrlAndTextChannel(Blurple.CONFIG.WEBHOOK_LINK, chat_text_channel).asIncomingWebhook().get();
        }
        else {
            chat_webhook = new WebhookBuilder(chat_text_channel)
                    .setName("Minecraft Chat")
                    .create()
                    .join();
            Blurple.CONFIG.WEBHOOK_LINK = chat_webhook.getUrl().toString();
        }
        Blurple.LOGGER.info("Webhooks set up");

    }

    public static Webhook getWebhookFromUrlAndTextChannel(String url, ServerTextChannel channel) {
        return channel.getWebhooks().join().stream().filter(webhook -> webhook.isIncomingWebhook() && webhook.asIncomingWebhook().isPresent() && webhook.asIncomingWebhook().get().getUrl().toString().equals(url)).toList().get(0);
    }

    public static void setupTextChannels() {
        if (chat_text_channel != null && console_text_channel != null) return;
        Blurple.LOGGER.info("Setting up text channels");
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L) {
            Blurple.LOGGER.warn("Server chat channel isn't setup!");
        }
        else if (chat_text_channel == null) {
            var temp = api.getServerTextChannelById(Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID);
            if (temp.isEmpty()) {
                Blurple.LOGGER.warn("Invalid server chat channel id, resetting channel id");
                Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID = 0L;
            }
            else {
                chat_text_channel = temp.get();
            }
        }
        if (Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID == 0L) {
            Blurple.LOGGER.warn("Server console channel isn't setup!");
        }
        else if (console_text_channel == null) {
            var temp = api.getServerTextChannelById(Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID);
            if (temp.isEmpty()) {
                Blurple.LOGGER.warn("Invalid server console channel id, resetting channel id");
                Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID = 0L;
            }
            else {
                console_text_channel = temp.get();
            }
        }
        Blurple.LOGGER.info("Text channels set up");
    }
}
