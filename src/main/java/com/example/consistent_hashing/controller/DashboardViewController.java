package com.example.consistent_hashing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    @GetMapping("/dashboard")
    public String viewDashboard() {
        return "dashboard";  // This returns the 'dashboard.html' template
    }
}