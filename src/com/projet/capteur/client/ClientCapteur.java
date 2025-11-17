package com.projet.capteur.client;

import java.io.*;
import java.net.Socket;
import java.util.Random;

/**
 * ClientCapteur : se connecte au serveur localhost:5000 et envoie toutes les 3 secondes
 * un message au format texte : "CAPTEUR|temperature|23.5"
 * tourne indéfiniment jusqu'à interruption manuelle.
 */
public class ClientCapteur {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final Random rand = new Random();

    public static void main(String[] args) {
        System.out.println("ClientCapteur démarrage...");
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)) {

            System.out.println("Connecté au serveur " + HOST + ":" + PORT);

            // On n'attend pas d'OK ; on envoie immédiatement le premier identifiant "CAPTEUR"
            // (la consigne indiquait que le serveur doit détecter dès la connexion si le client est CAPTEUR)
            // Nous commençons en envoyant des messages CAPTEUR|type|valeur
            while (true) {
                // temperature
                double temp = randomInRange(15.0, 35.0);
                out.println("CAPTEUR|temperature|" + String.format("%.3f", temp));
                Thread.sleep(3000);

                // humidite
                double hum = randomInRange(30.0, 90.0);
                out.println("CAPTEUR|humidite|" + String.format("%.3f", hum));
                Thread.sleep(3000);

                // pression
                double pres = randomInRange(990.0, 1030.0);
                out.println("CAPTEUR|pression|" + String.format("%.3f", pres));
                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            System.out.println("Client interrompu.");
        } catch (IOException e) {
            System.err.println("Erreur IO client: " + e.getMessage());
        }
    }

    private static double randomInRange(double min, double max) {
        return min + rand.nextDouble() * (max - min);
    }
}
