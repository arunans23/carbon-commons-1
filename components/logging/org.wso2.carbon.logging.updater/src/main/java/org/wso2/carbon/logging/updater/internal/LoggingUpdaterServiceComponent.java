/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */


package org.wso2.carbon.logging.updater.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.wso2.carbon.logging.updater.LogConfigUpdater;
import org.wso2.carbon.logging.updater.LoggingUpdaterConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(name = "org.wso2.carbon.logging.updater", immediate = true, property = EventConstants.EVENT_TOPIC
        + "=" + LoggingUpdaterConstants.PAX_LOGGING_CONFIGURATION_TOPIC)
public class LoggingUpdaterServiceComponent implements EventHandler {

    final static Log log = LogFactory.getLog(LoggingUpdaterServiceComponent.class);
    private ConfigurationAdmin configurationAdmin;

    @Reference(name = "osgi.configadmin.service",
            service = ConfigurationAdmin.class,
            unbind = "unsetConfigAdminService",
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC)
    public void setConfigAdminService(ConfigurationAdmin configAdminService) {

        ServiceReferenceHolder.getInstance().setConfigurationAdmin(configAdminService);
    }

    @Activate
    public void activate(ComponentContext bundleContext) {

        LogConfigUpdater logConfigUpdater =
                new LogConfigUpdater(ServiceReferenceHolder.getInstance().getConfigurationAdmin());
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        ServiceReferenceHolder.getInstance().setScheduledExecutorService(scheduledExecutorService);
        scheduledExecutorService.schedule(logConfigUpdater, 5000L, TimeUnit.MILLISECONDS);
    }

    @Deactivate
    public void deactivate() {

        ServiceReferenceHolder.getInstance().getScheduledExecutorService().shutdown();
    }

    public void unsetConfigAdminService(ConfigurationAdmin configurationAdmin) {

        ServiceReferenceHolder.getInstance().setConfigurationAdmin(null);
    }

    @Override
    public void handleEvent(Event event) {

        if (event.containsProperty(LoggingUpdaterConstants.EXCEPTIONS_PROPERTY)) {
            Object property = event.getProperty(LoggingUpdaterConstants.EXCEPTIONS_PROPERTY);
            Exception exception = (Exception) property;
            log.error("Logging Configuration issue " + exception.getMessage());
            log.error("Using the previous successful configuration to setup the logs");
        } else {
            log.info("Logging configuration applied successfully");
        }
    }
}
