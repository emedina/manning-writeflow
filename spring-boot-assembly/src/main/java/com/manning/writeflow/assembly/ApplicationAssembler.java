package com.manning.writeflow.assembly;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.domain.service.annotation.DomainService;
import com.manning.writeflow.outboxrelay.OutboxRelayScheduling;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Assembles the application by wiring the hexagonal-architecture beans.
 *
 * <p>Component scanning is restricted to the Architecture Kernel stereotypes so that only
 * application services, adapters and domain services are picked up as beans, keeping the inner layers free of framework
 * annotations.</p>
 *
 * @author Enrique Medina Montenegro
 */
@EnableTransactionManagement
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = "com.manning.writeflow",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = { Component.class, Service.class }
    ),
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = { ApplicationService.class, Adapter.class, DomainService.class }
    )
)
@Import(OutboxRelayScheduling.class)
class ApplicationAssembler {

}
