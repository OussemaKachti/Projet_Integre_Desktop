package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Produit {
    private int id;
    private String nomProd;
    private String descProd;
    private float prix;
    private String imgProd;
    private LocalDateTime createdAt;
    private String quantity;
    private Club club;
    private List<Orderdetails> orderdetails = new ArrayList<>();

    public Produit() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters et setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomProd() {
        return nomProd;
    }

    public void setNomProd(String nomProd) {
        this.nomProd = nomProd;
    }

    public String getDescProd() {
        return descProd;
    }

    public void setDescProd(String descProd) {
        this.descProd = descProd;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }

    public String getImgProd() {
        return imgProd;
    }

    public void setImgProd(String imgProd) {
        this.imgProd = imgProd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public List<Orderdetails> getOrderdetails() {
        return orderdetails;
    }

    public void addOrderdetail(Orderdetails orderdetail) {
        this.orderdetails.add(orderdetail);
    }

    public void removeOrderdetail(Orderdetails orderdetail) {
        this.orderdetails.remove(orderdetail);
    }
}
