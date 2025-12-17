package com.projectjavasneaker.backendis216.controllers;

import com.projectjavasneaker.backendis216.Exception.NotFoundException;
import com.projectjavasneaker.backendis216.models.Invoice;
import com.projectjavasneaker.backendis216.models.InvoiceDetails;
import com.projectjavasneaker.backendis216.models.User;
import com.projectjavasneaker.backendis216.payload.request.ChangePassword;
import com.projectjavasneaker.backendis216.payload.request.CartRequest;
import com.projectjavasneaker.backendis216.payload.request.InvoiceRequest;
import com.projectjavasneaker.backendis216.payload.response.PageResponse;
import com.projectjavasneaker.backendis216.payload.response.ResponseObject;
import com.projectjavasneaker.backendis216.repository.UserRepository;
import com.projectjavasneaker.backendis216.services.InvoiceDetailsService;
import com.projectjavasneaker.backendis216.services.InvoiceService;
import com.projectjavasneaker.backendis216.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private InvoiceDetailsService invoiceDetailsService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private CartService cartService;
    @Autowired
    PasswordEncoder encoder;

    @GetMapping("/all-user")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> getAllUsers(){
        List<User> users = userRepository.findAll();
        if(users.size() > 0){
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok","get all users successfully", users)
            );
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                new ResponseObject("ok", "cannot find users", "")
        );
    }
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<PageResponse> getAllUsersPage(@RequestParam Optional<Integer> page){
       Page<User> pageUsers = userRepository.findAll(PageRequest.of(page.orElse(0), 6));
       return ResponseEntity.status(HttpStatus.OK).body(
               new PageResponse(page, pageUsers.getSize(), pageUsers.getTotalElements(),
                       pageUsers.getTotalPages(),
                       pageUsers.getContent()
                       )
       );
    }

//    sua
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
ResponseEntity<ResponseObject> UpdateUser(@PathVariable Long id, @RequestBody User newUser){
    User existUser = userRepository.findById(id).map((user)->{
        user.setAddress(newUser.getAddress());
        user.setEmail(newUser.getEmail());
        user.setPhone(newUser.getPhone());
        return userRepository.save(user);
    }).orElseGet(()->{
        return userRepository.save(newUser);
    });

    return ResponseEntity.status(HttpStatus.OK).body(
            new ResponseObject("ok", "updated user success", existUser)
    );
}

