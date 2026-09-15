package com.vyroflix.common.storage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Auto-configuration for S3-compatible object storage (MinIO / Cloudflare R2 / AWS S3).
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ConditionalOnClass({S3Client.class, S3Presigner.class})
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "vyroflix.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.getEndpoint())
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder()
                .endpointOverride(properties.getEndpoint())
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient objectStorageClient(S3Client s3Client, S3Presigner s3Presigner) {
        return new S3ObjectStorageClient(s3Client, s3Presigner);
    }
}
