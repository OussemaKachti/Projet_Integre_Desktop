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

    public Commande() {
        this.dateComm = LocalDate.now();
    }

    public double getTotal() {
        double total = 0.0;
        for (Orderdetails detail : orderDetails) {
            total += detail.getTotal();
        }
        return total;
    }

    // Getters et setters

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
    }

    public void removeOrderDetail(Orderdetails orderDetail) {
        this.orderDetails.remove(orderDetail);
    }
}
