package tk.kiritofeng.jpendulum;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;


public class Main {
    public static Pair<Double> compute(Pair<Double> a, Pair<Double> b, Pair<Double> c) {
        // compute the lines
        // no two points share an y-coordinate
        double m1 = - (b.first - a.first) / (b.second - a.second);
        double m2 = - (c.first - a.first) / (c.second - a.second);
        double b1 = -m1 * (a.first + b.first) / 2 + (a.second + b.second) / 2;
        double b2 = -m2 * (a.first + c.first) / 2 + (a.second + c.second) / 2;

        // solve system

        double x = (b2 - b1) / (m1 - m2);
        double y = m1 * x + b1;

        // great return the solution
        return new Pair<Double>(x, y);
    }

    public static boolean similar(Color C1, Color C2, int tol) {
        if(Math.abs(C1.getRed() - C2.getRed()) > tol) return false;
        if(Math.abs(C1.getBlue() - C2.getBlue()) > tol) return false;
        if(Math.abs(C1.getGreen() - C2.getGreen()) > tol) return false;
        return true;
    }

    public static boolean close(Pair<Double> P1, Pair<Double> P2, Pair<Double> P3) {
        int tol = 10;
        if(Math.abs(P1.first - P2.first) < tol) return true;
        if(Math.abs(P1.second - P2.second) < tol) return true;
        if(Math.abs(P1.first - P3.first) < tol) return true;
        if(Math.abs(P1.second - P3.second) < tol) return true;
        if(Math.abs(P2.first - P3.first) < tol) return true;
        if(Math.abs(P2.second - P3.second) < tol) return true;
        return false;
    }

    public static void main(String[] args) {
        if(args.length < 4) {
            System.err.println("Usage: jpendulum <video> <output file> <colour> <tolerance> <enable gui?>");
            return;
        }

        boolean gui = args.length < 5 || Boolean.parseBoolean(args[4]);

        JFrame preview = new JFrame();
        PreviewPane pane = new PreviewPane();
        preview.add(pane);
        preview.setSize(400, 300);
        preview.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        preview.pack();
        if(gui) {
            preview.setVisible(true);
        } else {
            preview.dispose();
        }

        // close stderr before the user gets spammed...
        System.err.close();

        try {
            // input video
            File video = new File(args[0]);
            // output csv file
            File output = new File(args[1]);
            PrintWriter pw = new PrintWriter(output);

            // parse the colour's hex code
            String[] hexcodes = args[2].split(",");
            Color[] C = new Color[hexcodes.length];
            for(int i=0;i<hexcodes.length;++i)
                C[i] = new Color(Integer.parseInt(hexcodes[i], 16));
            // get the tolerance
            int tolerance = Integer.parseInt(args[3]);

            // to store all points
            ArrayList<Pair> loc = new ArrayList<Pair>();

            // get the video frames
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video);
            grabber.start();

            double FPS = grabber.getFrameRate();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            Frame tmpFrame;

            int width = 1, height = 1;

            while(true) {
                try {
                    tmpFrame = grabber.grab();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                if(tmpFrame == null) break;
                BufferedImage frame = converter.convert(tmpFrame);
                if(frame == null) continue;
                pane.setPic(frame);

                width = frame.getWidth();
                height = frame.getHeight();

                // scan the whole frame for any occurrences of C, within tolerance range
                ArrayList<Pair> A = new ArrayList<Pair>();
                for(int i = 0; i < frame.getWidth(); ++i) {
                    for(int j = 0; j < frame.getHeight(); ++j) {
                        Color c = new Color(frame.getRGB(i, j));
                        for(Color cc:C) {
                            if (similar(cc, c, tolerance)) {
                                A.add(new Pair<Integer>(i, j));
                                break;
                            }
                        }
                    }
                }

                // get the center of mass
                Pair<Double> com = new Pair<Double>(0.0, 0.0);
                for(Pair<Integer> P: A) {
                    com.first += P.first;
                    com.second += P.second;
                }
                com.first /= A.size();
                com.second /= A.size();

                // add to ArrayList
                loc.add(com);

                pane.addPoint(new Pair<Integer>((int)(com.first * preview.getWidth()/width), (int)(com.second * preview.getHeight() / height)));

                pane.displayPoints();

                Thread.sleep(50);

                pane.clearPoints();
            }

            double i = 1;
            for(Pair<Double> P: loc) {
                pw.printf("%f,%f,%f\n", i/FPS, P.first, P.second);
                i++;
            }

            pw.close();
            System.out.println("Tracking Complete!");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}

