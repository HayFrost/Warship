import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Random;

public class ClientGUI {
    private static final int GRID_SIZE = 10;
    private static final int NUM_SHIPS = 3;

    private JFrame frame;
    private JButton[][] buttons;
    private String[][] clientBoard;
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    private int clientShipsRemaining = NUM_SHIPS;

    public ClientGUI() {
        initializeConnection();
        initializeGUI();
        clientBoard = initializeBoard();
        placeShips(clientBoard, NUM_SHIPS);
        System.out.println("Grille du client initialisée :");
        printBoard(clientBoard);
    }

    private void initializeConnection() {
        try {
            client = new Socket("localhost", 4444);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            System.out.println("Connexion au serveur réussie!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Impossible de se connecter au serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeGUI() {
        frame = new JFrame("Bataille Navale - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 650);

        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                JButton button = new JButton();
                button.setBackground(Color.CYAN);
                button.addActionListener(new ButtonClickListener(i, j));
                buttons[i][j] = button;
                gridPanel.add(button);
            }
        }

        JButton quitButton = new JButton("Quitter");
        quitButton.addActionListener(e -> {
            closeConnection();
            frame.dispose();
        });

        frame.setLayout(new BorderLayout());
        frame.add(gridPanel, BorderLayout.CENTER);
        frame.add(quitButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private class ButtonClickListener implements ActionListener {
        private int x, y;

        public ButtonClickListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String move = x + "," + y;
            out.println(move);

            try {
                // Lecture de la réponse du serveur
                String response = in.readLine();
                System.out.println("Réponse du serveur : " + response);

                if (response.contains("Touché")) {
                    buttons[x][y].setBackground(Color.RED);
                } else if (response.contains("Manqué")) {
                    buttons[x][y].setBackground(Color.WHITE);
                }

                // Vérification de la fin de partie
                if (response.contains("Gagné!") || response.contains("Perdu!")) {
                    JOptionPane.showMessageDialog(frame, response);
                    closeConnection();
                    frame.dispose(); // Fermeture de la fenêtre après le message
                    return; // Sortie immédiate
                }

                // Tour du serveur
                String serverMove = in.readLine();
                System.out.println("Coup du serveur : " + serverMove);
                String serverResponse = processMove(clientBoard, serverMove);
                out.println(serverResponse);

                if (serverResponse.contains("Touché")) {
                    clientShipsRemaining--;
                    if (clientShipsRemaining <= 0) {
                        JOptionPane.showMessageDialog(frame, "Tous vos bateaux ont été coulés! Vous avez perdu.");
                        closeConnection();
                        frame.dispose(); // Fermeture de la fenêtre après le message
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String[][] initializeBoard() {
        String[][] board = new String[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                board[i][j] = ".";
            }
        }
        return board;
    }

    private void placeShips(String[][] board, int numShips) {
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

    private String processMove(String[][] board, String move) {
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

    private void closeConnection() {
        try {
            if (client != null) client.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printBoard(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
