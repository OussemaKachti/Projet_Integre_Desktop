package services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    void ajouter(T t) throws SQLException;   // Ajout d'un élément
    void modifier(T t) throws SQLException;  // Modification d'un élément
    void supprimer(int id) throws SQLException; // Suppression d'un élément
    List<T> afficher() throws SQLException; // Affichage de la liste des éléments
}
