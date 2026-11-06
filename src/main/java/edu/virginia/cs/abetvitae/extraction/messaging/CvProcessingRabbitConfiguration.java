package edu.virginia.cs.abetvitae.extraction.messaging;

import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.aopalliance.intercept.MethodInterceptor;

@Configuration
@EnableRabbit
@EnableScheduling
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
public class CvProcessingRabbitConfiguration {

    @Bean
    Declarables cvProcessingTopology() {
        DirectExchange exchange = new DirectExchange(
                CvProcessingQueues.EXCHANGE,
                true,
                false
        );
        DirectExchange deadLetterExchange = new DirectExchange(
                CvProcessingQueues.DEAD_LETTER_EXCHANGE,
                true,
                false
        );
        Queue processingQueue = QueueBuilder
                .durable(CvProcessingQueues.QUEUE)
                .quorum()
                .deadLetterExchange(CvProcessingQueues.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(
                        CvProcessingQueues.DEAD_LETTER_ROUTING_KEY
                )
                .build();
        Queue deadLetterQueue = QueueBuilder
                .durable(CvProcessingQueues.DEAD_LETTER_QUEUE)
                .quorum()
                .build();
        Binding processingBinding = BindingBuilder.bind(processingQueue)
                .to(exchange)
                .with(CvProcessingQueues.ROUTING_KEY);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(CvProcessingQueues.DEAD_LETTER_ROUTING_KEY);
        List<Declarable> declarables = List.of(
                exchange,
                deadLetterExchange,
                processingQueue,
                deadLetterQueue,
                processingBinding,
                deadLetterBinding
        );
        return new Declarables(declarables);
    }

    @Bean
    MethodInterceptor cvProcessingRetryInterceptor(
            CvProcessingProperties properties,
            MessageRecoverer cvProcessingDeadLetterRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(properties.maximumAttempts() - 1L)
                        .includes(CvProcessingRetryableException.class)
                        .delay(properties.initialRetryInterval())
                        .multiplier(properties.retryMultiplier())
                        .maxDelay(properties.maximumRetryInterval()))
                .recoverer(cvProcessingDeadLetterRecoverer)
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory cvProcessingListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MethodInterceptor cvProcessingRetryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(cvProcessingRetryInterceptor);
        return factory;
    }
}
