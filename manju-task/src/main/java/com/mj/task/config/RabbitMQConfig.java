package com.mj.task.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 漫剧任务消息队列
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机和队列名称 ==========
    public static final String TASK_EXCHANGE = "comic.task.exchange";
    public static final String DIRECTOR_QUEUE = "comic.task.director.queue";
    public static final String DIRECTOR_ROUTING_KEY = "comic.task.director";

    /**
     * 创建Direct交换机
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }

    /**
     * 创建Director队列
     */
    @Bean
    public Queue directorQueue() {
        return QueueBuilder.durable(DIRECTOR_QUEUE).build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding directorBinding() {
        return BindingBuilder.bind(directorQueue()).to(taskExchange()).with(DIRECTOR_ROUTING_KEY);
    }

    /**
     * JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}