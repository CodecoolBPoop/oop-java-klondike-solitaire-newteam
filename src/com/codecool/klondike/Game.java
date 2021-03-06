package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
        else if (e.getClickCount() % 2 == 0 && !e.isConsumed() && card.getContainingPile().getPileType() != Pile.PileType.STOCK) {
            for (Pile pile : foundationPiles){
                if (card.getRank().equals(Rank.ACE) && pile.isEmpty()){
                    flipNextCard(card);
                    card.moveToPile(pile);
                } else if (pile.getTopCard() != null && card.isSameSuit(card, pile.getTopCard()) && (pile.getTopCard().getRank().getValue()+1) == card.getRank().getValue()){
                    flipNextCard(card);
                    card.moveToPile(pile);
                }
            }
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);
        if(activePile.getPileType() == Pile.PileType.TABLEAU)
        for (Card item : activePile.getCards()){
            if (!item.isFaceDown() && item.getRank().getValue() < card.getRank().getValue()){
                draggedCards.add(item);
            }
        }

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        for (int i = 0; i < draggedCards.size(); i++) {
                draggedCards.get(i).setTranslateX(offsetX);
                draggedCards.get(i).setTranslateY(offsetY);
                draggedCards.get(i).getDropShadow().setOffsetX(10);
                draggedCards.get(i).getDropShadow().setOffsetY(10);
                draggedCards.get(i).toFront();
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        if (pile == null) {
            pile = getValidIntersectingPile(card, foundationPiles);
        }
        if (pile != null) {
            handleValidMove(card, pile);
            int cardIndex = deck.indexOf(card);
            if(cardIndex > 0){
                Card nextCard = deck.get(cardIndex-1);
                if (card.getContainingPile() == nextCard.getContainingPile()) {
                    nextCard.flip();
                    addMouseEventHandlers(nextCard);
                }
                else if (card.getContainingPile().getPileType() == Pile.PileType.DISCARD && discardPile.numOfCards() == 1){
                    Card nextStockCard = stockPile.getTopCard();
                    nextStockCard.moveToPile(discardPile);
                    nextStockCard.flip();
                    nextStockCard.setMouseTransparent(false);
                }
            }
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
        if (isGameWon() == false) {
            return;
        } else
            restartGame();
    };

    public boolean isGameWon() {
        boolean gameWon = true;
        for (Pile pile : foundationPiles) {
            if (pile.numOfCards() != 13 || pile.isEmpty()) {
                gameWon = false;
            }
        }
        return gameWon;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        initRestart();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        if(stockPile.isEmpty()) {
            for (Card card : discardPile.getCards()) {
                stockPile.addCard(card);
                card.flip();
            }
            stockPile.reversePile();
            discardPile.clear();
        }
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType() == Pile.PileType.FOUNDATION && destPile.isEmpty() && card.getRank() == Rank.ACE )  {
            return true;
        }
        else if (!destPile.isEmpty() && destPile.getPileType() == Pile.PileType.FOUNDATION && card.isSameSuit(card, destPile.getTopCard())
                && destPile.getTopCard().getRank().getValue() +1 == card.getRank().getValue())   {
            return true;
        }

        else if (destPile.getPileType() == Pile.PileType.TABLEAU && destPile.isEmpty() && card.getRank() == Rank.KING ) {
            return true;
        }
        else if (!destPile.isEmpty() && destPile.getPileType() == Pile.PileType.TABLEAU && card.isOppositeColor(card, destPile.getTopCard())
                && destPile.getTopCard().getRank().getValue() -1 == card.getRank().getValue())   {
            return true;
        }
        else {
            return false;
        }
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }

    private void flipNextCard(Card card) {
        int cardIndex = deck.indexOf(card);
        if(cardIndex > 0) {
            Card nextCard = deck.get(cardIndex - 1);
            if (card.getContainingPile() == nextCard.getContainingPile()) {
                nextCard.flip();
                addMouseEventHandlers(nextCard);
            }
        }
    }

    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        int turn = 0;
        Iterator<Card> deckIterator = deck.iterator();
        while(turn < 7){
            for (int i = 0; i < turn + 1; i++) {
                Card card = deckIterator.next();
                tableauPiles.get(turn).addCard(card);
                getChildren().add(card);
                if (i == turn) {
                    card.flip();
                    addMouseEventHandlers(card);
                }
            }
            turn++;
        }
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void initRestart() {
        Button restart_btn = new Button();
        restart_btn.setLayoutX(0);
        restart_btn.setLayoutY(0);
        restart_btn.setPrefWidth(80);
        restart_btn.setPrefHeight(50);
        restart_btn.setText("Restart");
        restart_btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                restartGame();
            }
        });
        getChildren().add(restart_btn);
    }

    public void restartGame() {
        stockPile.clear();
        discardPile.clear();
        foundationPiles.clear();
        tableauPiles.clear();
        getChildren().clear();
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        initRestart();
    }
}

