package com.boot.user.vo;

import java.util.Objects;

public class RGBColor {
    private final int red;
    private final int green;
    private final int blue;

    public RGBColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RGBColor rgbColor = (RGBColor) o;
        return red == rgbColor.red && green == rgbColor.green && blue == rgbColor.blue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue);
    }
}
