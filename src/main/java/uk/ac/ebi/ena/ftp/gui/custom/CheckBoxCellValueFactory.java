package uk.ac.ebi.ena.ftp.gui.custom;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

/**
 * Created by suranj on 27/05/2016.
 */
public class CheckBoxCellValueFactory implements Callback<TableColumn.CellDataFeatures<RemoteFile, Boolean>, ObservableValue<Boolean>> {

    @Override
    public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<RemoteFile, Boolean> rdf) {
        return rdf.getValue().isDownload();
    }
}
