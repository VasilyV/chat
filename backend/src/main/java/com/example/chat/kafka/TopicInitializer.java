package com.example.chat.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TopicInitializer {

    public TopicInitializer(AdminClient admin) {
        List<NewTopic> topics = List.of(
                new NewTopic("chat-messages", 10, (short) 1),
                new NewTopic("chat-messages-dlq", 10, (short) 1)
        );

        admin.createTopics(topics);
    }
}

