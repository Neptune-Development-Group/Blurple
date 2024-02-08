package com.neptunedevelopmentteam.blurple;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NeptuneDiscordIntegration {

    private static DiscordApi api;
    private static boolean initialized = false;

    private static SlashCommand bindChannelToMinecraftChatCommand;
    private static SlashCommand bindChannelToMinecraftConsoleCommand;
    private static ServerTextChannel binded_minecraft_chat_channel;
    private static ServerTextChannel binded_minecraft_console_channel;
    private static Webhook binded_minecraft_chat_webhook = null;
    private static MinecraftServer game_server;

    public static void init(MinecraftServer server) {
        game_server = server;
        if (Blurple.CONFIG.BOT_TOKEN.equals("PUT YOUR BOT TOKEN HERE")) {
            Blurple.LOGGER.error("INVALID BOT TOKEN SET IN CONFIG!");
            return;
        }
        api = new DiscordApiBuilder()
                .setToken(Blurple.CONFIG.BOT_TOKEN)
                .addIntents(Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS)
                .login()
                .join();
        api.updateStatus(UserStatus.ONLINE);
        api.setMessageCacheSize(0, 0);
        api.updateActivity("Watching the server");
        setupCommands();
        api.addSlashCommandCreateListener(NeptuneDiscordIntegration::slashCommandEventHandler);
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L) {
            Blurple.LOGGER.error("INVALID CHANNEL ID SET IN CONFIG!");
            initialized = true;
            return;
        }
        var optional_text_channel = api.getServerTextChannelById(Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID);
        if (optional_text_channel.isEmpty()) {
            Blurple.LOGGER.error("INVALID CHANNEL ID SET IN CONFIG!");
            initialized = true;
            return;
        }
        binded_minecraft_chat_channel = optional_text_channel.get();
        api.addMessageCreateListener(NeptuneDiscordIntegration::messageEventHandler);


        if (Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID == 0L) {
            Blurple.LOGGER.error("INVALID CHANNEL ID SET IN CONFIG!");
            initialized = true;
            return;
        }
        optional_text_channel = api.getServerTextChannelById(Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID);
        if (optional_text_channel.isEmpty()) {
            Blurple.LOGGER.error("INVALID CHANNEL ID SET IN CONFIG!");
            initialized = true;
            return;
        }
        binded_minecraft_console_channel = optional_text_channel.get();
        initialized = true;
    }

    public static void onIngameChatMessage(ServerPlayerEntity sender, Text message) {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L && binded_minecraft_chat_channel == null) return;
        if (binded_minecraft_chat_webhook == null) {
            binded_minecraft_chat_webhook = new WebhookBuilder(binded_minecraft_chat_channel)
                    .setName("Minecraft Chat")
                    .create()
                    .join();
        }
        String url_string = "https://mc-heads.net/head/" + sender.getUuidAsString() +  "/right.png";
        try {
            URL url = new URL(url_string);
            if (binded_minecraft_chat_webhook.asIncomingWebhook().isEmpty()) return;
            new WebhookMessageBuilder()
                    .append(message.getString())
                    .setDisplayName(sender.getName().getString())
                    .setDisplayAvatar(url)
                    .setAllowedMentions(new AllowedMentionsBuilder().build())
                    .send(binded_minecraft_chat_webhook.asIncomingWebhook().get());
        } catch (Exception e) {
            if (binded_minecraft_chat_webhook.asIncomingWebhook().isEmpty()) return;
            new WebhookMessageBuilder()
                    .append(message.getString())
                    .setDisplayName(sender.getName().getString())
                    .setAllowedMentions(new AllowedMentionsBuilder().build())
                    .setDisplayAvatar((URL) null)
                    .send(binded_minecraft_chat_webhook.asIncomingWebhook().get());
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L && binded_minecraft_chat_channel == null) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(player.getName().getString() + " joined the server!")
                .setColor(Color.GREEN)
                .setThumbnail("https://mc-heads.net/head/" + player.getUuidAsString() +  "/right.png");
        binded_minecraft_chat_channel.sendMessage(embed);
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L && binded_minecraft_chat_channel == null) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(player.getName().getString() + " left the server!")
                .setColor(Color.RED)
                .setThumbnail("https://mc-heads.net/head/" + player.getUuidAsString() +  "/right.png");
        binded_minecraft_chat_channel.sendMessage(embed);
    }

    public static void onServerStarted() {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L && binded_minecraft_chat_channel == null) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Server started!")
                .setColor(Color.GREEN);
        binded_minecraft_chat_channel.sendMessage(embed);
    }

    public static void onServerStopped() {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID == 0L && binded_minecraft_chat_channel == null) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Server stopped!")
                .setColor(Color.RED);
        binded_minecraft_chat_channel.sendMessage(embed);
    }

    private static void setupCommands() {
        setupBindChannelToMinecraftChatCommand();
        setupBindChannelToMinecraftConsoleCommand();
    }

    private static void messageEventHandler(MessageCreateEvent event) {
        if (event.getChannel().asServerTextChannel().isEmpty()) return;
        if (event.getMessageAuthor().isWebhook()) return;
        if (event.getMessageAuthor().isBotUser()) return;

        if (event.getChannel().asServerTextChannel().get().getId() == binded_minecraft_chat_channel.getId()) {
            Text discord_prefix = Text.literal("[DISCORD] ").setStyle(Style.EMPTY.withBold(true).withColor(TextColor.parse("#5865f2")).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.getMessageLink().toString())));
            Text username = Text.literal("<" + event.getMessageAuthor().getDisplayName() + "> ");
            Text message = getTextFromMessage(event.getMessage());
            if (event.getMessage().getType() == MessageType.REPLY && event.getMessage().getReferencedMessage().isPresent()) {
                Message referenceMessage = event.getMessage().getReferencedMessage().get();
                Text replyingToMessage = Text.literal("Replying to " + referenceMessage.getAuthor().getDisplayName() + ": ").setStyle(Style.EMPTY.withBold(true).withColor(TextColor.parse("#5865f2")).withItalic(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getTextFromMessage(referenceMessage)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.getMessageLink().toString())));
                Text final_message = Text.literal("").append(discord_prefix).append(replyingToMessage).append(username).append(message);
                game_server.getPlayerManager().broadcast(final_message, false);
                return;
            }
            Text final_message = Text.literal("").append(discord_prefix).append(username).append(message);
            game_server.getPlayerManager().broadcast(final_message, false);
        }
        else if (event.getChannel().asServerTextChannel().get().getId() == binded_minecraft_console_channel.getId()) {
            game_server.getCommandManager().executeWithPrefix(game_server.getCommandSource(), event.getMessageContent());
        }
    }

    private static Text getTextFromMessage(Message message) {
        String message_string = message.getContent();
        List<String> message_ats = new ArrayList<>();
        if (!message.getMentionedUsers().isEmpty()) {
            List<User> mentioned_users = message.getMentionedUsers();
            for (User user : mentioned_users) {
                if (message.getServer().isEmpty()) continue;
                message_string = message_string.replace("<@" + user.getId() + ">", "@" + user.getDisplayName(message.getServer().get()));
                message_ats.add("@" + user.getDisplayName(message.getServer().get()));
            }
        }
        if (!message.getMentionedRoles().isEmpty()) {
            List<Role> mentioned_roles = message.getMentionedRoles();
            for (Role role : mentioned_roles) {
                message_string = message_string.replace("<@&" + role.getId() + ">", "@" + role.getName());
                message_ats.add("@" + role.getName());
            }
        }
        if (!message.getMentionedChannels().isEmpty()) {
            List<ServerChannel> mentioned_channels = message.getMentionedChannels();
            for (ServerChannel channel : mentioned_channels) {
                message_string = message_string.replace("<#" + channel.getId() + ">", "#" + channel.getName());
                message_ats.add("#" + channel.getName());
            }
        }
        if (message_ats.isEmpty()) {
            return Text.literal(message_string);
        }
        return Text.literal(message_string);
    }
    public static void onIngameConsoleMessage(Text message) {
        if (!checkIfAllowedToRun()) return;
        if (Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID == 0L && binded_minecraft_console_channel == null) return;
        String message_string = message.getString();
        // @TODO: Add a fix for people being able to use @everyone and @here ingame
        binded_minecraft_console_channel.sendMessage(message.getString());
    }

    private static void slashCommandEventHandler(SlashCommandCreateEvent event) {
        SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
        if (slashCommandInteraction.getCommandId() == bindChannelToMinecraftChatCommand.getId()) {
            Optional<SlashCommandInteractionOption> optional_channel_option = slashCommandInteraction.getOptionByName("channel");
            if (optional_channel_option.isEmpty()) return;
            SlashCommandInteractionOption channel_option = optional_channel_option.get();
            Optional<ServerChannel> optional_channel = channel_option.getChannelValue();
            if (optional_channel.isEmpty()) return;
            ServerChannel channel = optional_channel.get();
            var temp  = channel.asServerTextChannel();
            if (temp.isEmpty()) {
                Blurple.LOGGER.error("INVALID CHANNEL ID SET FROM COMMAND");
                slashCommandInteraction
                        .createImmediateResponder()
                        .setContent("Invalid channel")
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond();
                return;
            }
            binded_minecraft_chat_channel = temp.get();
            Blurple.CONFIG.SERVER_CHAT_CHANNEL_ID = channel.getId();
            slashCommandInteraction
                    .createImmediateResponder()
                    .setContent("You've set the minecraft chat channel to " + channel.getName() + "!")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        }
        else if (slashCommandInteraction.getCommandId() == bindChannelToMinecraftConsoleCommand.getId()) {
            Optional<SlashCommandInteractionOption> optional_channel_option = slashCommandInteraction.getOptionByName("channel");
            if (optional_channel_option.isEmpty()) return;
            SlashCommandInteractionOption channel_option = optional_channel_option.get();
            Optional<ServerChannel> optional_channel = channel_option.getChannelValue();
            if (optional_channel.isEmpty()) return;
            ServerChannel channel = optional_channel.get();
            var temp = channel.asServerTextChannel();
            if (temp.isEmpty()) {
                Blurple.LOGGER.error("INVALID CHANNEL ID SET FROM COMMAND");
                slashCommandInteraction
                        .createImmediateResponder()
                        .setContent("Invalid channel")
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond();
                return;
            }
            binded_minecraft_console_channel = temp.get();
            Blurple.CONFIG.SERVER_CONSOLE_CHANNEL_ID = channel.getId();
            slashCommandInteraction
                    .createImmediateResponder()
                    .setContent("You've set the minecraft console channel to " + channel.getName() + "!")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        }
    }

    private static void setupBindChannelToMinecraftChatCommand() {
        bindChannelToMinecraftChatCommand = SlashCommand.with("bind_to_minecraft_chat", "Binds a channel to the minecraft chat",
                Collections.singletonList(
                        SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "The channel to bind", true)
                ))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();
        bindChannelToMinecraftChatCommand.createSlashCommandUpdater().updateGlobal(api);
    }


    private static void setupBindChannelToMinecraftConsoleCommand() {
        bindChannelToMinecraftConsoleCommand = SlashCommand.with("bind_to_minecraft_console", "Binds a channel to the minecraft console",
                Collections.singletonList(
                        SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "The channel to bind", true)
                ))
                .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();
        bindChannelToMinecraftConsoleCommand.createSlashCommandUpdater().updateGlobal(api);
    }

    public static void shutdown() {
        if (!checkIfAllowedToRun()) return;
        if (binded_minecraft_chat_webhook != null) {
            binded_minecraft_chat_webhook.delete();
            binded_minecraft_chat_webhook = null;
        }
        api.disconnect();

    }

    private static boolean checkIfAllowedToRun() {
        if (initialized) {
            return true;
        }
        return false;
    }
}
