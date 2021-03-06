package server;
import java.util.List;

import core.Card;
import core.GameOptions;
import core.Player;
import core.RoundDetails;
import core.Team;

public class HumanPlayer extends Player {
    private PlayerThread thread;
    
    public HumanPlayer(PlayerThread thread) {
        this.thread = thread;
    }
    
    @Override
    public void setId(String id) {
        super.setId(id);
        thread.setName("Player thread: " + id);
    }
    
    @Override
    public String realName() {
        return thread.getSocket().getInetAddress().toString();
    }
    
    @Override
    public boolean isHuman() {
        return true;
    }
    
    public PlayerThread getThread() {
        return thread;
    }
    
    public void setThread(PlayerThread thread) {
        this.thread = thread;
    }
    
    public void commandIdRequest(String version) {
        thread.sendCommand("IDREQUEST:" + version);
    }
    
    public void ping() {
        thread.sendCommand("PING");
    }

    @Override
    public void commandStart(GameOptions options) {
        thread.sendCommand("START:" + options);
    }
    
    @Override
    public void commandEndGame(Player player) {
        thread.sendCommand("STOP:" + "STRING " + player.getId().length() + ":" + player.getId());
    }
    
    @Override
    public void commandAddPlayers(List<? extends Player> players, List<? extends Player> kibitzers) {
        thread.sendCommand("ADDPLAYERS:" 
                + (players != null ? players.stream().map(p -> playerInfoString(p)).reduce("", (a, b) -> a + b) : "")
                + (kibitzers != null ? kibitzers.stream().map(p -> playerInfoString(p)).reduce("", (a, b) -> a + b) : ""));
    }

    @Override
    public void commandRemovePlayer(Player player) {
        thread.sendCommand("REMOVEPLAYER:" + "STRING " + player.getId().length() + ":" + player.getId());
    }

    @Override
    public void commandUpdatePlayers(List<? extends Player> players) {
        thread.sendCommand("UPDATEPLAYERS:"
                + (players != null ? players.stream().map(p -> playerInfoString(p)).reduce("", (a, b) -> a + b) : ""));
    }
    
    @Override
    public void commandUpdateTeams(List<Team> teams) {
        thread.sendCommand("UPDATETEAMS:"
                + (teams != null ? teams.stream().map(t -> teamInfoString(t)).reduce("", (a, b) -> a + b) : ""));
    }
    
    @Override
    public void commandUpdateOptions(GameOptions options) {
        thread.sendCommand("OPTIONS:" + options);
    }
    
    public String playerInfoString(Player player) {
        return "STRING " + player.getName().length() + ":" + player.getName() + ":"
                + "STRING " + player.getId().length() + ":" + player.getId() + ":"
                + player.getIndex() + ":"
                + player.isHuman() + ":"
                + player.isHost() + ":"
                + player.isDisconnected() + ":"
                + player.isKicked() + ":"
                + player.isKibitzer() + ":"
                + (this == player) + ":"
                + player.getTeam() + ":";
    }
    
    public String teamInfoString(Team team) {
        return "STRING " + team.getName().length() + ":" + team.getName() + ":"
                + team.getIndex() + ":";
    }

    @Override
    public void commandStatePlayer(Player player) {
        thread.sendCommand("STATEPLAYER:" +
                player.getIndex() + ":" + 
                player.hasBid() + ":" + 
                player.getBid() + ":" + 
                player.getTaken() + ":" + 
                player.getLastTrick() + ":" + 
                player.getTrick() + ":");
        thread.sendCommand(player.getBids().stream()
                .map(bid -> bid + ":")
                .reduce("STATEPLAYERBIDS:" + player.getIndex() + ":", 
                        (sofar, bid) -> sofar + bid));
        thread.sendCommand(player.getScores().stream()
                .map(score -> score + ":")
                .reduce("STATEPLAYERSCORES:" + player.getIndex() + ":", 
                        (sofar, score) -> sofar + score));
    }

    @Override
    public void commandDealerLeader(int dealer, int leader) {
        thread.sendCommand("STATEDEALERLEADER:" + dealer + ":" + leader);
    }

