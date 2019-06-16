package com.github.burkaa01.stream.config.tracing;

import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.streams.TracingKafkaClientSupplier;
import io.opentracing.util.GlobalTracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.StreamsBuilderFactoryBean;

import javax.annotation.PostConstruct;

@Configuration
public class TracingConfig {

    @Value("${topics.jaeger-topic}")
    private String tracingTopic;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final StreamsBuilderFactoryBean streamsBuilderFactory;

    public TracingConfig(StreamsBuilderFactoryBean streamsBuilderFactory) {
        this.streamsBuilderFactory = streamsBuilderFactory;
    }

    @Bean
    public Tracer tracer() {
        return io.jaegertracing.Configuration.fromEnv(applicationName)
                .withSampler(
                        io.jaegertracing.Configuration.SamplerConfiguration.fromEnv()
                                .withType(ConstSampler.TYPE)
                                .withParam(1))
                .withReporter(
                        io.jaegertracing.Configuration.ReporterConfiguration.fromEnv()
                                .withLogSpans(true)
                                .withFlushInterval(1000)
                                .withMaxQueueSize(10000)
                                .withSender(
                                        new KafkaSenderConfiguration(bootstrapServers, tracingTopic)
                                ))
                .getTracer();
    }

    @PostConstruct
    public void registerToGlobalTracer() {
        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(tracer());
        }
    }

    @PostConstruct
    public void setClientSupplierForStreams() {
        streamsBuilderFactory.setClientSupplier(new TracingKafkaClientSupplier(tracer()));
    }
}
