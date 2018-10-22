package tk.kiritofeng.jpendulum;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class PreviewPane extends JPanel {

    private BufferedImage pic = null;
    private Vector<Pair> A = new Vector<Pair>();

    public void setPic(BufferedImage img){
        pic = img;
        repaint();
    }

    public void addPoint(Pair<Integer> P) {
        A.add(P);
    }

    public void displayPoints() {
        repaint();
    }

    public void clearPoints() {
        A.clear();
        repaint();
    }

    @Override
    public void paintComponent(Graphics G) {
        super.paintComponent(G);
        G.drawImage(pic, 0, 0, getWidth(), getHeight(), this);
        G.setColor(Color.GREEN);
        for(int i = 0 ; i < A.size(); ++i) {
            Pair<Integer> P = A.get(i);
            G.fillRect(P.first, P.second, 10, 10);
        }
    }
}
