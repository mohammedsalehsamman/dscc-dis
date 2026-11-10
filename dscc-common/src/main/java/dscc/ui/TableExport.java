package dscc.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;

/**
 * Export a table to CSV (opens in Excel) or print it (choose "Microsoft Print to PDF" to get a PDF).
 * Only the rows currently visible (after search/filter) are exported, in the on-screen order.
 */
public final class TableExport {

    private TableExport() {
    }

    public static void toCsv(Component parent, JTable table, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getPath() + ".csv");

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            out.print('﻿'); // BOM so Excel reads UTF-8 (Arabic names) correctly
            int cols = table.getColumnCount();
            for (int c = 0; c < cols; c++) {
                out.print(csv(table.getColumnName(c)));
                out.print(c < cols - 1 ? "," : "\r\n");
            }
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < cols; c++) {
                    Object v = table.getValueAt(r, c);
                    out.print(csv(v == null ? "" : v.toString()));
                    out.print(c < cols - 1 ? "," : "\r\n");
                }
            }
            Toast.success(parent, "Exported " + table.getRowCount() + " rows to " + file.getName());
        } catch (IOException e) {
            Toast.error(parent, "Export failed: " + e.getMessage());
        }
    }

    public static void print(Component parent, JTable table, String title) {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(title),
                    new MessageFormat("DSCC-DIS  -  page {0}"));
        } catch (PrinterException e) {
            Toast.error(parent, "Print failed: " + e.getMessage());
        }
    }

    private static String csv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
