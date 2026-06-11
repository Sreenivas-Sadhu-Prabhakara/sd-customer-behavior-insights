package com.bank.bian.customerbehaviorinsights.api;

import com.bank.bian.customerbehaviorinsights.model.ControlRecord;
import com.bank.bian.customerbehaviorinsights.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Customer Behavior Insights" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/customer-behavior-model-analysis/initiate                    → Initiate a control record
 *   GET  /v1/customer-behavior-model-analysis                             → Retrieve (list)
 *   GET  /v1/customer-behavior-model-analysis/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/customer-behavior-model-analysis/{crId}/update               → Update
 *   PUT  /v1/customer-behavior-model-analysis/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Customer Behavior Insights",
                "businessArea", "Sales and Service",
                "businessDomain", "Customer Management",
                "functionalPattern", "Analyze",
                "assetType", "Customer Behavior Model",
                "controlRecord", "Customer Behavior Model Analysis",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/customer-behavior-model-analysis/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/customer-behavior-model-analysis")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/customer-behavior-model-analysis/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/customer-behavior-model-analysis/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/customer-behavior-model-analysis/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
