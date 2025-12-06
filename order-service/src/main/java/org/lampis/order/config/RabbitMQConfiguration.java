package org.lampis.order.config;

import org.lampis.common.config.RabbitMQConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for order service
 */
@Configuration
public class RabbitMQConfiguration {

    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Order exchange
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(RabbitMQConfig.ORDER_EXCHANGE);
    }

    /**
     * Order created queue
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConfig.ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLQ_EXCHANGE)
                .build();
    }

    /**
     * Order updated queue
     */
    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder.durable(RabbitMQConfig.ORDER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLQ_EXCHANGE)
                .build();
    }

    /**
     * Order status changed queue
     */
    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLQ_EXCHANGE)
                .build();
    }

    /**
     * Order cancelled queue
     */
    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(RabbitMQConfig.ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLQ_EXCHANGE)
                .build();
    }

    /**
     * Dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(RabbitMQConfig.DLQ_QUEUE, true);
    }

    /**
     * Dead letter exchange
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConfig.DLQ_EXCHANGE);
    }

    /**
     * Bindings
     */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderUpdatedBinding() {
        return BindingBuilder.bind(orderUpdatedQueue())
                .to(orderExchange())
                .with(RabbitMQConfig.ORDER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusChangedBinding() {
        return BindingBuilder.bind(orderStatusChangedQueue())
                .to(orderExchange())
                .with(RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue())
                .to(orderExchange())
                .with(RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMQConfig.DLQ_ROUTING_KEY);
    }
}