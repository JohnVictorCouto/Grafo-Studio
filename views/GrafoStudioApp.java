package views;

import javax.swing.SwingUtilities;

public final class GrafoStudioApp {
    private GrafoStudioApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().show());
    }
}
