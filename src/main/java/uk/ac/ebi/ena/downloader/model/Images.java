package uk.ac.ebi.ena.downloader.model;

import javafx.scene.paint.Color;

public enum Images {
    EXCLAMATION("exclamation.png", Color.RED),
    LOADING("loading.gif", Color.BLUE),
    TICK("tick.png", Color.GREEN),
    WARNING("warning.png", Color.ORANGE);

    private final String image;
    private final Color textColor;

    Images(String img, Color color) {
        this.image = img;
        this.textColor = color;
    }

    public String getImage() {
        return image;
    }

    public Color getTextColor() {
        return textColor;
    }
}
