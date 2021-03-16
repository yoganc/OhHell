package server;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import core.Card;
import core.GameOptions;
import core.OhHellCore;
import core.Player;
import common.OhcButton;
import common.FileTools;
import common.OhcScrollPane;
import common.OhcTextField;

public class GameServer extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private String version;
    private boolean updateChecked = false;
    private String newVersion;
    
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new OhcTextField("Port");
    private JButton goButton = new OhcButton("Go");
    private OhcButton updateButton = new OhcButton("Check for update");
    private JTextArea logTextArea = new JTextArea();
    private JScrollPane logScrollPane = new OhcScrollPane(logTextArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private DefaultListModel<String> playersListModel = new DefaultListModel<>();
    private List<Player> playersInList = new ArrayList<>();
    private JList<String> playersJList = new JList<>(playersListModel);
    private JScrollPane playersScrollPane = new OhcScrollPane(playersJList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JButton dcButton = new OhcButton("DC Player");
    private JButton kickButton = new OhcButton("Kick Player");
    
    private JFrame popUpFrame = new JFrame();
    
    private OhHellCore core;
    
    private int port = -1;
    private ServerSocket serverSocket;
    private ConnectionFinder finder;
    
    private List<Player> players = new ArrayList<>();
    private List<Player> kibitzers = new ArrayList<>();
    private GameOptions options;
    
    private Random random = new Random();
    
    public GameServer() {}
    
    public void execute(boolean deleteUpdater) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        
        setIconImage(FileTools.loadImage("resources/icon/cw.png", this));
        
        setSize(640, 480);
        setResizable(false);
        setLayout(new BorderLayout());
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout(3));
        northPanel.add(portLabel);
        portField.setPreferredSize(new Dimension(200, 40));
        portField.setText("6066");
        northPanel.add(portField);
        goButton.setPreferredSize(new Dimension(153, 40));
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPressed();
            }
        });
        northPanel.add(goButton);
        updateButton.setPreferredSize(new Dimension(200, 40));
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePressed();
            }
        });
        northPanel.add(updateButton);
        add(northPanel, BorderLayout.NORTH);
        
        logTextArea.setEditable(false);
        add(logScrollPane);
        
        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension(210, 400));
        eastPanel.setLayout(new FlowLayout(2));
        playersScrollPane.setPreferredSize(new Dimension(200, 320));
        eastPanel.add(playersScrollPane);
        dcButton.setPreferredSize(new Dimension(97, 40));
        dcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                forceRemovePlayer(
                        (HumanPlayer) playersInList.get(playersJList.getSelectedIndex()), 
                        false);
            }
        });
        eastPanel.add(dcButton);
        kickButton.setPreferredSize(new Dimension(97, 40));
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                forceRemovePlayer(
                        (HumanPlayer) playersInList.get(playersJList.getSelectedIndex()), 
                        true);
            }
        });
        eastPanel.add(kickButton);
        add(eastPanel, BorderLayout.EAST);

        BufferedReader versionReader = FileTools.getInternalFile("version", this);
        try {
            version = versionReader.readLine();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        setTitle("Oh Hell Server (v" + version + ")");
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                revalidate();
            }
        });
        
        if (deleteUpdater) {
            try {
                FileTools.deleteFile(getDirectory() + "/serverupdater.jar");
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        }
        checkForUpdates();
    }
    
    public void goPressed() {
        try {
            core = new OhHellCore(true) {
                @Override
                public void stopGame() {
                    super.stopGame();
                    updatePlayersList();
                }
            };
            core.setPlayers(players);
            core.setKibitzers(kibitzers);
            
            port = Integer.parseInt(portField.getText());
            if (serverSocket != null) {
                serverSocket.close();
            }
            serverSocket = new ServerSocket(port);
            finder = new ConnectionFinder(serverSocket, this);
            finder.start();
            logMessage("Server started on port " + port + ". Waiting for players.");
        } catch(NumberFormatException e1) {
            JOptionPane.showMessageDialog(popUpFrame, "Invalid port");
        } catch(IOException e1) {
            JOptionPane.showMessageDialog(popUpFrame, "Port unavailable");
        }
    }
    
    public boolean gameStarted() {
        return core.getGameStarted();
    }
    
    public void startGame(int robotCount, GameOptions options) {
        this.options = options;
        core.startGame(robotCount, options, null);
    }
    
    public void updatePlayersList() {
        playersListModel.clear();
        playersInList.clear();
        for (Player player : players) {
            if (player.isHuman()) {
                playersListModel.addElement(
                        player.getId()
                        + (player.isHost() ? " (host)" : "")
                        + (player.isDisconnected() && !player.isKicked() ? " (DCed)" : "")
                        + (player.isKicked() ? " (kicked)" : "")
                        + (player.isKibitzer() ? " (kibitzer)" : ""));
                playersInList.add(player);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playersJList.updateUI();
            }
        });
    }

    public void connectPlayer(Socket socket) {
        PlayerThread thread = new PlayerThread(socket, this);
        HumanPlayer player = new HumanPlayer(thread);
        thread.setPlayer(player);
        thread.start();
    }
    
    public void requestId(HumanPlayer player) {
        player.commandIdRequest(version);
    }
    
    public void joinPlayer(HumanPlayer player, String id) {
        player.setName(id);
        player.setId(id);
        player.getThread().setName("Player thread: " + id);
        player.setJoined(true);
        
        boolean reconnect = false;
        for (Player p : players) {
            if (p.getId().equals(player.getId()) && !p.isKicked()) {
                //players.remove(player);
                
                p.commandKick();
                ((HumanPlayer) p).getThread().endThread();
                ((HumanPlayer) p).setThread(player.getThread());
                ((HumanPlayer) p).getThread().setPlayer((HumanPlayer) p);
                ((HumanPlayer) p).setDisconnected(false);
                ((HumanPlayer) p).resetKickVotes();
                player = (HumanPlayer) p;
                
                for (Player p1 : players) {
                    if (p1 != player) {
                        p1.commandUpdatePlayers(Arrays.asList(player));
                    }
                }
                for (Player p1 : kibitzers) {
                    if (p1 != player) {
                        p1.commandUpdatePlayers(Arrays.asList(player));
                    }
                }
                
                reconnect = true;
                break;
            }
        }
        
        if (reconnect) {
            logMessage("Player reconnected: " + player.getId() + " at " + player.getThread().getSocket().getInetAddress());
        } else {
            logMessage("Player connected: " + player.getId() + " at " + player.getThread().getSocket().getInetAddress());
        }
        
        if (gameStarted()) {
            if (!reconnect) {
                player.setKibitzer(true);
                kibitzers.add(player);
                for (Player p : players) {
                    if (p != player) {
                        p.commandAddPlayers(null, Arrays.asList(player));
                    }
                }
                for (Player p : kibitzers) {
                    if (p != player) {
                        p.commandAddPlayers(null, Arrays.asList(player));
                    }
                }
            }
            player.commandAddPlayers(players, kibitzers);
            player.commandStart(options);
            core.sendFullGameState(player);
        } else {
            if (!reconnect) {
                players.add(player);
                player.setIndex(players.size() - 1);
                if (players.size() == 1) {
                    player.setHost(true);
                }
                for (Player p : players) {
                    if (p != player) {
                        p.commandAddPlayers(Arrays.asList(player), null);
                    }
                }
                for (Player p : kibitzers) {
                    if (p != player) {
                        p.commandAddPlayers(Arrays.asList(player), null);
                    }
                }
            }
            player.commandAddPlayers(players, kibitzers);
        }
        
        updatePlayersList();
    }
    
    public void renamePlayer(Player player, String name) {
        player.setName(name);
        for (Player p : players) {
            p.commandUpdatePlayers(Arrays.asList(player));
        }
    }
    
    public void addKickVote(int index, HumanPlayer fromPlayer) {
        HumanPlayer player = (HumanPlayer) players.get(index);
        if (player.isDisconnected()) {
            player.addKickVote(fromPlayer);
            if (player.getNumberOfKickVotes() * 2
                    >= players.stream()
                    .filter(p -> !p.isDisconnected() && !p.isKicked() && p.isHuman())
                    .count()) { 
                forceRemovePlayer(player, true);
            }
        }
    }
    
    public void setKibitzer(Player player, boolean kibitzer) {
        boolean wasKibitzer = player.isKibitzer();
        player.setKibitzer(kibitzer);
        if (!wasKibitzer && kibitzer) {
            players.remove(player);
            for (int i = player.getIndex(); i < players.size(); i++) {
                players.get(i).setIndex(i);
            }
            kibitzers.add(player);
        } else if (wasKibitzer && !kibitzer) {
            players.add(player);
            player.setIndex(players.size() - 1);
            kibitzers.remove(player);
        }

        for (Player p : players) {
            p.commandRemovePlayer(player);
        }
        for (Player p : kibitzers) {
            p.commandRemovePlayer(player);
        }
        for (Player p : players) {
            p.commandAddPlayers(Arrays.asList(player), null);
        }
        for (Player p : kibitzers) {
            p.commandAddPlayers(Arrays.asList(player), null);
        }
    }
    
    public void pokePlayer() {
        core.pokePlayer();
    }
    
    public void logMessage(String s) {
        logTextArea.append(s + "\n");
    }
    
    public void forceRemovePlayer(HumanPlayer player, boolean kick) {
        player.commandKick();
        removePlayer(player, kick);
        player.getThread().endThread();
    }
    
    public void removePlayer(HumanPlayer player, boolean kick) {
        if ((!players.contains(player)
                || player.isKicked() 
                || player.isDisconnected() && !kick)
                && !kibitzers.contains(player)) {
            return;
        }
        
        logMessage("Player " + (kick ? "kicked" : "disconnected") + ": " 
                + player.getName() + " at " + player.getThread().getSocket().getInetAddress());
        player.setDisconnected(!kick);
        player.setKicked(kick);
        
        // Change host if necessary
        if (player.isHost()) {
            List<Player> potentialHosts = players.stream()
                    .filter(p -> p.isHuman() && !p.isDisconnected() && !p.isKicked())
                    .collect(Collectors.toList());
            if (!potentialHosts.isEmpty()) {
                Player newHost = potentialHosts.get(random.nextInt(potentialHosts.size()));
                player.setHost(false);
                newHost.setHost(true);
                for (Player p : players) {
                    p.commandUpdatePlayers(Arrays.asList(newHost));
                }
                for (Player p : kibitzers) {
                    p.commandUpdatePlayers(Arrays.asList(newHost));
                }
            }
        }
        
        // Remove if game hasn't started or player is kibitzer
        // Update otherwise
        if (!gameStarted() || !player.isJoined() || player.isKibitzer()) {
            if (!player.isKibitzer()) {
                players.remove(player);
                for (int i = player.getIndex(); i < players.size(); i++) {
                    players.get(i).setIndex(i);
                }
            } else {
                kibitzers.remove(player);
            }
            for (Player p : players) {
                if (p != player) {
                    p.commandRemovePlayer(player);
                }
            }
            for (Player p : kibitzers) {
                if (p != player) {
                    p.commandRemovePlayer(player);
                }
            }
        } else {
            for (Player p : players) {
                if (p != player) {
                    p.commandUpdatePlayers(Arrays.asList(player));
                }
            }
            for (Player p : kibitzers) {
                if (p != player) {
                    p.commandUpdatePlayers(Arrays.asList(player));
                }
            }
        }
        
        // Restart round if kicked
        if (kick && gameStarted() && !player.isKibitzer()) {
            core.reportKick(player.getIndex());
        }
        
        updatePlayersList();
    }
    
    public void makeBid(Player player, int bid) {
        core.incomingBid(player, bid);
    }
    
    public void makePlay(Player player, Card card) {
        core.incomingPlay(player, card);
    }
    
    public void processUndoBid(Player player) {
        core.processUndoBid(player);
    }
    
    public void processClaim(Player player) {
        core.processClaim(player);
    }
    
    public void processClaimResponse(Player player, boolean accept) {
        core.processClaimResponse(player, accept);
    }
    
    public void sendChat(Player sender, String recipient, String text) {
        core.sendChat(sender, recipient, text);
    }
    
    public void requestEndGame(Player player) {
        core.requestEndGame(player);
    }
    
    public void checkForUpdates() {
        newVersion = FileTools.getCurrentVersion();
        updateChecked = true;
        if (newVersion.equals(version)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateButton.setText("Version up to date");
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateButton.setAlert(true);
                    updateButton.setText("Download v" + newVersion);
                }
            });
        }
    }
    
    public void updatePressed() {
        System.out.println("UPDATE BUTTON PRESSED");
        if (!updateChecked) {
            checkForUpdates();
        } else {
            try {
                String path = getDirectory() + "/serverupdater.jar";
                FileTools.downloadFile(
                        "https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/updater.jar", 
                        getDirectory() + "/serverupdater.jar");
                
                if (new File(path).exists()) {
                    if (FileTools.isUnix()) {
                        FileTools.runTerminalCommand(new String[] {
                                "chmod",
                                "777",
                                path
                        }, false);
                    }
                    FileTools.runTerminalCommand(new String[] {
                            FileTools.cmdJava(),
                            "-jar",
                            path,
                            newVersion,
                            "OhHellServer.jar",
                            getFileName()
                    }, false);
                    dispose();
                    System.exit(0);
                } else {
                    System.out.println("Error: Failed to download updater.");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getDirectory() throws URISyntaxException {
        return new File(GameServer.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getParent();
    }
    
    public String getFileName() throws URISyntaxException {
        return new File(GameServer.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getPath();
    }
    
    public static void main(String[] args) {
        boolean deleteUpdater = false;
        
        for (String arg : args) {
            if (arg.equals("-deleteupdater")) {
                deleteUpdater = true;
            }
        }
        
        GameServer server = new GameServer();
        server.execute(deleteUpdater);
    }
}