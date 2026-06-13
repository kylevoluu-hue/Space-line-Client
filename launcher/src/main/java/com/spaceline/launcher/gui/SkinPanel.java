package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.skin.SkinLibrary;

/**
 * The Skins page: type a player's name to fetch and preview their public skin in
 * a 3D 360° model, save skins to a named, favouritable library, and import /
 * export skin PNGs.
 */
public final class SkinPanel extends JPanel {

    private final transient LauncherContext context;
    private final SkinModelView model = new SkinModelView();
    private final JTextField username = new JTextField(14);
    private final JLabel status = new JLabel("Type a username to load a skin");
    private final DefaultListModel<SkinLibrary.Entry> libModel = new DefaultListModel<>();
    private final JList<SkinLibrary.Entry> library = new JList<>(libModel);

    public SkinPanel(LauncherContext context) {
        this.context = context;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: username fetch + auto-rotate.
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.add(new JLabel("Player name:"));
        top.add(username);
        JButton fetch = new JButton("Fetch skin");
        fetch.addActionListener(e -> fetchByUsername());
        top.add(fetch);
        JCheckBox autoRotate = new JCheckBox("Auto-rotate", true);
        autoRotate.addActionListener(e -> model.setAutoRotate(autoRotate.isSelected()));
        top.add(autoRotate);
        add(top, BorderLayout.NORTH);

        // Center: 3D model.
        model.setPreferredSize(new Dimension(320, 420));
        add(model, BorderLayout.CENTER);

        // Right: library.
        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setPreferredSize(new Dimension(240, 0));
        JLabel libLabel = new JLabel("Saved skins");
        side.add(libLabel, BorderLayout.NORTH);
        library.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        library.setCellRenderer(new SkinRenderer());
        library.addListSelectionListener(e -> loadSelectedFromLibrary());
        side.add(new JScrollPane(library), BorderLayout.CENTER);

        JPanel libButtons = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
        libButtons.add(btn("Favourite", e -> toggleFavorite()));
        libButtons.add(btn("Rename", e -> rename()));
        libButtons.add(btn("Import", e -> importFile()));
        libButtons.add(btn("Export", e -> exportSelected()));
        libButtons.add(btn("Delete", e -> deleteSelected()));
        side.add(libButtons, BorderLayout.SOUTH);
        add(side, BorderLayout.EAST);

        add(status, BorderLayout.SOUTH);
        refreshLibrary();
        model.start();
    }

    private JButton btn(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.addActionListener(a);
        return b;
    }

    private void refreshLibrary() {
        libModel.clear();
        context.skinLibrary().list().forEach(libModel::addElement);
    }

    private void fetchByUsername() {
        String name = username.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        status.setText("Looking up " + name + "…");
        new SwingWorker<BufferedImage, Void>() {
            private String savedName;

            @Override
            protected BufferedImage doInBackground() throws Exception {
                Optional<String> url = context.skins().publicSkinUrl(name);
                if (url.isEmpty()) {
                    return null;
                }
                // Save into the library and load the PNG for the 3D preview.
                SkinLibrary.Entry entry = context.skinLibrary()
                        .importFromUrl(name, url.get(), name, "classic");
                savedName = entry.name;
                return javax.imageio.ImageIO.read(context.skinLibrary().pngPath(entry).toFile());
            }

            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img == null) {
                        status.setText("No public skin found for '" + name + "'");
                        return;
                    }
                    model.setSkin(img);
                    refreshLibrary();
                    status.setText("Loaded and saved skin for " + savedName);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    status.setText("Fetch failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void loadSelectedFromLibrary() {
        SkinLibrary.Entry entry = library.getSelectedValue();
        if (entry == null) {
            return;
        }
        try {
            Path png = context.skinLibrary().pngPath(entry);
            if (Files.exists(png)) {
                model.setSkin(javax.imageio.ImageIO.read(png.toFile()));
                status.setText("Showing '" + entry.name + "'");
            }
        } catch (Exception e) {
            status.setText("Could not load skin: " + e.getMessage());
        }
    }

    private void toggleFavorite() {
        SkinLibrary.Entry entry = library.getSelectedValue();
        if (entry != null) {
            context.skinLibrary().setFavorite(entry.id, !entry.favorite);
            refreshLibrary();
        }
    }

    private void rename() {
        SkinLibrary.Entry entry = library.getSelectedValue();
        if (entry == null) {
            return;
        }
        String name = JOptionPane.showInputDialog(this, "New name:", entry.name);
        if (name != null && !name.isBlank()) {
            context.skinLibrary().rename(entry.id, name.trim());
            refreshLibrary();
        }
    }

    private void importFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Skin PNG", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Name this skin:", "Imported skin");
        try {
            context.skinLibrary().importFromFile(name, chooser.getSelectedFile().toPath(), "classic");
            refreshLibrary();
        } catch (Exception e) {
            status.setText("Import failed: " + e.getMessage());
        }
    }

    private void exportSelected() {
        SkinLibrary.Entry entry = library.getSelectedValue();
        if (entry == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(entry.name.replaceAll("\\s+", "_") + ".png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            context.skinLibrary().export(entry.id, chooser.getSelectedFile().toPath());
            status.setText("Exported '" + entry.name + "'");
        } catch (Exception e) {
            status.setText("Export failed: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        SkinLibrary.Entry entry = library.getSelectedValue();
        if (entry != null) {
            context.skinLibrary().delete(entry.id);
            refreshLibrary();
        }
    }

    private static final class SkinRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                      boolean sel, boolean focus) {
            super.getListCellRendererComponent(l, value, index, sel, focus);
            if (value instanceof SkinLibrary.Entry e) {
                setText((e.favorite ? "★ " : "") + e.name
                        + (e.sourceUsername.isBlank() ? "" : "  (" + e.sourceUsername + ")"));
            }
            return this;
        }
    }
}
