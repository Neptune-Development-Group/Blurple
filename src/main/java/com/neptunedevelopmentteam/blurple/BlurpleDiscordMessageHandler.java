package com.neptunedevelopmentteam.blurple;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

public class BlurpleDiscordMessageHandler {

    public static void onMessageCreateEvent(MessageCreateEvent event) {
        MutableText text = Text.empty();
        String message_string = event.getMessage().getContent();
        int current_pos = 0;
        boolean hasMentions = doesMessageHaveMentions(event.getMessage());
        for (int i = 0; i < message_string.length(); i++) {

        }
    }

    private static void parseMention(MutableText text, String message_string, int current_pos, Message message) {

    }

    private static boolean doesMessageHaveMentions(Message message) {
        return (!message.getMentionedUsers().isEmpty() || !message.getMentionedRoles().isEmpty() || !message.getMentionedChannels().isEmpty());
    }
}
