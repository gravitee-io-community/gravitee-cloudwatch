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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.GetDashboardRequest;
import com.amazonaws.services.cloudwatch.model.GetDashboardResult;
import io.gravitee.service.cloudwatch.config.CloudWatchConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class CloudwatchDashboardServiceTest {
    @Mock
    private AmazonCloudWatch cw;
    @Mock
    private CloudWatchConfiguration configuration;
    @InjectMocks
    private CloudwatchDashboardService service;


    @Test
    public void testCreateDashboard() throws Exception {
        Mockito.when(configuration.getDashboardName()).thenReturn("test-dashboard");
        Mockito.when(configuration.isEnabled()).thenReturn(true);

        Mockito.when(cw.getDashboard(any(GetDashboardRequest.class))).thenReturn(createDasboardResult());
        setFinalStatic(CloudwatchDashboardService.class.getDeclaredField("instanceId"), "instanceId-NOT_EXISTENT");

        service.createDashboard();

        Mockito.verify(cw).putDashboard(any());

    }

    @Test
    public void testOnStop() throws Exception {
        Mockito.when(configuration.getDashboardName()).thenReturn("test-dashboard");
        Mockito.when(configuration.isEnabled()).thenReturn(true);
        Mockito.when(cw.getDashboard(any(GetDashboardRequest.class))).thenReturn(createDasboardResult());
        setFinalStatic(CloudwatchDashboardService.class.getDeclaredField("instanceId"), "i-023cf1c0d42af8215");

        service.removeWidgets("i-023cf1c0d42af8215");

        Mockito.verify(cw).putDashboard(any());
    }

    @Test
    public void createWidgets() throws IOException {
        List<CloudwatchDashboardService.Widget> widgets = service.createWidgets();
        assertThat(widgets.size(), is(4));
        assertThat(widgets.get(0).properties.metrics.get(0).get(1), is("OsCpuPercent"));
    }


    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    private GetDashboardResult createDasboardResult() {
        GetDashboardResult getDashboardResult = new GetDashboardResult();
        getDashboardResult.setDashboardBody(getJsonBody());

        return getDashboardResult;
    }

    public String getJsonBody() {
        return "{\n" +
                "    \"widgets\": [\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 0,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"CPUUtilization\", \"AutoScalingGroupName\", \"APIM-dev-APIGateway-1S89B47WOC06U-ServerScalingGroup-1LDW2YLDD35TX\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"yAxis\": {\n" +
                "                    \"left\": {\n" +
                "                        \"min\": 0,\n" +
                "                        \"max\": 100\n" +
                "                    }\n" +
                "                },\n" +
                "                \"title\": \"API Gateway\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 6,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"CPUUtilization\", \"AutoScalingGroupName\", \"APIM-dev-MgmtAPI-1OBQ25OD436QO-ServerScalingGroup-F31QZQ8KP0ED\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"yAxis\": {\n" +
                "                    \"left\": {\n" +
                "                        \"min\": 0,\n" +
                "                        \"max\": 100\n" +
                "                    }\n" +
                "                },\n" +
                "                \"title\": \"Management API\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 12,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"CPUUtilization\", \"AutoScalingGroupName\", \"APIM-dev-ElasticSearch-35VHZDNLUEJ0-ServerScalingGroup-VWV44D2H75RU\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"Elastic Search\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 18,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"CPUUtilization\", \"AutoScalingGroupName\", \"APIM-dev-ProxyServer-1Y0M12V7IH86H-ServerScalingGroup-11TAPAIAJEB4A\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"Proxy Server\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 0,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"DiskReadBytes\", \"AutoScalingGroupName\", \"APIM-dev-APIGateway-1S89B47WOC06U-ServerScalingGroup-1LDW2YLDD35TX\" ],\n" +
                "                    [ \".\", \"DiskWriteBytes\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"API Gateway\",\n" +
                "                \"period\": 300\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 12,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"DiskReadBytes\", \"AutoScalingGroupName\", \"APIM-dev-ElasticSearch-35VHZDNLUEJ0-ServerScalingGroup-VWV44D2H75RU\" ],\n" +
                "                    [ \".\", \"DiskWriteBytes\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"Elastic Search\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 6,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"DiskReadBytes\", \"AutoScalingGroupName\", \"APIM-dev-MgmtAPI-1OBQ25OD436QO-ServerScalingGroup-F31QZQ8KP0ED\" ],\n" +
                "                    [ \".\", \"DiskWriteBytes\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"Management API\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 18,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/EC2\", \"DiskReadBytes\", \"AutoScalingGroupName\", \"APIM-dev-ProxyServer-1Y0M12V7IH86H-ServerScalingGroup-11TAPAIAJEB4A\" ],\n" +
                "                    [ \".\", \"DiskWriteBytes\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"period\": 300,\n" +
                "                \"title\": \"Proxy Server\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 24,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/DynamoDB\", \"ReturnedItemCount\", \"TableName\", \"GraviteeioApimApi\", \"Operation\", \"Scan\" ],\n" +
                "                    [ \"...\", \"Query\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"TableOps GraviteeioApimApi\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 24,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/DynamoDB\", \"SuccessfulRequestLatency\", \"TableName\", \"GraviteeioApimEvent\", \"Operation\", \"GetItem\" ],\n" +
                "                    [ \"...\", \"UpdateItem\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"TableOps GraviteeioApimEvent\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 30,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/ELB\", \"HealthyHostCount\", \"AvailabilityZone\", \"eu-central-1a\" ],\n" +
                "                    [ \".\", \"RequestCount\", \".\", \".\", { \"stat\": \"Sum\" } ],\n" +
                "                    [ \".\", \"UnHealthyHostCount\", \".\", \".\", { \"stat\": \"Sum\" } ],\n" +
                "                    [ \".\", \"HealthyHostCount\", \".\", \"eu-central-1b\" ],\n" +
                "                    [ \".\", \"RequestCount\", \".\", \".\", { \"stat\": \"Sum\" } ],\n" +
                "                    [ \".\", \"UnHealthyHostCount\", \".\", \".\", { \"stat\": \"Sum\" } ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"ELB\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 6,\n" +
                "            \"y\": 30,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"AWS/ElastiCache\", \"CPUUtilization\", \"CacheClusterId\", \"apimdev\", \"CacheNodeId\", \"0001\" ],\n" +
                "                    [ \".\", \"CacheMisses\", \".\", \".\", \".\", \".\" ],\n" +
                "                    [ \".\", \"CacheHits\", \".\", \".\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"ElastiCache\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 12,\n" +
                "            \"y\": 0,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"System/Linux\", \"MemoryUtilization\", \"AutoScalingGroupName\", \"APIM-dev-APIGateway-1S89B47WOC06U-ServerScalingGroup-1LDW2YLDD35TX\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"period\": 300,\n" +
                "                \"title\": \"API Gateway Memory %\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 18,\n" +
                "            \"y\": 0,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"System/Linux\", \"MemoryUsed\", \"AutoScalingGroupName\", \"APIM-dev-APIGateway-1S89B47WOC06U-ServerScalingGroup-1LDW2YLDD35TX\" ],\n" +
                "                    [ \"System/Linux\", \"MemoryAvailable\", \"AutoScalingGroupName\", \"APIM-dev-APIGateway-1S89B47WOC06U-ServerScalingGroup-1LDW2YLDD35TX\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"period\": 300,\n" +
                "                \"title\": \"API Gateway Memory MB\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 12,\n" +
                "            \"y\": 6,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"heapUsed\", \"instanceId\", \"i-023cf1c0d42af8215\" ],\n" +
                "                    [ \".\", \"heapMax\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"API GW i-023cf1c0d42af8215\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 12,\n" +
                "            \"y\": 12,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"heapUsed\", \"instanceId\", \"i-023cf1c0d42af8215\" ],\n" +
                "                    [ \".\", \"heapMax\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"API GW i-023cf1c0d42af8215\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 12,\n" +
                "            \"y\": 18,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"heapUsed\", \"instanceId\", \"i-023cf1c0d42af8215\" ],\n" +
                "                    [ \".\", \"heapMax\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"API GW i-023cf1c0d42af8215\"\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"metric\",\n" +
                "            \"x\": 12,\n" +
                "            \"y\": 24,\n" +
                "            \"width\": 6,\n" +
                "            \"height\": 6,\n" +
                "            \"properties\": {\n" +
                "                \"view\": \"timeSeries\",\n" +
                "                \"stacked\": false,\n" +
                "                \"metrics\": [\n" +
                "                    [ \"API Gateway\", \"heapUsed\", \"instanceId\", \"i-023cf1c0d42af8215\" ],\n" +
                "                    [ \".\", \"heapMax\", \".\", \".\" ]\n" +
                "                ],\n" +
                "                \"region\": \"eu-central-1\",\n" +
                "                \"title\": \"API GW i-023cf1c0d42af8215\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";

    }

}