import java.io.*;
import java.net.*;
import java.util.Random;

public class Server {
    private static final int GRID_SIZE = 10;
    private static final int NUM_SHIPS = 3;

    private static String[][] serverBoard;
    private static int serverShipsRemaining = NUM_SHIPS;
    private static int clientShipsRemaining = NUM_SHIPS;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(4444);
            System.out.println("Serveur en attente de connexion...");
            clientSocket = serverSocket.accept();
            System.out.println("Client connecté!");

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Initialisation des grilles
            serverBoard = initializeBoard();
            placeShips(serverBoard, NUM_SHIPS);
            System.out.println("Grille du serveur initialisée :");
            printBoard(serverBoard);

            // Jeu principal
            boolean isServerTurn = false;
            boolean gameRunning = true;

            while (gameRunning) {
                if (isServerTurn) {
                    // Tour du serveur
                    String serverMove = generateRandomMove();
                    System.out.println("Le serveur joue : " + serverMove);
                    out.println(serverMove);

                    String result = in.readLine();
                    System.out.println("Résultat du coup du serveur : " + result);

                    if (result.contains("Touché")) {
                        clientShipsRemaining--;
                        if (clientShipsRemaining <= 0) {
                            System.out.println("Le serveur a gagné !");
                            out.println("Perdu! Tous vos bateaux sont coulés.");
                            gameRunning = false;
                        }
                    }

                    isServerTurn = false;
                } else {
                    // Tour du client
                    String clientMove = in.readLine();
                    System.out.println("Le client joue : " + clientMove);

                    String result = processMove(serverBoard, clientMove);
                    out.println(result);

                    if (result.contains("Touché")) {
                        serverShipsRemaining--;
                        if (serverShipsRemaining <= 0) {
                            System.out.println("Le client a gagné !");
                            out.println("Gagné! Tous les bateaux du serveur sont coulés.");
                            gameRunning = false;
                        }
                    }

                    isServerTurn = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[][] initializeBoard() {
        String[][] board = new String[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                board[i][j] = ".";
            }
        }
        return board;
    }

    private static void placeShips(String[][] board, int numShips) {
        Random rand = new Random();
        int shipsPlaced = 0;
        while (shipsPlaced < numShips) {
            int x = rand.nextInt(GRID_SIZE);
            int y = rand.nextInt(GRID_SIZE);
            if (board[x][y].equals(".")) {
                board[x][y] = "B";
                shipsPlaced++;
            }
        }
    }

    private static String processMove(String[][] board, String move) {
        try {
            String[] parts = move.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            if (board[x][y].equals("B")) {
                board[x][y] = "X"; // Marquer le bateau touché
                return "Touché!";
            } else if (board[x][y].equals(".")) {
                board[x][y] = "O"; // Marquer un tir manqué
                return "Manqué!";
            } else {
                return "Déjà tiré ici!";
            }
        } catch (Exception e) {
            return "Mouvement invalide!";
        }
    }

    private static String generateRandomMove() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(GRID_SIZE);
            y = rand.nextInt(GRID_SIZE);
        } while (!serverBoard[x][y].equals("."));
        return x + "," + y;
    }

    private static void printBoard(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }
}
