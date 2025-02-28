package dev.azn9;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.intent.IntentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(args[0])
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.all())
                .login()
                .block();

        if (client == null) {
            Main.LOGGER.error("Failed to login to Discord.");
            return;
        }

        Main.LOGGER.info("Successfully logged in to Discord.");

        // TODO

        client.onDisconnect().block();
    }

}
