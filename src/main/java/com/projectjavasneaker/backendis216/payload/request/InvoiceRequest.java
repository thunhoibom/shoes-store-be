package com.projectjavasneaker.backendis216.payload.request;

import java.util.List;

public class InvoiceRequest {
    private String shipAddress;
    private List<CartItemRequest> cartItems;

    public InvoiceRequest() {
    }

    public InvoiceRequest(String shipAddress, List<CartItemRequest> cartItems) {
        this.shipAddress = shipAddress;
        this.cartItems = cartItems;
    }

    public String getShipAddress() {
        return shipAddress;
    }

    public void setShipAddress(String shipAddress) {
        this.shipAddress = shipAddress;
    }

    public List<CartItemRequest> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItemRequest> cartItems) {
        this.cartItems = cartItems;
    }
}
