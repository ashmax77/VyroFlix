package com.vyroflix.platform.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entities and repositories of every merged domain.
 *
 * <p>Only applies when JPA is actually set up. Tests that exclude JPA, and deployments without a database, get no
 * repository scan, and without this condition the repository scan would fail for want of an EntityManagerFactory.
 * Registered as an auto-configuration so it is processed after Hibernate, the same as the outbox configuration.</p>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan(basePackages = "com.vyroflix")
@EnableJpaRepositories(basePackages = "com.vyroflix")
public class PlatformJpaConfig {
}
