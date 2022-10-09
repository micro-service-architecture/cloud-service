package com.boot.user.vo;

import java.util.Objects;

public class Card {
    String name;
    String number;
    public Card(String name, String number) {
        this.name = name;
        this.number = number;
    }

    @Override
    public String toString() {
        return "Card{" +
                "name='" + name + '\'' +
                ", number=" + number +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(name, card.name) && Objects.equals(number, card.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, number);
    }
}
