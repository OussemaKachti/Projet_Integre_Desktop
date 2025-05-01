package com.esprit.models;

public class Orderdetails {
    private int id;
    private Commande commande;
    private Produit produit;
    private int quantity;
    private double price;
    private double total;

    public Orderdetails() {
    }

    public Orderdetails(Produit produit, int quantity, int price) {
        this.produit = produit;
        this.quantity = quantity;
        this.price = price;
        calculateTotal();
    }

    // Getters et setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateTotal(); // Recalculer à chaque changement
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        calculateTotal(); // Recalculer à chaque changement
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void calculateTotal() {
        this.total = this.price * this.quantity;
    }

    @Override
    public String toString() {
        return "Orderdetails{id=" + id + ", quantity=" + quantity + ", price=" + price + ", total=" + total + "}";
    }
}
