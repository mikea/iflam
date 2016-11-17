package flam.mains;

import flam.AnimationProvider;
import flam.FlamComponent;
import flam.Genome;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class AnimationMain {
    private static AnimationProvider animationProvider;
    private static JFrame frame;

    public static void main(String[] args) throws IOException, SAXException {
        frame = new JFrame();

        animationProvider = new AnimationProvider();
        openGenome("../sheeps/11000.flam3");

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());

        FlamComponent flamComponent = new FlamComponent(animationProvider);
        flamComponent.setFps(10);

        frame.getContentPane().add(flamComponent, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();

        JMenuItem openMenuItem = new JMenuItem("Open...");
        menuBar.add(openMenuItem);

        frame.setJMenuBar(menuBar);

        openMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("../sheeps"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("flam3", "flam3"));
            int val = fileChooser.showOpenDialog(frame);
            if (val == JFileChooser.APPROVE_OPTION) {
                openGenome(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    private static void openGenome(String path) {
        System.out.println("*** Opening " + path);
        try {
            frame.setTitle(path);
            animationProvider.setGenome(Genome.parse(path));
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }
}