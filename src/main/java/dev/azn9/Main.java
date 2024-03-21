package dev.azn9;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.command.ApplicationIntegrationType;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.gateway.intent.IntentSet;
import reactor.core.publisher.Mono;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(args[0])
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.nonPrivileged())
                .login()
                .block();

        if (client == null) {
            System.exit(1);
            return;
        }

        // Register commands
        var applicationService = client.rest().getApplicationService();

        applicationService.getCurrentApplicationInfo().map(ApplicationInfoData::id).flatMap(appId -> {
            return Mono.when(applicationService.bulkOverwriteGlobalApplicationCommand(appId.asLong(), List.of(
                    ApplicationCommandRequest.builder()
                            .name("whereami")
                            .description("Get information about the current context")
                            .contexts(List.of(ApplicationCommandContexts.BOT_DM.getValue()))
                            .integrationTypes(List.of(ApplicationIntegrationType.USER_INSTALL.getValue()))
                            .build(),
                    ApplicationCommandRequest.builder()
                            .name("onlyonguildsbyusers")
                            .description("Get information about the current context")
                            .contexts(List.of(ApplicationCommandContexts.GUILD.getValue()))
                            .integrationTypes(List.of(ApplicationIntegrationType.USER_INSTALL.getValue()))
                            .build(),
                    ApplicationCommandRequest.builder()
                            .name("onlyonbotdm")
                            .description("Get information about the current context")
                            .contexts(List.of(ApplicationCommandContexts.BOT_DM.getValue()))
                            .integrationTypes(List.of(ApplicationIntegrationType.USER_INSTALL.getValue()))
                            .build(),
                    ApplicationCommandRequest.builder()
                            .name("onlyongroupdm")
                            .description("Get information about the current context")
                            .contexts(List.of(ApplicationCommandContexts.PRIVATE_CHANNEL.getValue()))
                            .integrationTypes(List.of(ApplicationIntegrationType.USER_INSTALL.getValue()))
                            .build()
            )));
        }).block();

        client.on(ChatInputInteractionEvent.class, event -> {
            String context = event.getInteraction().getContext().map(ctx -> "`" + ctx.name() + "`").orElse("`No context`");
            String integrationOwners = event.getInteraction().getAuthorizingIntegrationOwners().toString();

            return event.reply("You are in " + context + " and the integration owners are " + integrationOwners).withEphemeral(true);
        }).subscribe();

        client.onDisconnect().block();
    }

}
