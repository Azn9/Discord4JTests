package dev.azn9;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.command.ApplicationIntegrationType;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Section;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

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

        DiscordClient discordClient = client.rest();
        discordClient.getApplicationId()
                .flatMap(applicationId -> {
                    return Mono.when(discordClient.getApplicationService()
                            .bulkOverwriteGlobalApplicationCommand(applicationId, List.of(
                                    ApplicationCommandRequest.builder()
                                            .name("test")
                                            .description("test")
                                            .contexts(List.of(
                                                    ApplicationCommandContexts.GUILD.getValue(),
                                                    ApplicationCommandContexts.BOT_DM.getValue(),
                                                    ApplicationCommandContexts.PRIVATE_CHANNEL.getValue()
                                            ))
                                            .integrationTypes(List.of(
//                                                    ApplicationIntegrationType.GUILD_INSTALL.getValue(),
                                                    ApplicationIntegrationType.USER_INSTALL.getValue()
                                            ))
                                            .build()
                            )));
                })
                .subscribe();

        client.on(ChatInputInteractionEvent.class)
                .flatMap(event -> {
                    if (!event.getCommandName().equalsIgnoreCase("test")) {
                        return Mono.empty();
                    }

                    return event.reply()
//                            .withEphemeral(true)
                            .withComponents(
                                    Section.of(
                                            Button.danger("zqsd", "zqsd"),
                                            TextDisplay.of("Test message"),
                                            TextDisplay.of("Test message"),
                                            TextDisplay.of("Test message")
                                    ),
                                    Separator.of(true),
                                    Container.of(
                                            3,
                                            Color.CINNABAR,
                                            TextDisplay.of("Zqsd"),
                                            ActionRow.of(
                                                    Button.danger("erpgoije", "POFKPE")
                                            )
                                    )
                            )
                            .onErrorResume(throwable -> {
                                Main.LOGGER.error(throwable);
                                return Mono.empty();
                            });
                })
                .onErrorResume(throwable -> {
                    Main.LOGGER.error(throwable);
                    return Mono.empty();
                })
                .subscribe();

        client.on(ButtonInteractionEvent.class, event -> {
                    Optional<Message> messageOptional = event.getMessage();
                    if (messageOptional.isEmpty()) {
                        return Mono.empty();
                    }

                    Message message = messageOptional.get();
                    LOGGER.info(message.getData());

                    return message
                            .getComponentById(3)
                            .map(component -> {
                                LOGGER.info("Found component");
                                return component;
                            })
                            .filter(component -> component instanceof Container)
                            .map(component -> (Container) component)
                            .map(container -> {
                                return message
                                        .edit()
                                        .withComponents(
                                                Section.of(
                                                        Button.danger("zqsd", "zqsd"),
                                                        TextDisplay.of("Test message"),
                                                        TextDisplay.of("Test message"),
                                                        TextDisplay.of("Test message")
                                                ),
                                                Separator.of(true),
                                                container.withColor(Color.GREEN)
                                        );
                            })
                            .map(messageEditMono -> messageEditMono.then(event.reply("ok").withEphemeral(true)))
                            .orElse(event.reply("container not found").withEphemeral(true));
                })
                .subscribe();

        client.onDisconnect().block();
    }

}
