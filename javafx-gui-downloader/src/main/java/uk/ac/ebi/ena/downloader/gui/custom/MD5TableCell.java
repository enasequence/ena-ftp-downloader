package uk.ac.ebi.ena.downloader.gui.custom;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by suranj on 10/03/2017.
 */
public class MD5TableCell<RemoteFile> extends TableCell<RemoteFile, String> {

    public static final String SUCCESS_ICON = "accept";
    public static final String LOADING_ICON = "loading";
    public static final String ERROR_ICON = "cross";
    private final static Logger log = LoggerFactory.getLogger(MD5TableCell.class);
    VBox vb;
    ImageView imageView;

    public MD5TableCell() {
        if (StringUtils.equals(this.getItem(), "N/A")) {
            setText("N/A");
        } else {
            vb = new VBox();
            vb.setAlignment(Pos.CENTER);
            imageView = new ImageView();
            imageView.setFitHeight(20);
            imageView.setFitWidth(20);
            vb.getChildren().add(imageView);
            setGraphic(vb);
        }
    }

    public static <RemoteFile> Callback<TableColumn<RemoteFile, String>, TableCell<RemoteFile, String>> forTableColumn() {
        return new Callback<TableColumn<RemoteFile, String>, TableCell<RemoteFile, String>>() {
            @Override
            public TableCell<RemoteFile, String> call(TableColumn<RemoteFile, String> param) {
                return new MD5TableCell<>();
            }
        };
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (item.startsWith(ERROR_ICON)) {
                String[] parts = item.split("\\:");
                imageView.setImage(new Image(parts[0] + ".png"));
                Tooltip tooltip = new Tooltip(parts.length > 1 ? StringUtils.substringAfter(item, ":").trim() : "File did not download correctly. Please try again later.");
                tooltip.setWrapText(true);
                Tooltip.install(imageView, tooltip);
            } else if (SUCCESS_ICON.equals(item)) {
                imageView.setImage(new Image(item + ".png"));
                Tooltip.install(imageView, new Tooltip("File fully downloaded. MD5 checksum verified."));
            } else if (LOADING_ICON.equals(item)) {
                imageView.setImage(new Image(item + ".gif"));
                Tooltip.install(imageView, new Tooltip("Calculating MD5."));
            }
            setGraphic(vb);
        }
    }
}