//    xoa
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> DeleteProduct(@PathVariable Long id){
        boolean existProduct = userRepository.existsById(id);
        if(existProduct){
            userRepository.deleteById(id);
            List<User> users = userRepository.findAll();
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "Delete user successfully", users)
            );
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ResponseObject("failed", "cannot find user to delete", "")
        );
    }
    // lấy ra danh sách sản phẩm user đã mua
    @GetMapping("/{userId}/list-product")
    public ResponseEntity<List<InvoiceDetails>> getPurchasedProducts(@PathVariable Long userId) {
        List<InvoiceDetails> purchasedProducts = invoiceDetailsService.getInvoiceDetailsByUserId(userId);
        return ResponseEntity.ok(purchasedProducts);
    }
    @GetMapping("/{userId}/all-invoices")
    public ResponseEntity<List<Invoice>> getUserInvoices(@PathVariable Long userId) {
        List<Invoice> invoices = invoiceService.getInvoicesByUserId(userId);
        return ResponseEntity.ok(invoices);
    }

    // API for admin to get all invoices with pagination
    @GetMapping("/admin/all-invoices")
    @CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS}, allowedHeaders = "*")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse> getAllInvoices(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<String> sortBy,
            @RequestParam Optional<String> status) {

        // For now, return all invoices without pagination
        // TODO: Implement pagination later
        List<Invoice> allInvoices = invoiceService.getAllInvoices();

        // Filter by status if provided
        if (status.isPresent() && !status.get().isEmpty()) {
            allInvoices = allInvoices.stream()
                    .filter(invoice -> status.get().equalsIgnoreCase(invoice.getStatus()))
                    .collect(Collectors.toList());
        }

        // Sort if requested
        if (sortBy.isPresent() && !sortBy.get().isEmpty()) {
            switch (sortBy.get().toLowerCase()) {
                case "date":
                    allInvoices.sort((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()));
                    break;
                case "total":
                    allInvoices.sort((a, b) -> {
                        BigDecimal priceA = a.getTotalPrice() != null ? a.getTotalPrice() : BigDecimal.ZERO;
                        BigDecimal priceB = b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO;
                        return priceB.compareTo(priceA); // Descending order
                    });
                    break;
                default:
                    // Default sort by date descending
                    allInvoices.sort((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()));
            }
        }

        return ResponseEntity.ok(new PageResponse(
                page,
                10, // page size
                allInvoices.size(), // total elements
                (allInvoices.size() + 9) / 10, // total pages
                allInvoices
        ));
    }

    // API to update invoice
    @PutMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject> updateInvoice(
            @PathVariable Long invoiceId,
            @RequestBody Map<String, String> updateData) {

        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            if (invoice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseObject("error", "Invoice not found", null));
            }

            // Update status if provided
            if (updateData.containsKey("status")) {
                String status = updateData.get("status");
                if (Arrays.asList("notpaid", "paid", "cancelled").contains(status)) {
                    invoice.setStatus(status);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ResponseObject("error", "Invalid status value", null));
                }
            }

            // Update shipping address if provided
            if (updateData.containsKey("shipAddress")) {
                invoice.setShipAddress(updateData.get("shipAddress"));
            }

            invoiceService.saveInvoice(invoice);

            return ResponseEntity.ok(new ResponseObject("ok", "Invoice updated successfully", invoice));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject("error", "Error updating invoice: " + e.getMessage(), null));
        }
    }

    // API to create invoice from cart items
    @PostMapping("/{userId}/create-invoice")
    public ResponseEntity<ResponseObject> createInvoiceFromCartItems(
            @PathVariable Long userId,
            @RequestBody InvoiceRequest invoiceRequest) {

        try {
            // Get user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Check if cart items are provided
            if (invoiceRequest.getCartItems() == null || invoiceRequest.getCartItems().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ResponseObject("failed", "Cart is empty", null)
                );
            }

            // Create invoice
            Invoice invoice = invoiceService.createInvoice();
            invoice.setUsers(user);
            invoice.setShipAddress(invoiceRequest.getShipAddress());
            invoiceService.saveInvoice(invoice);

            // Create invoice details from cart items
            invoiceService.createInvoiceDetailsFromItems(invoice, invoiceRequest.getCartItems());

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "Invoice created successfully", invoice)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("failed", "Error creating invoice: " + e.getMessage(), null)
            );
        }
    }

    @PutMapping("/profile/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    ResponseEntity<ResponseObject> UpdateUserProfile(@PathVariable Long id, @RequestBody User newUser){
        User existUser = userRepository.findById(id).map((user)->{
            user.setAddress(newUser.getAddress());
            user.setEmail(newUser.getEmail());
            user.setPhone(newUser.getPhone());
            return userRepository.save(user);
        }).orElseGet(()->{
            return userRepository.save(newUser);
        });

        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("ok", "updated user success", existUser)
        );
    }

    @PutMapping("/changePassword/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    ResponseEntity<ResponseObject> ChangePassword(@PathVariable Long id, @RequestBody ChangePassword changePassword){



        Optional<User> existUser = userRepository.findById(id).map((user)->{
            if(!encoder.matches(changePassword.getCurrentPass(), user.getPassword())){
                throw new NotFoundException("old password is not correct");
            }
            user.setPassword(encoder.encode(changePassword.getPassword()));
            return userRepository.save(user);
        });

        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("ok", "change password user success", existUser)
        );
    }

}
