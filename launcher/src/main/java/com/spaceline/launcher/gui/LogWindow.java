package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.spaceline.launcher.process.GameProcess;

/**
 * A standalone, resizable window that streams a running instance's log live, so
 * the user can watch it separately from the main launcher. It subscribes to the
 * process's log buffer and detaches its listener when closed.
 */
public final class LogWindow extends JFrame {

    private final GameProcess process;
    private final JTextArea area = new JTextArea();
    private final transient Consumer<String> listener;

    public LogWindow(GameProcess process) {
        super("Log — " + process.instanceId());
        this.process = process;
        setSize(new Dimension(760, 520));
        setLocationByPlatform(true);

        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        // Seed with what has already been logged.
        process.logBuffer().snapshot().forEach(line -> area.append(line + "\n"));
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        JButton export = new JButton("Export…");
        export.addActionListener(e -> export());
        JButton clear = new JButton("Clear view");
        clear.addActionListener(e -> area.setText(""));
        bar.add(export);
        bar.add(clear);
        add(bar, BorderLayout.SOUTH);

        // Live updates (marshalled onto the EDT).
        this.listener = line -> SwingUtilities.invokeLater(() -> {
            area.append(line + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
        process.logBuffer().addListener(listener);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                process.logBuffer().removeListener(listener);
            }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(process.instanceId() + "-log.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), area.getText());
            } catch (Exception ignored) {
                // best effort
            }
        }
    }
}
