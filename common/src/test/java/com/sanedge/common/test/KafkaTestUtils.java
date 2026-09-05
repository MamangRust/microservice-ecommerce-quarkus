package com.sanedge.common.test;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Blocking helpers for integration tests that need to produce to / consume from
 * a real Kafka broker. These block the calling thread, so invoke them from a
 * worker thread (e.g. {@code .runSubscriptionOn(worker)}) inside reactive tests.
 */
public final class KafkaTestUtils {

    private KafkaTestUtils() {
    }

    public static void produce(String bootstrapServers, String topic, String key, String value) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
            producer.flush();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to produce Kafka record to " + topic, e);
        }
    }

    /**
     * Polls the topic with a fresh consumer group until a record matches the
     * predicate or the timeout elapses. Returns the first match or {@code null}.
     */
    public static ConsumerRecord<String, String> awaitRecord(String bootstrapServers, String topic,
            Predicate<ConsumerRecord<String, String>> predicate, Duration timeout) {
        Properties props = consumerProps(bootstrapServers);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (predicate.test(record)) {
                        return record;
                    }
                }
            }
        }
        return null;
    }

    /** Counts records matching the predicate within the given timeout. */
    public static long countMatching(String bootstrapServers, String topic,
            Predicate<ConsumerRecord<String, String>> predicate, Duration timeout) {
        Properties props = consumerProps(bootstrapServers);
        long matches = 0;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
                for (ConsumerRecord<String, String> record : records) {
                    if (predicate.test(record)) {
                        matches++;
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Reads the committed offset for the given consumer group on the topic.
     * Returns the first partition offset found, or -1 when the group has no
     * committed offset for the topic yet.
     */
    public static long committedOffset(String bootstrapServers, String group, String topic, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                try {
                    var offsets = admin.listConsumerGroupOffsets(group)
                            .partitionsToOffsetAndMetadata()
                            .get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                    for (var entry : offsets.entrySet()) {
                        if (entry.getKey().topic().equals(topic) && entry.getValue() != null) {
                            return entry.getValue().offset();
                        }
                    }
                } catch (Exception ignored) {
                    // group may not exist yet; retry until timeout
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
        }
        return -1;
    }

    private static Properties consumerProps(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-test-utils-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return props;
    }
}
