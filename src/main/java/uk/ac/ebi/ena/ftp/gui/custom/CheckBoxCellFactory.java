package uk.ac.ebi.ena.ftp.gui.custom;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

/**
 * Created by suranj on 27/05/2016.
 */
public class CheckBoxCellFactory implements Callback<TableColumn<RemoteFile, Boolean>, TableCell<RemoteFile, Boolean>> {

    @Override
    public TableCell<RemoteFile, Boolean> call(TableColumn<RemoteFile, Boolean> p) {
        return new CheckBoxTableCell<>();
    }
}