package com.sh.tbs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class InfoController {

    @GetMapping("/info")
    public Map<String, String> info() {
        log.info("GET /info");
        return Map.of(
            "app", "tbs",
            "status", "running"
        );
    }
}
