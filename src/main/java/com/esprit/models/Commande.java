package com.esprit.models;

import com.esprit.models.enums.StatutCommandeEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Commande {
    private int id;
    private LocalDate dateComm;
    private StatutCommandeEnum statut;
    private User user;
    private List<Orderdetails> orderDetails = new ArrayList<>();
    private double total; // Add a field to store the total

    public Commande() {
        this.dateComm = LocalDate.now();
    }

    public double getTotal() {
        return total; // Return the stored total
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDateComm() {
        return dateComm;
    }

    public void setDateComm(LocalDate dateComm) {
        this.dateComm = dateComm;
    }

    public StatutCommandeEnum getStatut() {
        return statut;
    }

    public void setStatut(StatutCommandeEnum statut) {
        this.statut = statut;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Orderdetails> getOrderDetails() {
        return orderDetails;
    }

    public void addOrderDetail(Orderdetails orderDetail) {
        this.orderDetails.add(orderDetail);
        calculateTotal(); // Recalculate total when adding a detail
    }

    public void removeOrderDetail(Orderdetails orderDetail) {
        this.orderDetails.remove(orderDetail);
        calculateTotal(); // Recalculate total when removing a detail
    }

    public void setOrderDetails(List<Orderdetails> orderDetails) {
        this.orderDetails = orderDetails != null ? orderDetails : new ArrayList<>();
        calculateTotal(); // Recalculate total when setting details
    }

    public void setTotal(double total) {
        this.total = total;
    }

    private void calculateTotal() {
        this.total = 0.0;
        for (Orderdetails detail : orderDetails) {
            this.total += detail.getTotal();
        }
    }

    @Override
    public String toString() {
        return "Commande{id=" + id + ", dateComm=" + dateComm + ", statut=" + statut + ", user=" + user + ", total=" + total + "}";
    }
}