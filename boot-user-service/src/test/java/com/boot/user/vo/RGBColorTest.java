package com.boot.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class RGBColorTest {

    @Test
    @DisplayName("vo_비교_테스트")
    public void vo_비교_테스트() {
        RGBColor color1 = new RGBColor(124, 1, 0);
        RGBColor color2 = new RGBColor(124, 1, 0);

        boolean actual = color1.equals(color2);

        assert(color1.equals(color2));
    }

    @Test
    @DisplayName("string_비교_테스트")
    public void string_비교_테스트() {
        String str1 = new String("hello");
        String str2 = new String("hello");

        System.out.printf("%s : %b\n", "str1==str2", (str1==str2));
        System.out.printf("%s : %b\n", "str1.equals(str2)", (str1.equals(str2)));
    }

    @Test
    @DisplayName("set_비교_테스트")
    public void set_비교_테스트() {
        Set<Card> cardSet = new HashSet<>();
        cardSet.add(new Card("Hyundai","0000-0000-0000"));
        cardSet.add(new Card("Hyundai","0000-0000-0000"));

        for (Card card : cardSet) {
            System.out.println(card.hashCode());
        }
        System.out.println("size : "+ cardSet.size());
    }
}
