package com.projet.capteur.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * ClientSpecial : se connecte au serveur, envoie "SPECIAL", attend "OK", puis présente
 * le menu envoyé par le serveur et interagit correctement.
 */
public class ClientSpecial {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("ClientSpecial démarrage...");
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
             Scanner scanner = new Scanner(System.in)) {

            // Envoyer l'identifiant SPECIAL
            out.println("SPECIAL");

            // Lire la réponse du serveur
            String response = in.readLine();
            if (response == null || !response.equals("OK")) {
                System.err.println("Réponse inattendue du serveur: " + response);
                return;
            }

            System.out.println("Connecté en tant que CLIENT SPECIAL. Tapez votre choix quand le serveur le demande.");

            boolean running = true;

            while (running) {
                String serverLine = in.readLine();
                if (serverLine == null) break;

                // 🔹 Le serveur envoie un menu
                if (serverLine.startsWith("MENU|")) {
                    // Remplacement des \n envoyés en texte par de vrais retours à la ligne
                    String menu = serverLine.substring(5).replace("\\n", "\n");
                    System.out.println(menu);

                    String choix = scanner.nextLine();
                    out.println(choix.trim());
                    continue;
                }

                // 🔹 Le serveur pose une question
                if (serverLine.startsWith("QUESTION|")) {
                    System.out.println(serverLine.substring(9));
                    String reponse = scanner.nextLine();
                    out.println(reponse.trim());
                    continue;
                }

                // 🔹 Le serveur envoie un résultat
                if (serverLine.startsWith("RESULT|")) {
                    String result = serverLine.substring(7).replace("\\n", "\n");
                    System.out.println(result);
                    continue;
                }

                // 🔹 Le serveur signale une erreur
                if (serverLine.startsWith("ERREUR|")) {
                    System.err.println("Serveur: " + serverLine.substring(7));
                    continue;
                }

                // 🔹 Le serveur demande à fermer
                if (serverLine.equals("QUIT")) {
                    System.out.println("Serveur demande fermeture. Fermeture du client.");
                    running = false;
                    continue;
                }

                // 🔹 Message inconnu
                System.out.println(serverLine);
            }
        } catch (IOException e) {
            System.err.println("Erreur IO client special: " + e.getMessage());
        }
    }
}
