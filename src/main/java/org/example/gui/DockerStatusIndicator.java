package org.example.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Small indicator widget showing the current Docker/SonarQube container status
 * as a coloured dot followed by a human-readable label.
 *
 * <p>Thread-safe: {@link #setState} may be called from any thread.</p>
 */
public class DockerStatusIndicator extends JPanel {

    /** Possible runtime states of the SonarQube Docker container. */
    public enum State {
        IDLE    ("Docker: —",          Color.LIGHT_GRAY),
        CHECKING("Docker: prüft…",     new Color(200, 180, 0)),
        STARTING("Docker: startet…",   Color.ORANGE),
        READY   ("Docker: bereit",     new Color(0, 160, 0)),
        STOPPING("Docker: stoppt…",    Color.ORANGE),
        STOPPED ("Docker: gestoppt",   Color.LIGHT_GRAY),
        ERROR   ("Docker: Fehler",     Color.RED);

        private final String label;
        private final Color  color;

        State(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        /** Human-readable label shown next to the dot. */
        public String getLabel() { return label; }

        /** Dot colour representing this state. */
        public Color  getColor() { return color; }
    }

    private static final int DOT_SIZE = 12;

    private volatile State currentState = State.IDLE;

    private final JLabel  statusLabel;
    private final DotPanel dot;

    /** Creates the indicator in {@link State#IDLE}. */
    public DockerStatusIndicator() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        dot         = new DotPanel();
        statusLabel = new JLabel(State.IDLE.label);
        add(dot);
        add(statusLabel);
    }

    /**
     * Updates the displayed state. Safe to call from any thread.
     *
     * @param state new state; {@code null} is silently ignored
     */
    public void setState(State state) {
        if (state == null) {
            return;
        }
        currentState = state;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(state.label);
            dot.repaint();
        });
    }

    /**
     * Returns the most recently applied state.
     *
     * @return current state
     */
    public State getState() {
        return currentState;
    }

    // -------------------------------------------------------------------------
    // Inner dot panel
    // -------------------------------------------------------------------------

    private class DotPanel extends JPanel {

        DotPanel() {
            setPreferredSize(new Dimension(DOT_SIZE, DOT_SIZE));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int y = Math.max(0, (getHeight() - DOT_SIZE) / 2);
            g.setColor(currentState.color);
            g.fillOval(0, y, DOT_SIZE, DOT_SIZE);
            g.setColor(Color.DARK_GRAY);
            g.drawOval(0, y, DOT_SIZE - 1, DOT_SIZE - 1);
        }
    }
}
