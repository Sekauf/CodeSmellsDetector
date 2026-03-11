package org.example.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Custom JPanel that renders a 3-circle Venn diagram for Baseline / SonarQube / JDeodorant.
 * Call {@link #update} to refresh with new counts.
 */
public class VennDiagramPanel extends JPanel {

    private static final Color COLOR_BASELINE   = new Color(100, 150, 255);
    private static final Color COLOR_SONAR      = new Color(255, 150, 80);
    private static final Color COLOR_JDEODORANT = new Color(80, 200, 100);
    private static final float FILL_ALPHA       = 0.28f;
    private static final double R_FRAC          = 0.28;

    // Circle centre positions as fractions of panel width/height
    private static final double BX = 0.32, BY = 0.40;
    private static final double SX = 0.65, SY = 0.40;
    private static final double JX = 0.48, JY = 0.65;

    // Region label positions (fractions of w/h)
    private static final double[][] LABEL_POS = {
        {0.10, 0.37},   // only Baseline
        {0.84, 0.37},   // only SonarQube
        {0.48, 0.88},   // only JDeodorant
        {0.48, 0.24},   // Baseline ∩ SonarQube (no JD)
        {0.23, 0.65},   // Baseline ∩ JDeodorant (no S)
        {0.72, 0.65},   // SonarQube ∩ JDeodorant (no B)
        {0.48, 0.50},   // all three
    };

    private AgreementCalculator.VennCounts vennCounts;

    /** Creates the panel with a sensible preferred size. */
    public VennDiagramPanel() {
        setPreferredSize(new Dimension(480, 300));
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    /**
     * Updates the displayed counts and repaints.
     *
     * @param counts new Venn region counts; null clears the display
     */
    public void update(AgreementCalculator.VennCounts counts) {
        this.vennCounts = counts;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = (int) (Math.min(w, h) * R_FRAC);
        int bx = px(w, BX), by = py(h, BY);
        int sx = px(w, SX), sy = py(h, SY);
        int jx = px(w, JX), jy = py(h, JY);

        drawFilledCircle(g2, bx, by, r, COLOR_BASELINE);
        drawFilledCircle(g2, sx, sy, r, COLOR_SONAR);
        drawFilledCircle(g2, jx, jy, r, COLOR_JDEODORANT);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2.setStroke(new BasicStroke(1.5f));
        drawCircleOutline(g2, bx, by, r, COLOR_BASELINE.darker());
        drawCircleOutline(g2, sx, sy, r, COLOR_SONAR.darker());
        drawCircleOutline(g2, jx, jy, r, COLOR_JDEODORANT.darker());

        paintLabels(g2, w, h, bx, by, sx, sy, jx, jy, r);
        if (vennCounts != null) {
            paintCounts(g2, w, h);
        }
        g2.dispose();
    }

    private void paintLabels(Graphics2D g2, int w, int h,
            int bx, int by, int sx, int sy, int jx, int jy, int r) {
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        drawCenteredString(g2, "Baseline",   bx, by - r - 8);
        drawCenteredString(g2, "SonarQube",  sx, sy - r - 8);
        drawCenteredString(g2, "JDeodorant", jx, jy + r + 16);
    }

    private void paintCounts(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        int[] counts = {
            vennCounts.onlyBaseline(), vennCounts.onlySonar(), vennCounts.onlyJdeo(),
            vennCounts.baselineSonarOnly(), vennCounts.baselineJdeoOnly(),
            vennCounts.sonarJdeoOnly(), vennCounts.allThree()
        };
        for (int i = 0; i < counts.length; i++) {
            drawCenteredString(g2, String.valueOf(counts[i]),
                    px(w, LABEL_POS[i][0]), py(h, LABEL_POS[i][1]));
        }
    }

    private void drawFilledCircle(Graphics2D g2, int cx, int cy, int r, Color color) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FILL_ALPHA));
        g2.setColor(color);
        g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private void drawCircleOutline(Graphics2D g2, int cx, int cy, int r, Color color) {
        g2.setColor(color);
        g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private void drawCenteredString(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy);
    }

    private static int px(int w, double frac) { return (int) (w * frac); }
    private static int py(int h, double frac) { return (int) (h * frac); }
}
