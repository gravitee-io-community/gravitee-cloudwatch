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
package io.gravitee.reporter.cloudwatch.engine;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class CloudWatchReportEngine implements ReportEngine {
    private final Logger logger = LoggerFactory.getLogger(CloudWatchReportEngine.class);
    private AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.standard().withClientConfiguration(getClientConfiguration()).build();
    private String instanceId = EC2MetadataUtils.getInstanceId();

    private static final String nameSpace = "API Gateway";


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

    @Override
    public void start() {
        logger.info("start");
    }

    @Override
    public void stop() {
        logger.info("stop");
    }

    @Override
    public void report(Reportable reportable) {
        logger.debug("report start");

        if (reportable instanceof Monitor) {
            logger.debug("Send metrics to cloudwatch");


            Dimension dimension = new Dimension().withName("instanceId").withValue(instanceId);

            Monitor monitor = (Monitor) reportable;

            createMetricData(dimension, "JvmHeapUsed", monitor.getJvm().mem.heapUsed);
            createMetricData(dimension, "JvmHeapMax", monitor.getJvm().mem.heapMax);
            createMetricData(dimension, "JvmHTreadsCount", monitor.getJvm().threads.count);
            createMetricData(dimension, "JvmThreadsPeak", monitor.getJvm().threads.peakCount);
            createMetricData(dimension, "OsCpuPercent", monitor.getOs().cpu.percent);
            createMetricData(dimension, "OsMemCpuFree", monitor.getOs().mem.free);
            createMetricData(dimension, "OsMemCpuTotal", monitor.getOs().mem.total);
        }

        logger.debug("report stop");
    }



    private void createMetricData(Dimension dimension, String metricName, long value) {
        logger.debug("createMetricData start for metricName {} and value {}", metricName, value);

        MetricDatum heapUsedInBytes = new MetricDatum()
                .withMetricName(metricName)
                .withUnit(StandardUnit.None)
                .withValue(Double.parseDouble(value + ""))
                .withDimensions(dimension);

        PutMetricDataRequest heapUsedRequest = new PutMetricDataRequest()
                .withNamespace(nameSpace)
                .withMetricData(heapUsedInBytes);

        cw.putMetricData(heapUsedRequest);

        logger.debug("createMetricData stop");
    }


}
