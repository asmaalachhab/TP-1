package com.projet.capteur.serveur;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe utilitaire pour obtenir une connexion JDBC vers la base MySQL.
 * Modifiez USER et PASSWORD si nécessaire.
 */
public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/capteurs_db?serverTimezone=UTC&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // modifier si nécessaire

    static {
        try {
            // Charger le driver MySQL (souvent pas nécessaire si driver sur classpath)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC MySQL introuvable. Assurez-vous que le connector est sur le classpath.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
