package com.codecool.klondike;

public enum Suit {
    HEARTS("hearts", "red"), SPADES("spades", "black"), DIAMONDS("diamonds", "red"), CLUBS("clubs", "black");

    private final String name;
    private final String color;

    Suit(String name, String color) {
        this.name = name;
        this.color=color;
    }

    public String toString() {
        return name;
    }

    public String getColor() {
        return color;
    }

}
