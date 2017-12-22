/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.service.cloudwatch.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * CloudWatch client reporter configuration.
 */
@Configuration
@ComponentScan
public class CloudWatchConfiguration {
    private final Logger logger = LoggerFactory.getLogger(CloudWatchConfiguration.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix = "services.cloudwatch.";

    @Value("${services.cloudwatch.dashboardName:API-Gateway-Dashboard}")
    private String dashboardName;

    @Value("${services.cloudwatch.dashboardName:lifecycleQueueName}")
    private String lifecycleQueueName;

    @Value("${services.cloudwatch.enabled:false}")
    private boolean enabled;

    @Value("${services.sync.cron:*/5 * * * * *}")
    private String cronTrigger;

    public String getDashboardName() {
        return dashboardName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCronTrigger() {
        return cronTrigger;
    }

    public String getLifecycleQueueName() {
        return lifecycleQueueName;
    }

    @Bean
    public AmazonCloudWatch cloudWatch() {
        logger.debug("creating AmazonCloudWatch...");

        return AmazonCloudWatchClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration())
                .withCredentials(createAWSCredentials())
                .build();
    }

    @Bean
    public AmazonSQS sqs() {
        logger.debug("creating AmazonSQS...");

        return AmazonSQSClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration())
                .withCredentials(createAWSCredentials())
                .build();
    }

    @Bean
    public AmazonAutoScaling autoScaling() {
        logger.debug("creating AmazonAutoScaling...");

        return AmazonAutoScalingClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration())
                .withCredentials(createAWSCredentials())
                .build();
    }


    private AWSStaticCredentialsProvider createAWSCredentials() {
        logger.debug("createAWSCredentials start");
        AWSStaticCredentialsProvider credentialsProvider = null;

        String accessKeyId = readPropertyValue(propertyPrefix + "awsAccessKeyId");
        String secretKey = readPropertyValue(propertyPrefix + "awsSecretKey");

        if (accessKeyId != null && secretKey != null) {

            logger.debug("Load AWS Credentials from gravitee.yml");
            logger.debug("    aws.access.key.id: {}", accessKeyId);
            logger.debug("    aws.secret.key: {}", secretKey.replaceAll(".*", "#"));

            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretKey);
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);

        } else {
            logger.debug("Load default AWS Credentials");
        }

        logger.debug("createAWSCredentials stop");

        return credentialsProvider;
    }

    public static ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        Map<String, String> env = System.getenv();
        String proxy = env.get("HTTP_PROXY");

        if (proxy != null) {
            String[] split = proxy.split("(?=:[0-9])");

            String host = split[0].replace("http://", "");
            String port = split[1].replace(":", "");

            clientConfiguration.setProxyHost(host);
            clientConfiguration.setProxyPort(Integer.parseInt(port));
        }

        return clientConfiguration;
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }
}
