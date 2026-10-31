package com.parcelflow.shipment.service;

import com.parcelflow.shipment.dto.AdminProcessRequest;
import com.parcelflow.shipment.dto.CreateShipmentOrderRequest;
import com.parcelflow.shipment.dto.CreateShipmentRequest;
import com.parcelflow.shipment.dto.LoginRequest;
import com.parcelflow.shipment.dto.ShipmentResponse;
import com.parcelflow.shipment.dto.SignupRequest;
import com.parcelflow.shipment.dto.UserResponse;
import com.parcelflow.shipment.exception.NotFoundException;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.model.ShipmentRequest;
import com.parcelflow.shipment.model.User;
import com.parcelflow.shipment.repository.ShipmentRequestRepository;
import com.parcelflow.shipment.repository.UserRepository;
import com.parcelflow.shipment.util.SecurityUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private final UserRepository userRepository;
    private final ShipmentRequestRepository requestRepository;
    private final ShipmentService shipmentService;

    public UserService(UserRepository userRepository, ShipmentRequestRepository requestRepository, ShipmentService shipmentService) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.shipmentService = shipmentService;
    }

    public UserService() {
        this(new UserRepository(), new ShipmentRequestRepository(), new ShipmentService());
    }

    public UserResponse signup(SignupRequest req) {
        if (req == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        if (req.getUsername() == null || !USERNAME_PATTERN.matcher(req.getUsername().trim()).matches()) {
            throw new ValidationException("VALIDATION_ERROR", "Username must be 3-30 alphanumeric characters or underscores.");
        }
        if (req.getEmail() == null || !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            throw new ValidationException("VALIDATION_ERROR", "A valid email address is required.");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new ValidationException("VALIDATION_ERROR", "Password must be at least 6 characters long.");
        }
        if (req.getFullName() == null || req.getFullName().trim().isEmpty() || req.getFullName().trim().length() > 100) {
            throw new ValidationException("VALIDATION_ERROR", "Full name is required (max 100 characters).");
        }

        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase();
        String role = "ADMIN".equalsIgnoreCase(req.getRole()) ? "ADMIN" : "USER";

        try {
            if (userRepository.findByUsername(username).isPresent()) {
                throw new ValidationException("USERNAME_EXISTS", "Username is already taken.");
            }
            if (userRepository.findByEmail(email).isPresent()) {
                throw new ValidationException("EMAIL_EXISTS", "Email is already registered.");
            }

            User user = new User(
                    null,
                    username,
                    email,
                    SecurityUtil.hashPassword(req.getPassword()),
                    role,
                    req.getFullName().trim(),
                    LocalDateTime.now()
            );

            user = userRepository.insert(user);
            return toResponse(user);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during signup", e);
        }
    }

    public UserResponse login(LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            throw new ValidationException("VALIDATION_ERROR", "Username and password are required.");
        }

        try {
            var opt = userRepository.findByUsername(req.getUsername().trim());
            if (opt.isEmpty() || !SecurityUtil.checkPassword(req.getPassword(), opt.get().getPasswordHash())) {
                throw new ValidationException("INVALID_CREDENTIALS", "Invalid username or password.");
            }
            return toResponse(opt.get());
        } catch (SQLException e) {
            throw new RuntimeException("Database error during login", e);
        }
    }

    public UserResponse getUserProfile(Long id) {
        try {
            var opt = userRepository.findById(id);
            if (opt.isEmpty()) {
                throw new NotFoundException("USER_NOT_FOUND", "User not found.");
            }
            return toResponse(opt.get());
        } catch (SQLException e) {
            throw new RuntimeException("Database error during profile lookup", e);
        }
    }

    public ShipmentRequest createShipmentOrder(CreateShipmentOrderRequest req) {
        if (req == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        if (req.getUserId() == null) {
            throw new ValidationException("VALIDATION_ERROR", "userId is required.");
        }
        if (req.getSenderName() == null || req.getSenderName().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Sender name is required.");
        }
        if (req.getRecipientName() == null || req.getRecipientName().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Recipient name is required.");
        }
        if (req.getOrigin() == null || req.getOrigin().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Origin is required.");
        }
        if (req.getDestination() == null || req.getDestination().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Destination is required.");
        }

        try {
            if (userRepository.findById(req.getUserId()).isEmpty()) {
                throw new NotFoundException("USER_NOT_FOUND", "User not found.");
            }

            ShipmentRequest sr = new ShipmentRequest();
            sr.setUserId(req.getUserId());
            sr.setSenderName(req.getSenderName().trim());
            sr.setRecipientName(req.getRecipientName().trim());
            sr.setOrigin(req.getOrigin().trim());
            sr.setDestination(req.getDestination().trim());
            sr.setPackageDetails(req.getPackageDetails() != null ? req.getPackageDetails().trim() : "");
            sr.setStatus("PENDING");

            return requestRepository.insert(sr);
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating shipment request", e);
        }
    }

    public List<ShipmentRequest> getUserShipmentRequests(Long userId) {
        try {
            return requestRepository.findByUserId(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching user requests", e);
        }
    }

    public List<ShipmentRequest> getAllShipmentRequests() {
        try {
            return requestRepository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching all requests", e);
        }
    }

    public ShipmentRequest processShipmentRequest(Long requestId, AdminProcessRequest req) {
        if (req == null || req.getAction() == null) {
            throw new ValidationException("VALIDATION_ERROR", "Action is required (APPROVE, REJECT, or UPDATE).");
        }

        try {
            var opt = requestRepository.findById(requestId);
            if (opt.isEmpty()) {
                throw new NotFoundException("REQUEST_NOT_FOUND", "Shipment request not found.");
            }

            ShipmentRequest sr = opt.get();
            String action = req.getAction().toUpperCase();

            if ("APPROVE".equals(action)) {
                // If not already approved/created, create official shipment in shipment service
                if (sr.getTrackingId() == null || sr.getTrackingId().isEmpty()) {
                    CreateShipmentRequest createReq = new CreateShipmentRequest();
                    createReq.setSenderName(sr.getSenderName());
                    createReq.setRecipientName(sr.getRecipientName());
                    createReq.setOrigin(sr.getOrigin());
                    createReq.setDestination(req.getDestination() != null && !req.getDestination().isBlank() ? req.getDestination().trim() : sr.getDestination());
                    
                    ShipmentResponse created = shipmentService.createShipment(createReq);
                    sr.setTrackingId(created.getTrackingId());
                }
                sr.setStatus("APPROVED");
                if (req.getAdminNotes() != null) sr.setAdminNotes(req.getAdminNotes().trim());
                if (req.getDestination() != null && !req.getDestination().isBlank()) sr.setDestination(req.getDestination().trim());
                if (req.getPackageDetails() != null) sr.setPackageDetails(req.getPackageDetails().trim());
            } else if ("REJECT".equals(action)) {
                sr.setStatus("REJECTED");
                if (req.getAdminNotes() != null) sr.setAdminNotes(req.getAdminNotes().trim());
            } else if ("UPDATE".equals(action)) {
                if (req.getAdminNotes() != null) sr.setAdminNotes(req.getAdminNotes().trim());
                if (req.getDestination() != null && !req.getDestination().isBlank()) sr.setDestination(req.getDestination().trim());
                if (req.getPackageDetails() != null) sr.setPackageDetails(req.getPackageDetails().trim());
            } else {
                throw new ValidationException("VALIDATION_ERROR", "Invalid action: " + action + ". Allowed: APPROVE, REJECT, UPDATE.");
            }

            requestRepository.update(sr);
            return sr;
        } catch (SQLException e) {
            throw new RuntimeException("Database error processing shipment request", e);
        }
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getFullName(), u.getCreatedAt());
    }
}
