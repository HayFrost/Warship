import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {
    private static final int GRID_SIZE = 10;
    private static final int NUM_SHIPS = 3;

    private JFrame frame;
    private JButton[][] clientButtons;
    private JButton[][] serverButtons;
    private String[][] clientBoard;
    private String[][] serverView;
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    private int clientShipsRemaining = NUM_SHIPS;
    private int serverShipsRemaining = NUM_SHIPS;
    private boolean isClientTurn = true;

    public Client() {
        initializeConnection();
        initializeGUI();
        clientBoard = initializeBoard();
        serverView = initializeBoard();
        placeShips(clientBoard, NUM_SHIPS);
        System.out.println("Grille du client initialisée :");
        printBoard(clientBoard);

        new Thread(this::receiveServerMoves).start();
    }

    private void initializeConnection() {
        try {
            client = new Socket("localhost", 4444);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            System.out.println("Connexion au serveur réussie!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Impossible de se connecter au serveur ", "Erreur", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeGUI() {
        frame = new JFrame("Bataille Navale (Client)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        clientButtons = new JButton[GRID_SIZE][GRID_SIZE];
        serverButtons = new JButton[GRID_SIZE][GRID_SIZE];

        JPanel clientGrid = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        JPanel serverGrid = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // Grille du client
                JButton clientButton = new JButton();
                clientButton.setBackground(Color.CYAN);
                clientButton.setEnabled(false);
                clientButtons[i][j] = clientButton;
                clientGrid.add(clientButton);


                JButton serverButton = new JButton();
                serverButton.setBackground(Color.CYAN);
                serverButton.addActionListener(new ServerButtonClickListener(i, j));
                serverButtons[i][j] = serverButton;
                serverGrid.add(serverButton);
            }
        }

        JButton quitButton = new JButton("Quitter");
        quitButton.addActionListener(e -> {
            closeConnection();
            frame.dispose();
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel gridsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        gridsPanel.add(clientGrid);
        gridsPanel.add(serverGrid);

        mainPanel.add(new JLabel("Grille coté Client (à gauche) et Grille coté Serveur (à droite)", SwingConstants.CENTER), BorderLayout.NORTH);
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
            if (!isClientTurn) {
                JOptionPane.showMessageDialog(frame, "C'est pas a toi de jouer !");
                return;
            }

            String move = x + "," + y;
            out.println(move);

            try {
                String response = in.readLine();
                System.out.println("Réponse du serveur : " + response);

                if (response.contains("BAM !!!")) {
                    serverButtons[x][y].setBackground(Color.RED);
                    serverView[x][y] = "X";
                    serverShipsRemaining--;
                } else if (response.contains("Hé non !")) {
                    serverButtons[x][y].setBackground(Color.WHITE);
                    serverView[x][y] = "O";
                }

                if (response.contains("Gagné!")) {
                    JOptionPane.showMessageDialog(frame, "Félicitations, la victoire navale est votre !!!");
                    closeConnection();
                    frame.dispose();
                }

                updateServerGrid();
                isClientTurn = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void receiveServerMoves() {
        try {
            while (true) {
                String serverMove = in.readLine();
                if (serverMove == null) {
                    System.out.println("Le serveur a interrompu la connexion.");
                    break;
                }

                System.out.println("Coup du serveur : " + serverMove);

                String response = processMove(clientBoard, serverMove);
                out.println(response);

                if (response.contains("BAM !!!")) {
                    clientShipsRemaining--;
                    if (clientShipsRemaining <= 0) {
                        JOptionPane.showMessageDialog(frame, "Ah nan la loose !!!");
                        break;
                    }
                }

                updateClientGrid();
                isClientTurn = true;
            }
        } catch (IOException e) {
            System.out.println("Connexion au serveur interrompue : " + e.getMessage());
        } finally {
            closeConnection();
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
                return "Tu as deja tiré ici";
            }
        } catch (Exception e) {
            return "ATTENTION JE TIRE !!!";
        }
    }

    private void updateClientGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (clientBoard[i][j].equals("X")) {
                    clientButtons[i][j].setBackground(Color.RED);
                } else if (clientBoard[i][j].equals("O")) {
                    clientButtons[i][j].setBackground(Color.WHITE);
                }
            }
        }
    }

    private void updateServerGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (serverView[i][j].equals("X")) {
                    serverButtons[i][j].setBackground(Color.RED);
                } else if (serverView[i][j].equals("O")) {
                    serverButtons[i][j].setBackground(Color.WHITE);
                }
            }
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

    private void closeConnection() {
        try {
            if (client != null) client.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
