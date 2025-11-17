package com.projet.capteur.serveur;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class Serveur {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Serveur démarré, écoute sur le port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion depuis " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur sur le serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }

  
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                String firstLine = in.readLine();
                if (firstLine == null) {
                    closeAll();
                    return;
                }

                if (firstLine.startsWith("CAPTEUR")) {
                    handleCapteur(firstLine);
                } else if (firstLine.equalsIgnoreCase("SPECIAL")) {
                    handleSpecial();
                } else {
                    out.println("ERREUR|Type client inconnu");
                    closeAll();
                }
            } catch (IOException e) {
                System.err.println("Communication interrompue: " + e.getMessage());
            } finally {
                closeAll();
            }
        }

        
        private void handleCapteur(String firstLine) {
            System.out.println("Client capteur identifié: " + socket.getRemoteSocketAddress());
            try {
                processCapteurMessage(firstLine);

                String line;
                while ((line = in.readLine()) != null) {
                    processCapteurMessage(line);
                }
            } catch (IOException e) {
                System.err.println("Lecture capteur interrompue: " + e.getMessage());
            }
            System.out.println("Client capteur déconnecté: " + socket.getRemoteSocketAddress());
        }

        
        private void processCapteurMessage(String msg) {
            if (msg == null) return;
            if (!msg.startsWith("CAPTEUR|")) {
                System.err.println("Message capteur mal formé: " + msg);
                return;
            }
            String[] parts = msg.split("\\|");
            if (parts.length != 3) {
                System.err.println("Message capteur mal formé (nombre de segments): " + msg);
                return;
            }
            String type = parts[1].trim().toLowerCase();
            String valeurStr = parts[2].trim();
            double valeur;
            try {
                valeur = Double.parseDouble(valeurStr);
            } catch (NumberFormatException e) {
                System.err.println("Valeur capteur non numérique: " + valeurStr);
                return;
            }

            String sql = "INSERT INTO mesures (type, valeur) VALUES (?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setDouble(2, valeur);
                ps.executeUpdate();
                System.out.printf("Mesure insérée : type=%s valeur=%.3f%n", type, valeur);
            } catch (SQLException ex) {
                System.err.println("Erreur insertion mesure: " + ex.getMessage());
            }
        }

       
        private void handleSpecial() {
            System.out.println("Client SPECIAL connecté: " + socket.getRemoteSocketAddress());
            out.println("OK"); // Accusé de réception

            try {
                boolean quitter = false;
                while (!quitter) {
                    sendMenu();
                    String choix = in.readLine();
                    if (choix == null) break;
                    choix = choix.trim();
                    switch (choix) {
                        case "1":
                            sendDernieresMesuresToutes();
                            break;
                        case "2":
                            out.println("QUESTION|Quelle grandeur ? (temperature/humidite/pression)");
                            String grandeur2 = in.readLine();
                            if (grandeur2 != null) {
                                sendDernieresParGrandeur(grandeur2.trim().toLowerCase());
                            }
                            break;
                        case "3":
                            sendMoyennesToutes();
                            break;
                        case "4":
                            out.println("QUESTION|Quelle grandeur ? (temperature/humidite/pression)");
                            String grandeur4 = in.readLine();
                            if (grandeur4 != null) {
                                sendMoyenneParGrandeur(grandeur4.trim().toLowerCase());
                            }
                            break;
                        case "5":
                            out.println("QUIT");
                            quitter = true;
                            break;
                        default:
                            out.println("ERREUR|Choix invalide");
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur communication client special: " + e.getMessage());
            }
            System.out.println("Client SPECIAL déconnecté: " + socket.getRemoteSocketAddress());
        }

        private void sendMenu() {
            StringBuilder menu = new StringBuilder();
            menu.append("===CLIENT SPÉCIAL - MENU===\n");
            menu.append("1. Voir les 10 dernières mesures (toutes grandeurs)\n");
            menu.append("2. Voir les dernières mesures par grandeur\n");
            menu.append("3. Voir les moyennes de toutes les grandeurs\n");
            menu.append("4. Voir la moyenne par grandeur\n");
            menu.append("5. Quitter\n");
            menu.append("Votre choix :");
            out.println("MENU|" + menu.toString().replace("\n", "\\n"));


        }

        private void sendDernieresMesuresToutes() {
            String sql = "SELECT id, type, valeur, date_envoi FROM mesures ORDER BY date_envoi DESC LIMIT 10";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                StringBuilder sb = new StringBuilder();
                sb.append("RESULT|10 dernières mesures (toutes grandeurs):\n");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("type");
                    double valeur = rs.getDouble("valeur");
                    Timestamp ts = rs.getTimestamp("date_envoi");
                    sb.append(String.format("id=%d | %s | %.3f | %s%n", id, type, valeur, ts.toLocalDateTime().format(fmt)));
                }
                out.println(sb.toString());
            } catch (SQLException ex) {
                out.println("ERREUR|Impossible de récupérer les mesures: " + ex.getMessage());
            }
        }

        private void sendDernieresParGrandeur(String grandeur) {
            if (!isValidGrandeur(grandeur)) {
                out.println("ERREUR|Grandeur invalide");
                return;
            }
            String sql = "SELECT id, type, valeur, date_envoi FROM mesures WHERE type = ? ORDER BY date_envoi DESC LIMIT 10";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, grandeur);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("RESULT|10 dernières mesures pour ").append(grandeur).append(":\n");
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        int id = rs.getInt("id");
                        double valeur = rs.getDouble("valeur");
                        Timestamp ts = rs.getTimestamp("date_envoi");
                        sb.append(String.format("id=%d | %s | %.3f | %s%n", id, grandeur, valeur, ts.toLocalDateTime().format(fmt)));
                    }
                    if (!any) sb.append("(Aucune mesure pour cette grandeur)\n");
                    out.println(sb.toString());
                }
            } catch (SQLException ex) {
                out.println("ERREUR|Impossible de récupérer les mesures: " + ex.getMessage());
            }
        }

        private void sendMoyennesToutes() {
            String sql = "SELECT type, AVG(valeur) AS moyenne FROM mesures GROUP BY type";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("RESULT|Moyennes par grandeur (sur toutes les mesures):\n");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String type = rs.getString("type");
                    double moy = rs.getDouble("moyenne");
                    sb.append(String.format("%s : %.3f%n", type, moy));
                }
                if (!any) sb.append("(Aucune mesure en base)\n");
                out.println(sb.toString());
            } catch (SQLException ex) {
                out.println("ERREUR|Impossible de calculer les moyennes: " + ex.getMessage());
            }
        }

        private void sendMoyenneParGrandeur(String grandeur) {
            if (!isValidGrandeur(grandeur)) {
                out.println("ERREUR|Grandeur invalide");
                return;
            }
            String sql = "SELECT AVG(valeur) AS moyenne FROM mesures WHERE type = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, grandeur);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double moy = rs.getDouble("moyenne");
                        if (rs.wasNull()) {
                            out.println("RESULT|Aucune mesure pour " + grandeur);
                        } else {
                            out.println(String.format("RESULT|Moyenne pour %s : %.3f", grandeur, moy));
                        }
                    } else {
                        out.println("RESULT|Aucune mesure pour " + grandeur);
                    }
                }
            } catch (SQLException ex) {
                out.println("ERREUR|Impossible de calculer la moyenne: " + ex.getMessage());
            }
        }

        private boolean isValidGrandeur(String g) {
            if (g == null) return false;
            switch (g.toLowerCase()) {
                case "temperature":
                case "humidite":
                case "pression":
                    return true;
                default:
                    return false;
            }
        }

        private void closeAll() {
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {}
            if (out != null) out.close();
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
