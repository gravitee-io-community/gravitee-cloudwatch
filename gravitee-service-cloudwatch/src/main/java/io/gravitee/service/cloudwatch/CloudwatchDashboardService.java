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
package io.gravitee.service.cloudwatch;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.GetDashboardRequest;
import com.amazonaws.services.cloudwatch.model.GetDashboardResult;
import com.amazonaws.services.cloudwatch.model.PutDashboardRequest;
import com.amazonaws.services.cloudwatch.model.ResourceNotFoundException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.service.cloudwatch.config.CloudWatchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CloudwatchDashboardService extends AbstractService implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(CloudwatchDashboardService.class);
    private AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.standard().withClientConfiguration(getClientConfiguration()).build();
    private final AmazonSQS sqs = AmazonSQSClientBuilder.standard().withClientConfiguration(getClientConfiguration()).build();
    private final AmazonAutoScaling scaling = AmazonAutoScalingClientBuilder.standard().withClientConfiguration(getClientConfiguration()).build();
    private static ObjectMapper mapper = new ObjectMapper();
    private static String instanceId = EC2MetadataUtils.getInstanceId();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    @Autowired
    private CloudWatchConfiguration configuration;

    @Autowired
    private TaskScheduler scheduler;


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

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected void doStart() throws Exception {
        logger.info("doStart start with configuration {}",configuration.isEnabled());

        if (configuration.isEnabled()) {
            super.doStart();
            logger.info("Sync service has been initialized with cron [{}]", configuration.getCronTrigger());
            scheduler.schedule(this, new CronTrigger(configuration.getCronTrigger()));
            createDashboard();
        }
    }

    @Override
    public void run() {
        pollForMessages();
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    protected String name() {
        return "CloudwatchDashboard Service";
    }

    private void pollForMessages() {
        logger.debug("pollForMessages start with configuration {}",configuration.isEnabled());

        if (configuration.isEnabled()) {
            ListQueuesResult listQueuesResult = sqs.listQueues();

            logger.debug("listQueuesResult {}", listQueuesResult);

            Optional<String> instanceTerminatingQueue = listQueuesResult.getQueueUrls().stream().filter(p -> p.contains("InstanceTerminatingQueue")).findFirst();
            if (instanceTerminatingQueue.isPresent()) {
                ReceiveMessageRequest receive_request = new ReceiveMessageRequest()
                        .withQueueUrl(instanceTerminatingQueue.get())
                        .withWaitTimeSeconds(5);
                ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receive_request);

                logger.debug("receiveMessageResult {}", receiveMessageResult);

                receiveMessageResult.getMessages().forEach(p -> {

                    try {
                        Notification notification = mapper.readValue(p.getBody(), Notification.class);
                        String message = notification.Message;
                        message.replace('\"', '"');
                        Content content = mapper.readValue(message, Content.class);

                        if ("autoscaling:EC2_INSTANCE_TERMINATING".equals(content.LifecycleTransition)) {
                            removeWidgets(content.EC2InstanceId);
                        }

                        logger.debug("deleting message from queue");
                        sqs.deleteMessage(instanceTerminatingQueue.get(), p.getReceiptHandle());

                        logger.debug("sending complete action to autoscaling AWS for CONTINUE removing instance from the scaling group");

                        CompleteLifecycleActionRequest request = new CompleteLifecycleActionRequest().withLifecycleHookName(content.LifecycleHookName)
                                .withAutoScalingGroupName(content.AutoScalingGroupName).withLifecycleActionToken(content.LifecycleActionToken)
                                .withLifecycleActionResult("CONTINUE");
                        CompleteLifecycleActionResult response = scaling.completeLifecycleAction(request);

                        logger.debug("response {}", response);

                    } catch (IOException e) {
                        logger.error("Something went wrong deserializing the message", e);
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    protected void createDashboard() {
        String dashboardName = configuration.getDashboardName();
        logger.debug("start creating or updating dashboard with dashboardName {}", dashboardName);
        GetDashboardResult dashboard = new GetDashboardResult().withDashboardBody("{}");

        try {
            dashboard = cw.getDashboard(new GetDashboardRequest().withDashboardName(dashboardName));
        } catch (ResourceNotFoundException e) {
            logger.debug("unable to find dashboard with name dashboardName {}, creating new one", dashboardName);
        }

        try {
            String dashboardBody = dashboard.getDashboardBody();

            if (!dashboardBody.contains(instanceId)) {
                logger.debug("dashboard does not yet contains widgets for instanceId {}, adding them", instanceId);
                Widgets widgets = mapper.readValue(dashboardBody, Widgets.class);

                List<Widget> widgetList = createWidgets();
                widgets.widgets.addAll(widgetList);

                cw.putDashboard(new PutDashboardRequest().withDashboardName(dashboardName).withDashboardBody(mapper.writeValueAsString(widgets)));
            }
        } catch (Exception e) {
            logger.error("Unable to add widgets for instanceId {}", instanceId, e);
        }

        logger.debug("stop creating/updating dashboard");
    }

    protected void removeWidgets(String instanceId) {

        String dashboardName = configuration.getDashboardName();
        logger.debug("removing widgets for this instanceId {} if any for dashboardName {}", instanceId, dashboardName);

        try {
            GetDashboardResult dashboard = cw.getDashboard(new GetDashboardRequest().withDashboardName(dashboardName));
            String dashboardBody = dashboard.getDashboardBody();

            if (dashboardBody.contains(instanceId)) {
                logger.debug("dashboard contains widgets for instanceId {}, removing them", instanceId);

                Widgets widgets = mapper.readValue(dashboardBody, Widgets.class);

                logger.debug("Actual widgets number before removing them for this instanceId {}", widgets.widgets.size());
                widgets.widgets.removeIf(p -> p.properties.metrics.get(0).get(3).equals(instanceId));

                logger.debug("Actual widgets number after removing  them for this instanceId {}", widgets.widgets.size());

                cw.putDashboard(new PutDashboardRequest().withDashboardName(dashboardName).withDashboardBody(mapper.writeValueAsString(widgets)));
            }
        } catch (ResourceNotFoundException e) {
            logger.warn("unable to find dashboard with name dashboardName {}, pay attention to the name in the gravitee.yaml file under ", dashboardName);
        } catch (Exception e) {
            logger.error("Unable to remove widgets for instanceId {}", instanceId, e);
        }
        logger.debug("removing widgets stop ");
    }


    protected List<Widget> createWidgets() throws IOException {
        logger.debug("createWidgets start");

        List<Widget> widgets = new ArrayList<>();

        widgets.add(createWidgetSingleMetric(0, 0, "OsCpuPercent"));
        widgets.add(createWidgetDoubleMetrics(6, 0, "OsMemCpuFree", "OsMemCpuTotal"));
        widgets.add(createWidgetDoubleMetrics(12, 0, "JvmHeapMax", "JvmHeapUsed"));
        widgets.add(createWidgetDoubleMetrics(18, 0, "JvmHTreadsCount", "JvmThreadsPeak"));

        logger.debug("createWidgets stop");

        return widgets;

    }

    private Widget createWidgetSingleMetric(int x, int y, String metric1) throws IOException {
        logger.debug("createWidgetSingleMetric start");

        String jsonWidget = "{\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": " + x + ",\n" +
                "            \"y\": " + y + ",\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"" + metric1 + "\", \"instanceId\" ]\n" +
                "                ],\n" +
                "                \"region\": \"\"\n" +
                "            }\n" +
                "        }";

        logger.debug("createWidgetSingleMetric stop");

        return createWidgetFromTemplate(jsonWidget);
    }

    private Widget createWidgetDoubleMetrics(int x, int y, String metric1, String metric2) throws IOException {
        logger.debug("createWidgetDoubleMetrics stop");

        String jsonWidget = "{\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": " + x + ",\n" +
                "            \"y\": " + y + ",\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"" + metric1 + "\", \"instanceId\" ],\n" +
                "                    [ \".\", \"" + metric2 + "\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"\"\n" +
                "            }\n" +
                "        }";

        logger.debug("createWidgetDoubleMetrics stop");

        return createWidgetFromTemplate(jsonWidget);
    }

    private Widget createWidgetFromTemplate(String jsonWidget) throws IOException {
        logger.debug("createWidgetFromTemplate start");

        Region currentRegion = Optional.ofNullable(Regions.getCurrentRegion()).orElse(Region.getRegion(Regions.EU_CENTRAL_1));

        Widget widget = mapper.readValue(jsonWidget, Widget.class);
        widget.properties.metrics.forEach(p -> p.add(3, instanceId));
        widget.properties.region = currentRegion.getName();
        widget.properties.title = "API GW " + instanceId;

        logger.debug("createWidgetFromTemplate stop");

        return widget;
    }


    static class Widgets {
        public List<Widget> widgets = new ArrayList<>();

        @Override
        public String toString() {
            return "Widgets{" +
                    "widgets=" + widgets +
                    '}';
        }
    }

    static class Widget {
        public String type = "metric";
        public int x;
        public int y;
        public int width = 6;
        public int height = 6;
        public Properties properties = new Properties();

        @Override
        public String toString() {
            return "Widget{" +
                    "type='" + type + '\'' +
                    ", x='" + x + '\'' +
                    ", y='" + y + '\'' +
                    ", width='" + width + '\'' +
                    ", height='" + height + '\'' +
                    ", properties=" + properties +
                    '}';
        }
    }

    static class Properties {
        public String view = "timeSeries";
        public boolean stacked = false;
        public List<List<Object>> metrics = new ArrayList();
        public String region;
        public String title;

        @Override
        public String toString() {
            return "Properties{" +
                    "view='" + view + '\'' +
                    ", stacked=" + stacked +
                    ", metrics=" + metrics +
                    ", region='" + region + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    static class Notification {
        public String Message;

    }

    static class Content {
        public String EC2InstanceId;
        public String LifecycleHookName;
        public String AutoScalingGroupName;
        public String LifecycleActionToken;
        public String LifecycleTransition;
    }
}
