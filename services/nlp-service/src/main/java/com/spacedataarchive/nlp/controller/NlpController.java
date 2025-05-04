package com.spacedataarchive.nlp.controller;

import com.spacedataarchive.common.model.SpaceData;
import com.spacedataarchive.nlp.service.NlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nlp")
public class NlpController {

    private final NlpService nlpService;

    @Autowired
    public NlpController(NlpService nlpService) {
        this.nlpService = nlpService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<SpaceData> analyzeContent(@RequestBody SpaceData spaceData) {
        SpaceData analyzed = nlpService.analyzeContent(spaceData);
        return ResponseEntity.ok(analyzed);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("NLP Service is healthy");
    }
} 