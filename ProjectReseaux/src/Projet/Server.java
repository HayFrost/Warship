package Projet;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        ServerSocket server = null;
        Socket client = null;

        try {
            // Création d'un serveur qui écoute sur le port 4444
            server = new ServerSocket(4444);
            System.out.println("Serveur en attente de connexion...");

            // Attente d'une connexion client
            client = server.accept();
            System.out.println("Client connecté!");

            // Récupération des flux d'entrée et de sortie
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            // Lecture du message envoyé par le client
            String messageClient = in.readLine();
            System.out.println("Message reçu du client: " + messageClient);

            // Réponse au client
            out.println("Message reçu et traité: " + messageClient);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (client != null) client.close();
                if (server != null) server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}