package org.example.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the GUI fat JAR.
 * Sets the system look-and-feel and opens {@link MainWindow}.
 */
public class GuiMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default L&F
        }
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
