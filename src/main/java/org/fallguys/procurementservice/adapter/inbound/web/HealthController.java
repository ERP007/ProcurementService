package org.fallguys.procurementservice.adapter.inbound.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/procurement")
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "procurement-service ok";
    }
}
