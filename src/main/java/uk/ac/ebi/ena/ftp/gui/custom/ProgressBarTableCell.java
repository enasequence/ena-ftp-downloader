package uk.ac.ebi.ena.ftp.gui.custom;

import com.sun.javafx.binding.SelectBinding;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.util.Callback;

/**
 * Created by suranj on 01/06/2016.
 */
public class ProgressBarTableCell<S> extends TableCell<S, Double> {
    public static <S> Callback<TableColumn<S, Double>, TableCell<S, Double>> forTableColumn() {
        return new Callback<TableColumn<S, Double>, TableCell<S, Double>>() {
            @Override
            public TableCell<S, Double> call(TableColumn<S, Double> param) {
                return new ProgressBarTableCell<>();
            }
        };
    }

    private final ProgressBar progressBar;
    private ObservableValue observable;

    public ProgressBarTableCell() {
        this.getStyleClass().add("progress-bar-table-cell");
        this.progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(300);
        setGraphic(progressBar);
    }

    @Override public void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
        } else {
            progressBar.progressProperty().unbind();
            observable = getTableColumn().getCellObservableValue(getIndex());
            if (observable != null) {
                progressBar.progressProperty().bind(observable);
            } else {
                progressBar.setProgress(item);
            }

            setGraphic(progressBar);
        }
    }
}
