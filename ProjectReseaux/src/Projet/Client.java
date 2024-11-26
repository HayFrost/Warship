package Projet;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        Socket client = null;

        try {
            // Connexion au serveur sur localhost et port 4444
            client = new Socket("localhost", 4444);
            System.out.println("Connexion au serveur réussie!");

            // Récupération des flux d'entrée et de sortie
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            // Envoi d'un message au serveur
            out.println("Bonjour serveur, c'est le client!");

            // Lecture de la réponse du serveur
            String messageServeur = in.readLine();
            System.out.println("Réponse du serveur: " + messageServeur);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (client != null) client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}