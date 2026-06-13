package com.spaceline.launcher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.spaceline.common.ui.Theme;
import com.spaceline.common.ui.TextColorStyle;
import com.spaceline.launcher.LauncherContext;

/**
 * The Appearance page: choose a theme, UI font and font size, build an accent
 * text-colour style (solid / gradient / chrome-RGB) with a live preview, toggle
 * the interactive particle background, and import custom themes, fonts and
 * background images.
 */
public final class AppearancePanel extends JPanel {

    private final transient LauncherContext context;
    private final transient LauncherFrame owner;
    private final transient FontManager fontManager;

    private final AnimatedTitleLabel preview;
    private int color1 = 0xFF4C8DFF;
    private int color2 = 0xFFC46BFF;

    public AppearancePanel(LauncherFrame owner, LauncherContext context) {
        this.owner = owner;
        this.context = context;
        this.fontManager = new FontManager(context.paths().fonts());

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Theme.
        JComboBox<String> themeCombo = new JComboBox<>();
        context.themes().all().forEach(t -> themeCombo.addItem(t.id()));
        themeCombo.setSelectedItem(context.uiSettings().themeId());
        themeCombo.addActionListener(e -> {
            String id = (String) themeCombo.getSelectedItem();
            context.themes().setActive(id);
            context.uiSettings().setThemeId(id);
            context.saveUiSettings();
            owner.applyAppearance();
        });
        addRow(c, row++, "Theme:", themeCombo);

        // Font family + size.
        JComboBox<String> fontCombo = new JComboBox<>(fontManager.available().toArray(new String[0]));
        fontCombo.setSelectedItem(context.uiSettings().fontFamily());
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(context.uiSettings().fontSize(), 8, 28, 1));
        Runnable applyFont = () -> {
            context.uiSettings().setFontFamily((String) fontCombo.getSelectedItem());
            context.uiSettings().setFontSize((Integer) sizeSpinner.getValue());
            context.saveUiSettings();
            owner.applyAppearance();
        };
        fontCombo.addActionListener(e -> applyFont.run());
        sizeSpinner.addChangeListener(e -> applyFont.run());
        addRow(c, row++, "UI font:", fontCombo);
        addRow(c, row++, "Font size:", sizeSpinner);

        // Colour style.
        JComboBox<String> colorMode = new JComboBox<>(new String[]{"Solid", "Gradient", "Chrome (RGB)"});
        addRow(c, row++, "Title colour:", colorMode);

        JPanel colorButtons = new JPanel();
        JButton pick1 = new JButton("Colour 1");
        JButton pick2 = new JButton("Colour 2");
        colorButtons.add(pick1);
        colorButtons.add(pick2);
        addRow(c, row++, "", colorButtons);

        preview = new AnimatedTitleLabel("Spaceline", new Font("Serif", Font.BOLD, 30), buildStyle("Gradient"));
        preview.setPreferredSize(new Dimension(260, 44));
        addRow(c, row++, "Preview:", preview);

        Runnable applyColor = () -> {
            TextColorStyle style = buildStyle((String) colorMode.getSelectedItem());
            preview.setStyle(style);
            context.uiSettings().setTitleColor(style);
            context.saveUiSettings();
            owner.applyAppearance();
        };
        colorMode.addActionListener(e -> applyColor.run());
        pick1.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Colour 1", new Color(color1, true));
            if (chosen != null) {
                color1 = chosen.getRGB();
                applyColor.run();
            }
        });
        pick2.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Colour 2", new Color(color2, true));
            if (chosen != null) {
                color2 = chosen.getRGB();
                applyColor.run();
            }
        });

        // Particles.
        JCheckBox particles = new JCheckBox("Interactive particle background", context.uiSettings().particlesEnabled());
        particles.addActionListener(e -> {
            context.uiSettings().setParticlesEnabled(particles.isSelected());
            context.saveUiSettings();
            owner.applyAppearance();
        });
        addRow(c, row++, "", particles);

        // Imports.
        JPanel imports = new JPanel();
        imports.add(importButton("Import theme (.json)", "json", this::importTheme));
        imports.add(importButton("Import font", "ttf", this::importFont));
        imports.add(importButton("Import background", null, this::importBackground));
        addRow(c, row++, "Import:", imports);

        // Filler.
        c.gridx = 0;
        c.gridy = row;
        c.weighty = 1;
        add(new JLabel(), c);
    }

    private TextColorStyle buildStyle(String mode) {
        return switch (mode) {
            case "Solid" -> TextColorStyle.solid(color1);
            case "Chrome (RGB)" -> TextColorStyle.chrome();
            default -> TextColorStyle.gradient(color1, color2);
        };
    }

    private JButton importButton(String text, String ext, java.util.function.Consumer<Path> action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (ext != null) {
                chooser.setFileFilter(new FileNameExtensionFilter(text, ext, "otf", "png", "jpg", "jpeg"));
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                action.accept(chooser.getSelectedFile().toPath());
            }
        });
        return b;
    }

    private void importTheme(Path file) {
        try {
            Theme theme = context.themes().upload(file);
            owner.applyAppearance();
            info("Imported theme '" + theme.name() + "'");
        } catch (Exception e) {
            error(e);
        }
    }

    private void importFont(Path file) {
        try {
            String family = fontManager.importFont(file);
            info("Imported font '" + family + "'. Pick it in the UI font list.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void importBackground(Path file) {
        try {
            Path dest = context.paths().backgrounds().resolve(file.getFileName());
            java.nio.file.Files.copy(file, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            context.uiSettings().setBackgroundImage(dest.toString());
            context.saveUiSettings();
            owner.applyAppearance();
            info("Background set.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void addRow(GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        add(field, c);
    }

    private void info(String msg) {
        javax.swing.JOptionPane.showMessageDialog(this, msg, "Appearance",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(Throwable e) {
        javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Appearance",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
