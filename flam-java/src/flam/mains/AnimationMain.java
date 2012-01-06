package flam.mains;

import flam.AnimationProvider;
import flam.FlamComponent;
import flam.FlamGenome;
import flam.GenomeProvider;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class AnimationMain {

    private static AnimationProvider animationProvider;
    private static JFrame frame;

    public static void main(String[] args) throws IOException, SAXException {
        frame = new JFrame();

        animationProvider = new AnimationProvider();
        openGenome("sheeps/1006.flam3");


        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());

        FlamComponent flamComponent = new FlamComponent(animationProvider);
        flamComponent.setFps(10);
        frame.getContentPane().add(flamComponent, BorderLayout.CENTER);


        JMenuBar menuBar = new JMenuBar();

        JMenuItem openMenuItem = new JMenuItem("Open...");
        menuBar.add(openMenuItem);

        frame.setJMenuBar(menuBar);

        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("sheeps"));
                fileChooser.setFileFilter(new FileNameExtensionFilter("flam3", "flam3"));
                int val = fileChooser.showOpenDialog(frame);
                if (val == JFileChooser.APPROVE_OPTION) {
                    openGenome(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    private static void openGenome(String path) {
        try {
            frame.setTitle(path);
            animationProvider.setGenome(FlamGenome.parse(path));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }
}