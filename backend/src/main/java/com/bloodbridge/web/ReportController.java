package com.bloodbridge.web;

import com.bloodbridge.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/requests.csv")
    @PreAuthorize("hasAuthority('PERM_REPORT_EXPORT')")
    public ResponseEntity<String> requests() {
        return csv("bloodbridge-requests.csv", service.requestsCsv());
    }

    @GetMapping("/donations.csv")
    @PreAuthorize("hasAuthority('PERM_REPORT_EXPORT')")
    public ResponseEntity<String> donations() {
        return csv("bloodbridge-donations.csv", service.donationsCsv());
    }

    private ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }
}
