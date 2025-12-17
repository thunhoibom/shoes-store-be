package com.projectjavasneaker.backendis216.services;

import com.projectjavasneaker.backendis216.models.Cart;
import com.projectjavasneaker.backendis216.models.Invoice;
import com.projectjavasneaker.backendis216.payload.request.CartItemRequest;
import java.util.List;

import java.util.List;

public interface InvoiceService {
    Invoice createInvoice();
    void saveInvoice(Invoice invoice);
    void createInvoiceDetails(Invoice invoice, Cart cart);
    void createInvoiceDetailsFromItems(Invoice invoice, List<CartItemRequest> cartItems);
    public void updatePaymentStatus(Long invoiceId, String paymentStatus);
    public List<Invoice> getInvoicesByUserId(Long userId);
    public List<Invoice> getAllInvoices();
    public Invoice getInvoiceById(Long invoiceId);
}
