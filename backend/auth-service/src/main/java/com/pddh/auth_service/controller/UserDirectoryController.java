package com.pddh.auth_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pddh.auth_service.dto.UserDirectoryRequest;
import com.pddh.auth_service.dto.UserDirectoryResponse;
import com.pddh.auth_service.security.CurrentUserService;
import com.pddh.auth_service.service.UserDirectoryService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users/directory")
public class UserDirectoryController {
    private final CurrentUserService currentUserService;
    private final UserDirectoryService directoryService;

    public UserDirectoryController(CurrentUserService currentUserService, UserDirectoryService directoryService) {
        this.currentUserService = currentUserService;
        this.directoryService = directoryService;
    }

    @GetMapping
    public List<UserDirectoryResponse> list(HttpServletRequest request) {
        return directoryService.list(currentUserService.requireRole(request));
    }

    @PostMapping
    public ResponseEntity<UserDirectoryResponse> create(
            @RequestBody UserDirectoryRequest entry, HttpServletRequest request) {
        UserDirectoryResponse saved = directoryService.create(currentUserService.requireRole(request), entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
