package flam.mains;

import flam.DeepCopy;
import flam.FlamComponent;
import flam.Genome;
import flam.GenomeProvider;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class ViewMain {

    private static Genome genome;
    private static final JFrame frame = new JFrame();

    public static void main(String[] args) throws IOException, SAXException {
        openGenome("../sheeps/510.flam3");

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());

        FlamComponent flamComponent = new FlamComponent(new GenomeProvider() {
            @Override
            public Genome getGenome() {
                return genome;
            }

            @Override
            public void reset() {
            }
        });
        flamComponent.setFps(20);
        frame.getContentPane().add(flamComponent, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();

        JMenuItem openMenuItem = new JMenuItem("Open...");
        menuBar.add(openMenuItem);

        frame.setJMenuBar(menuBar);

        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("../sheeps"));
                fileChooser.setFileFilter(new FileNameExtensionFilter("flam3", "flam3"));
                int val = fileChooser.showOpenDialog(frame);
                if (val == JFileChooser.APPROVE_OPTION) {
                    openGenome(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("+") {
            @Override
            public void actionPerformed(ActionEvent e) {

                Genome newGenome = (Genome) DeepCopy.copy(genome);
                newGenome.zoom += .1;
                genome = newGenome;
            }
        });
        toolBar.add(new AbstractAction("-") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Genome newGenome = (Genome) DeepCopy.copy(genome);
                newGenome.zoom -= .1;
                genome = newGenome;
            }
        });
        toolBar.addSeparator();
        toolBar.add(new AbstractAction("<-") {
            @Override
            public void actionPerformed(ActionEvent e) {

                Genome newGenome = (Genome) DeepCopy.copy(genome);
                newGenome.center[0] += .1;
                genome = newGenome;
            }
        });
        toolBar.add(new AbstractAction("->") {
            @Override
            public void actionPerformed(ActionEvent e) {

                Genome newGenome = (Genome) DeepCopy.copy(genome);
                newGenome.center[0] -= .1;
                genome = newGenome;
            }
        });

        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        
        

        frame.pack();
        frame.setVisible(true);
    }

    private static void openGenome(String path) {
        try {
            System.out.println("***** Loading " + path);
            frame.setTitle(path);
            genome = Genome.parse(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }
}