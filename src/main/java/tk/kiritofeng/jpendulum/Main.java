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
        int tol = 20;
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
            Color C = new Color(Integer.parseInt(args[2], 16));
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
                        if(similar(C, c, tolerance)) {
                            A.add(new Pair<Integer>(i, j));
                            pane.addPoint(new Pair<Integer>(i*preview.getWidth()/frame.getWidth(), j*preview.getHeight()/frame.getHeight()));
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


            for(Pair<Double> P: loc) {
                //System.out.printf("%.2f\t%.2f\n",P.first,P.second);
            }

            System.out.println(loc.size());

            // approximate the center
            ArrayList<Pair> centers = new ArrayList<Pair>();
            for(int i = 0; i < loc.size(); ++i) {
                for(int j = i + 1; j < loc.size(); ++j) {
                    for(int k = j + 1; k < loc.size(); ++ k) {
                        if(!close(loc.get(i), loc.get(j), loc.get(k))) {
                            centers.add(compute(loc.get(i), loc.get(j), loc.get(k)));
                        }
                    }
                }
            }

            Pair<Double> center = new Pair<Double>(0.0, 0.0);
            for(Pair<Double> P: centers) {
                center.first += P.first;
                center.second += P.second;
                System.out.printf("%.2f\t%.2f\n",P.first,P.second);
                //pane.addPoint(new Pair<Integer>((int)(P.first * preview.getWidth()/width), (int)(P.second * preview.getHeight() / height)));
            }
            center.first /= centers.size();
            center.second /= centers.size();

            //System.out.printf("(%f, %f)\n",center.first,center.second);

            pane.addPoint(new Pair<Integer>((int)(center.first * preview.getWidth()/width), (int)(center.second * preview.getHeight() / height)));

            pane.displayPoints();

            // print the angles
            double i = 1;
            for(Pair<Double> P: loc) {
                pw.printf("%f,%f\n", i/FPS,Math.PI/2 + Math.atan2(center.second - P.second, center.first - P.first));
                i++;
            }

            //System.out.println(center.first + "\t" + center.second);
            pw.close();
            System.out.println("Tracking Complete!");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}

