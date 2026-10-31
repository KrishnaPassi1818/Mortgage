package com.parcelflow.shipment;

import com.parcelflow.shipment.dto.SignupRequest;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceValidationTest {

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testSignupValidationNullBody() {
        assertThrows(ValidationException.class, () -> service.signup(null));
    }

    @Test
    void testSignupValidationInvalidUsername() {
        SignupRequest req = new SignupRequest();
        req.setUsername("a!");
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFullName("Test User");
        assertThrows(ValidationException.class, () -> service.signup(req));
    }

    @Test
    void testSignupValidationInvalidEmail() {
        SignupRequest req = new SignupRequest();
        req.setUsername("validuser");
        req.setEmail("notanemail");
        req.setPassword("password123");
        req.setFullName("Test User");
        assertThrows(ValidationException.class, () -> service.signup(req));
    }

    @Test
    void testSignupValidationShortPassword() {
        SignupRequest req = new SignupRequest();
        req.setUsername("validuser");
        req.setEmail("valid@example.com");
        req.setPassword("123");
        req.setFullName("Test User");
        assertThrows(ValidationException.class, () -> service.signup(req));
    }
}
