package dev.azn9;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.poll.PollVoteAddEvent;
import discord4j.core.event.domain.poll.PollVoteRemoveEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.poll.Poll;
import discord4j.core.object.entity.poll.PollAnswer;
import discord4j.core.object.entity.poll.PollAnswerCount;
import discord4j.core.object.entity.poll.PollQuestion;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.EmojiData;
import discord4j.discordjson.json.PollAnswerObject;
import discord4j.discordjson.json.PollMediaObject;
import discord4j.gateway.intent.IntentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Main.LOGGER.info("Starting...");

        GatewayDiscordClient client = DiscordClient.builder(args[0])
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.all())
                .login()
                .block();

        if (client == null) {
            throw new IllegalStateException("Could not connect to Discord");
        }

        Main.LOGGER.info("Client connected");

        Snowflake channelId = Snowflake.of(902975768613781534L);
        Snowflake messageId = Snowflake.of(1249842710668513362L);

        client.getMessageById(channelId, messageId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Message not found")))
                .flatMap(message -> {
                    Main.LOGGER.info("Message found: {}", message);

                    Optional<Poll> pollOptional = message.getPoll();

                    if (pollOptional.isPresent()) {
                        Poll poll = pollOptional.get();
                        Main.LOGGER.info("Poll found: {}", poll);

                        Main.LOGGER.info("Poll allows multiple answers: {}", poll.allowMultiselect());
                        Main.LOGGER.info("Poll expires in: {}", poll.getExpiry().map(instant -> instant.minusMillis(Instant.now().toEpochMilli())).map(Instant::getEpochSecond).map(i -> i / 60.f / 60.f).map(d -> d + " hours").orElse("/"));

                        PollQuestion question = poll.getQuestion();

                        Main.LOGGER.info("Poll question title: {}", question.getText().orElse("/"));
                        Main.LOGGER.info("Poll question emoji: {}", question.getEmoji().flatMap(ReactionEmoji::asUnicodeEmoji).map(ReactionEmoji.Unicode::getRaw).orElse("/"));

                        Main.LOGGER.info("Poll answers: {}", poll.getAnswers().size());

                        for (PollAnswer answer : poll.getAnswers()) {
                            Main.LOGGER.info("- Answer: {}", answer.getText().orElse("/"));
                            Main.LOGGER.info("  Emoji: {}", answer.getEmoji().flatMap(ReactionEmoji::asUnicodeEmoji).map(ReactionEmoji.Unicode::getRaw).orElse("/"));
                        }
                    } else {
                        Main.LOGGER.info("No poll found");
                    }

                    return Mono.empty();
                }).block();

        client.on(PollVoteAddEvent.class, event -> {
            Main.LOGGER.info("Poll vote add event received:");
            Main.LOGGER.info("- User ID: {}", event.getUserId().asLong());
            Main.LOGGER.info("- Channel ID: {}", event.getChannelId().asLong());
            Main.LOGGER.info("- Message ID: {}", event.getMessageId().asLong());
            Main.LOGGER.info("- Answer ID: {}", event.getAnswerId());

            return Mono.empty();
        }).subscribe();

        client.on(PollVoteRemoveEvent.class, event -> {
            Main.LOGGER.info("Poll vote remove event received:");
            Main.LOGGER.info("- User ID: {}", event.getUserId().asLong());
            Main.LOGGER.info("- Channel ID: {}", event.getChannelId().asLong());
            Main.LOGGER.info("- Message ID: {}", event.getMessageId().asLong());
            Main.LOGGER.info("- Answer ID: {}", event.getAnswerId());

            return Mono.empty();
        }).subscribe();

        client.on(MessageCreateEvent.class, event -> {
            String content = event.getMessage().getContent();

            if (content.equalsIgnoreCase("!listVoters")) {
                return client.getMessageById(channelId, messageId).flatMap(message -> {
                    return message.getPoll().map(Mono::justOrEmpty).orElse(Mono.empty()).flatMap(poll -> {
                        return Mono.when(Flux.fromIterable(poll.getAnswers()).flatMap(pollAnswer -> {
                            return poll.getVoters(pollAnswer.getAnswerId()).collectList().flatMap(users -> {
                                Main.LOGGER.info("- Voters for answer ID {}: {}", pollAnswer.getAnswerId(), users.stream().map(User::getId).map(Snowflake::asString).collect(Collectors.joining(", ")));

                                return Mono.empty();
                            });
                        }));
                    });
                });
            } else if (content.equalsIgnoreCase("!getCurrentVotes")) {
                return client.getMessageById(channelId, messageId).flatMap(message -> {
                    return message.getPoll().map(Mono::justOrEmpty).orElse(Mono.empty()).flatMap(poll -> {
                        return poll.getLatestResults().flatMap(pollResult -> {
                            Main.LOGGER.info("Current votes:");
                            Main.LOGGER.info("- Finalized? {}", pollResult.isFinalized() ? "Yes" : "No");

                            for (PollAnswerCount pollAnswerCount : pollResult.getAnswerCount()) {
                                Main.LOGGER.info("- Answer ID {}: {}", pollAnswerCount.getAnswerId(), pollAnswerCount.getCount());
                            }

                            return Mono.empty();
                        });
                    });
                });
            } else if (content.equalsIgnoreCase("!endPoll")) {
                return client.getMessageById(channelId, messageId).flatMap(message -> {
                    return message.getPoll().map(Mono::justOrEmpty).orElse(Mono.empty()).flatMap(poll -> {
                        return poll.end().flatMap(pollMessage -> {
                            Main.LOGGER.info("Poll ended: {}", pollMessage);

                            return Mono.empty();
                        }).onErrorResume(throwable -> {
                            Main.LOGGER.error("Could not end poll", throwable);

                            return Mono.empty();
                        });
                    });
                });
            } else if (content.equalsIgnoreCase("!createPoll")) {
                return client.getChannelById(channelId).ofType(MessageChannel.class).flatMap(messageChannel -> {
                    return messageChannel.createPoll()
                            .withQuestion(PollMediaObject.builder()
                                    .text("What is your favorite color?")
                                    .build())
                            .withAnswers(
                                    PollAnswerObject.builder()
                                            .data(PollMediaObject.builder()
                                                    .text("Red")
                                                    .emoji(EmojiData.builder()
                                                            .name("\uD83D\uDD34")
                                                            .build())
                                                    .build())
                                            .build(),
                                    PollAnswerObject.builder()
                                            .data(PollMediaObject.builder()
                                                    .text("Green")
                                                    .emoji(EmojiData.builder()
                                                            .name("\uD83D\uDFE2")
                                                            .build())
                                                    .build())
                                            .build()
                                    )
                            .withAllowMultiselect(true)
                            .withDuration(3) // 3 hours
                            .withLayoutType(Poll.PollLayoutType.DEFAULT.getValue());
                });
            }

            return Mono.empty();
        }).subscribe();

        client.onDisconnect().block();
    }

}

