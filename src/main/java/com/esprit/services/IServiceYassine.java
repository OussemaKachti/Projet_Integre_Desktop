package com.esprit.services;

import java.sql.SQLException;
import java.util.List;

public interface IServiceYassine<T> {
    void add(T t) throws SQLException; // Ajout d'un élément

    void update(T t) throws SQLException; // Modification d'un élément

    void delete(int id) throws SQLException; // Suppression d'un élément

    List<T> getAll() throws SQLException; // Affichage de la liste des éléments
}