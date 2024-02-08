package com.neptunedevelopmentteam.blurple;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blurple implements ModInitializer {

    public static final BlurpleConfig CONFIG = new BlurpleConfig();
    public static Logger LOGGER = LoggerFactory.getLogger("blurple");
    @Override
    public void onInitialize() {
        CONFIG.init("blurple");
        ServerLifecycleEvents.SERVER_STARTING.register(DiscordIntegration::init);
    }
}
