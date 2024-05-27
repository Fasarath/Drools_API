package org.example.service;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
public class DroolsService {

    private final KieContainer kieContainer;

    public DroolsService() {
        KieServices kieServices = KieServices.Factory.get();
        this.kieContainer = kieServices.getKieClasspathContainer();
    }

    public void executeRules(Object object) {
        KieSession kieSession = kieContainer.newKieSession();
        kieSession.insert(object);
        kieSession.fireAllRules();
        kieSession.dispose();
    }
}
