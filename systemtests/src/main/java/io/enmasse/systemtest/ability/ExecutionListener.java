/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;


public class ExecutionListener implements TestExecutionListener {
    static final Logger log = CustomLogger.getLogger();


    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        Environment env = new Environment();
        if (!env.skipCleanup()) {
            Kubernetes kube = Kubernetes.create(env);
            try {
                AddressApiClient apiClient = new AddressApiClient(kube);
                GlobalLogCollector logCollector = new GlobalLogCollector(kube, new File(env.testLogDir()));
                try {
                    TestUtils.getAddressSpacesObjects(apiClient).forEach((addrSpace) -> {
                        log.info("address space '{}' will be removed", addrSpace);
                        try {
                            TestUtils.deleteAddressSpace(apiClient, addrSpace, logCollector);
                            TestUtils.waitForAddressSpaceDeleted(kube, addrSpace);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                apiClient.close();
            } catch (MalformedURLException e) {
                log.error("AddressApiClient wasn't initialized properly!");
                e.printStackTrace();
            }
        } else {
            log.warn("Remove address spaces when test run finished - SKIPPED!");
        }
        TimeMeasuringSystem.printAndSaveResults();
    }
}
