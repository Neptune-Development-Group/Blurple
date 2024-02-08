package com.neptunedevelopmentteam.blurple;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Arrays;
import java.util.Collections;

public class BlurpleSlashCommands {

    public static SlashCommand BIND_COMMAND;
    public static void init(DiscordApi api) {
        Blurple.LOGGER.info("Setting up slash commands");
        setupBindCommand(api);
        api.addSlashCommandCreateListener(BlurpleSlashCommands::handleSlashCommands);
        Blurple.LOGGER.info("Slash commands have been initialized");
    }

    public static void stop() {
        BIND_COMMAND.delete().join();
        BIND_COMMAND = null;
    }

    public static void setupBindCommand(DiscordApi api) {
        Blurple.LOGGER.info("Setting up bind slash command");
        BIND_COMMAND = SlashCommand.with("bind", "Binds a channel to a specificed action",
                Arrays.asList(
                        SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "chat", "Binds a channel to the minecraft chat",
                                Collections.singletonList(SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "the channel to bind the minecraft chat to", true))),
                        SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "console", "Binds a channel to the minecraft chat",
                                Collections.singletonList(SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "the channel to bind the minecraft console to", true)))
                )).createGlobal(api).join();
        Blurple.LOGGER.info("Bind slash command initialized");
    }

    public static void handleSlashCommands(SlashCommandCreateEvent event) {
        if (event.getSlashCommandInteractionWithCommandId(BIND_COMMAND.getId()).isPresent()) {
            handleBindCommand(event.getSlashCommandInteractionWithCommandId(BIND_COMMAND.getId()).get());
        }
    }

    public static void handleBindCommand(SlashCommandInteraction interaction) {
        Blurple.LOGGER.info(interaction.getFullCommandName());
    }

    public static void eventHandler(SlashCommandCreateEvent event) {
        SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();

    }
}
