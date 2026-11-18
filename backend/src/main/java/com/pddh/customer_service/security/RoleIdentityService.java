package com.pddh.customer_service.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class RoleIdentityService extends com.pddh.roleprofile.RoleIdentityService {
    public RoleIdentityService(@Value("${app.jwt.secret}") String secret) { super(secret); }
}