    @Override
    public void commandUpdateRounds(List<RoundDetails> rounds, int roundNumber) {
        thread.sendCommand(rounds.stream()
                .map(r -> r.getDealer() + ":" + r.getHandSize() + ":")
                .reduce("UPDATEROUNDS:" + roundNumber + ":", 
                        (sofar, cString) -> sofar + cString));
    }

    @Override
    public void commandDeal(List<Player> players, Card trump) {
        for (Player p : players) {
            if (p.isKicked()) {
                thread.sendCommand("DEAL:" + p.getIndex());
            } else {
                thread.sendCommand(
                        p.getHand().stream()
                        .map(card -> 
                            (this == p || isKibitzer() || p.isHandRevealed() ? card.toString() : "0") + ":")
                        .reduce("DEAL:" + p.getIndex() + ":", 
                                (sofar, cString) -> sofar + cString));
            }
        }
        thread.sendCommand("TRUMP:" + trump.toString());
    }

    @Override
    public void commandRedeal() {
        thread.sendCommand("REDEAL");
    }

    @Override
    public void commandBid(int index) {
        thread.sendCommand("BID:" + index);
    }

    @Override
    public void commandPlay(int index) {
        thread.sendCommand("PLAY:" + index);
    }

    @Override
    public void commandBidReport(int index, int bid) {
        thread.sendCommand("BIDREPORT:" + index + ":" + bid);
    }

    @Override
    public void commandPlayReport(int index, Card card) {
        thread.sendCommand("PLAYREPORT:" + index + ":" + card);
    }

    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        thread.sendCommand("TRICKWINNER:" + index);
    }
    
    @Override
    public void commandUndoBidReport(int index) {
        thread.sendCommand("UNDOBID:" + index);
    }
    
    @Override
    public void commandClaimRequest(int index) {
        thread.sendCommand("CLAIMREQUEST:" + index);
    }
    
    @Override
    public void commandClaimResult(boolean accept) {
        thread.sendCommand("CLAIMRESULT:" + (accept ? "ACCEPT" : "REFUSE"));
    }

    @Override
    public void commandNewScores(List<Integer> scores) {
        thread.sendCommand(scores.stream()
                .map(score -> score == null ? "-:" : score + ":")
                .reduce("REPORTSCORES:", (a, b) -> a + b));
    }
    
    @Override
    public void commandPostGameTrumps(List<Card> trumps) {
        thread.sendCommand(trumps.stream().map(c -> c + ":").reduce(
                "POSTGAMETRUMPS:", (c1, c2) -> c1 + c2));
    }
    
    @Override
    public void commandPostGameTakens(List<Player> players) {
        for (Player player : players) {
            thread.sendCommand(player.getTakens()
                    .stream()
                    .map(t -> t + ":")
                    .reduce("POSTGAMETAKENS:" + player.getIndex() + ":", (c1, c2) -> c1 + c2));
        }
    }

    @Override
    public void commandPostGameHands(List<Player> players) {
        for (Player player : players) {
            for (List<Card> hand : player.getHands()) {
                thread.sendCommand(
                        hand.stream().map(c -> c + ":").reduce(
                                "POSTGAMEHAND:" + player.getIndex() + ":", (c1, c2) -> c1 + c2));
            }
        }
    }

    @Override
    public void commandPostGame() {
        thread.sendCommand("FINALSCORES");
    }
    
    @Override
    public void commandPostGameFile(String file) {
        /*int step = 100;
        for (int i = 0; i < file.length(); i += step) {
            String piece = file.substring(i, Math.min(i + step, file.length()));
            thread.sendCommand("POSTGAMEFILEPIECE:" + "STRING " + piece.length() + ":" + piece);
        }
        thread.sendCommand("POSTGAMEFILEPIECE:STRING 0:");*/
        thread.sendCommand("POSTGAMEFILE:" + "STRING " + file.length() + ":" + file);
    }

    @Override
    public void commandChat(String text) {
        thread.sendCommand("CHAT:STRING " + text.length() + ":" + text);
    }

    @Override
    public void commandKick() {
        thread.sendCommand("KICK");
        thread.endThread();
    }
    
    @Override
    public void commandPoke() {
        thread.sendCommand("POKE");
    }
}
