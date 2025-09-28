package com.autoheal.model;

/**
 * Represents a position and size of an element
 */
public class Position {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Position(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    @Override
    public String toString() {
        return String.format("Position{x=%d, y=%d, width=%d, height=%d}", x, y, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return x == position.x && y == position.y && width == position.width && height == position.height;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, width, height);
    }
}