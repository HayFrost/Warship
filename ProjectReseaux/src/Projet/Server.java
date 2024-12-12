import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Random;

public class Server {
    private static final int GRID_SIZE = 10;
    private static final int NUM_SHIPS = 3;

    private JFrame frame;
    private JButton[][] serverButtons;
    private JButton[][] clientViewButtons;
    private String[][] serverBoard;
    private String[][] clientView;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int serverShipsRemaining = NUM_SHIPS;
    private int clientShipsRemaining = NUM_SHIPS;
    private boolean isServerTurn = true;

    public Server() {
        initializeConnection();
        initializeGUI();
        serverBoard = initializeBoard();
        clientView = initializeBoard();
        placeShips(serverBoard, NUM_SHIPS);
        System.out.println("Grille du serveur initialisée :");
        printBoard(serverBoard);


        new Thread(this::receiveClientMoves).start();
    }

    private void initializeConnection() {
        try {
            serverSocket = new ServerSocket(4444);
            System.out.println("Serveur en attente de connexion...");
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println("Client connecté !");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erreur lors de la connexion.", "Erreur", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeGUI() {
        frame = new JFrame("Bataille Navale  (Serveur)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        serverButtons = new JButton[GRID_SIZE][GRID_SIZE];
        clientViewButtons = new JButton[GRID_SIZE][GRID_SIZE];

        JPanel serverGrid = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        JPanel clientGrid = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {

                JButton serverButton = new JButton();
                serverButton.setBackground(Color.CYAN);
                serverButton.setEnabled(false);
                serverButtons[i][j] = serverButton;
                serverGrid.add(serverButton);


                JButton clientButton = new JButton();
                clientButton.setBackground(Color.CYAN);
                clientButton.addActionListener(new ServerButtonClickListener(i, j));
                clientViewButtons[i][j] = clientButton;
                clientGrid.add(clientButton);
            }
        }

        JButton quitButton = new JButton("Quitter");
        quitButton.addActionListener(e -> {
            closeConnection();
            frame.dispose();
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel gridsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        gridsPanel.add(serverGrid);
        gridsPanel.add(clientGrid);

        mainPanel.add(new JLabel("Grille coté Serveur (à gauche) et Grille coté Client (à droite)", SwingConstants.CENTER), BorderLayout.NORTH);
        mainPanel.add(gridsPanel, BorderLayout.CENTER);
        mainPanel.add(quitButton, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private class ServerButtonClickListener implements ActionListener {
        private int x, y;

        public ServerButtonClickListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isServerTurn) {
                JOptionPane.showMessageDialog(frame, "C'est pas a toi de jouer !");
                return;
            }

            String move = x + "," + y;
            out.println(move);

            try {
                String response = in.readLine();
                System.out.println("Réponse du client : " + response);

                if (response.contains("Bam !!!")) {
                    clientViewButtons[x][y].setBackground(Color.RED);
                    clientView[x][y] = "X";
                    clientShipsRemaining--;
                } else if (response.contains("Hé non !")) {
                    clientViewButtons[x][y].setBackground(Color.WHITE);
                    clientView[x][y] = "O";
                }

                if (response.contains("Gagné!")) {
                    JOptionPane.showMessageDialog(frame, "Félicitations la victoire navale est votre !!!");
                    closeConnection();
                    frame.dispose();
                }

                isServerTurn = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void receiveClientMoves() {
        try {
            while (true) {
                String clientMove = in.readLine();
                if (clientMove == null) break;

                System.out.println("Coup reçu du client : " + clientMove);
                String response = processMove(serverBoard, clientMove);
                out.println(response);

                if (response.contains("BAM !!!")) {
                    serverShipsRemaining--;
                    if (serverShipsRemaining <= 0) {
                        JOptionPane.showMessageDialog(frame, "Ah nan la loose !!!");
                        closeConnection();
                        frame.dispose();
                    }
                }

                updateServerGrid();
                isServerTurn = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                board[x][y] = "X";
                return "BAM !!!";
            } else if (board[x][y].equals(".")) {
                board[x][y] = "O";
                return "Hé non !";
            } else {
                return "Tu as deja tiré ici ";
            }
        } catch (Exception e) {
            return "ATTENTION JE TIREE !!!";
        }
    }

    private void updateServerGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (serverBoard[i][j].equals("X")) {
                    serverButtons[i][j].setBackground(Color.RED);
                } else if (serverBoard[i][j].equals("O")) {
                    serverButtons[i][j].setBackground(Color.WHITE);
                }
            }
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
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
        SwingUtilities.invokeLater(Server::new);
    }
}
