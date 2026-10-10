package com.example.customguimod;

/** Presentation-only flight state; inventory updates stay in CardsGuiScreen. */
final class FlyingCard {
    double startX;
    double startY;
    double endX;
    double endY;
    double progress;
    int cardIndex;
    int color;

    FlyingCard(double startX, double startY, double endX, double endY,
               int cardIndex, int color) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.cardIndex = cardIndex;
        this.color = color;
    }

    void update() {
        progress += 0.12D;
        if (progress > 1.0D) progress = 1.0D;
    }

    double getX() {
        return startX + (endX - startX) * progress;
    }

    double getY() {
        return startY + (endY - startY) * progress;
    }

    boolean isDone() {
        return progress >= 1.0D;
    }
}
