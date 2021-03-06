package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Deck {
    private List<Card> deck = new ArrayList<Card>();
    int D = 1;
    
    private List<List<Card>> played = new ArrayList<>();
    private HashMap<String, Integer> playCounts = new HashMap<>();
    Random random = new Random();
    
    public Deck() {}
    
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
    
    public void setD(int D) {
        this.D = D;
    }
    
    public int getD() {
        return D;
    }
    
    public void initialize() {
        deck = new ArrayList<>();
        for (int d = 1; d <= D; d++) {
            for (int suit = 0; suit < 4; suit++) {
                for (int num = 2; num <= 14; num++) {
                    deck.add(new Card(num, suit));
                }
            }
        }
        
        played = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            played.add(new LinkedList<>());
        }
        playCounts = new HashMap<>();
    }
    
    public List<List<Card>> deal(int numHands, int handSize) {
        if (numHands * handSize + 1 > deck.size()) {
            throw new IllegalCoreStateException("Attempted to deal " + handSize + " cards to " + numHands + " players.");
        }
        List<List<Card>> out = new ArrayList<List<Card>>();
        for (int i = 0; i < numHands + 1; i++) {
            out.add(new ArrayList<Card>());
        }
        for (int j = 0; j < handSize; j++) {
            for (int i = 0; i < numHands; i++) {
                insert(out.get(i), deck.remove(random.nextInt(deck.size())));
            }
        }
        out.get(numHands).add(deck.remove(random.nextInt(deck.size())));
        return out;
    }
    
    private void insert(List<Card> hand, Card c) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isGreaterThanSort(c)) {
                hand.add(i, c);
                return;
            }
        }
        hand.add(c);
    }
    
    public void playCard(Card card) {
        if (!card.isEmpty()) {
            played.get(card.getSuit()).add(card);
            String cardName = card.toString();
            if (playCounts.get(cardName) == null) {
                playCounts.put(cardName, 1);
            } else {
                playCounts.put(cardName, playCounts.get(cardName) + 1);
            }
        }
    }
    
    public int cardsLeftOfSuit(Card card, List<List<Card>> additionalPlayeds) {
        return cardsLeftOfSuit(card.getSuit(), additionalPlayeds);
    }
    
    public int cardsLeftOfSuit(int suit, List<List<Card>> additionalPlayeds) {
        return cardsLeftOfSuit(suit, additionalPlayeds, false);
    }
    
    public int cardsLeftOfSuit(int suit, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
        int count = 0;
        for (List<Card> additionalPlayed : additionalPlayeds) {
            for (Card c : additionalPlayed) {
                if (c != null && c.getSuit() == suit) {
                    count++;
                }
            }
        }
        return 13 * D - (ignorePlayed ? 0 : played.get(suit).size()) - count;
    }
    
    public int adjustedCardValueSmall(Card card, List<List<Card>> additionalPlayeds) {
        return adjustedCardValueSmall(card, additionalPlayeds, false);
    }
    
    public int adjustedCardValueSmall(Card card, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
        int val = (card.getNum() - 2) * D;
        if (!ignorePlayed) {
            for (Card c : played.get(card.getSuit())) {
                if (c.getNum() >= card.getNum()) {
                    val++;
                }
            }
        }
        for (List<Card> additionalPlayed : additionalPlayeds) {
            for (Card c : additionalPlayed) {
                if (c != null && c.getNum() >= card.getNum() && c.getSuit() == card.getSuit()) {
                    val++;
                }
            }
        }
        return val;
    }
    
    public int matchingCardsLeft(Card card, List<List<Card>> additionalPlayeds) {
        return matchingCardsLeft(card, additionalPlayeds, false);
    }
    
    public int matchingCardsLeft(Card card, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
        if (D == 1) {
            return 0;
        } else {
            int count = 0;
            if (!ignorePlayed) {
                if (playCounts.get(card.toString()) != null) {
                    count += playCounts.get(card.toString());
                }
            }
            for (List<Card> additionalPlayed : additionalPlayeds) {
                for (Card c : additionalPlayed) {
                    if (c != null && c.matches(card)) {
                        count++;
                    }
                }
            }
            return D - count;
        }
    }
    
    public List<Card> getPlayedCards() {
        List<Card> ans = new LinkedList<>();
        for (List<Card> playedSuit : played) {
            ans.addAll(playedSuit);
        }
        return ans;
    }
}