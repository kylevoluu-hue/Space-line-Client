package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.content.ResourcePackManager;
import com.spaceline.launcher.content.ShaderManager;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.mods.ModFile;
import com.spaceline.launcher.mods.ModManager;

/**
 * The per-instance content page: enable/disable and remove mods, resource packs
 * and shaders for the selected instance, and import new ones. Mods toggle via the
 * {@code .disabled} convention; resource packs maintain a load order; shaders
 * track the single active pack.
 */
public final class ContentPanel extends JPanel {

    private final transient LauncherContext context;
    private final transient LauncherFrame owner;

    private final DefaultListModel<String> modsModel = new DefaultListModel<>();
    private final JList<String> modsList = new JList<>(modsModel);
    private final DefaultListModel<String> packsModel = new DefaultListModel<>();
    private final JList<String> packsList = new JList<>(packsModel);
    private final DefaultListModel<String> shadersModel = new DefaultListModel<>();
    private final JList<String> shadersList = new JList<>(shadersModel);
    private final JLabel header = new JLabel("Select an instance");

    public ContentPanel(LauncherFrame owner, LauncherContext context) {
        this.owner = owner;
        this.context = context;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mods", modsTab());
        tabs.addTab("Resource Packs", packsTab());
        tabs.addTab("Shaders", shadersTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    // Mods
    // ------------------------------------------------------------------

    private JPanel modsTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(modsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(button("Enable", e -> withMods(m -> m.enable(selectedMod(m)))));
        buttons.add(button("Disable", e -> withMods(m -> m.disable(selectedMod(m)))));
        buttons.add(button("Remove", e -> withMods(m -> m.remove(selectedMod(m)))));
        buttons.add(button("Import .jar…", e -> importMod()));
        panel.add(wrapButtons(buttons), BorderLayout.EAST);
        return panel;
    }

    private ModFile selectedMod(ModManager manager) {
        String name = modsList.getSelectedValue();
        if (name == null) {
            return null;
        }
        String bare = name.replace("  [disabled]", "");
        return manager.list().stream()
                .filter(f -> f.fileName().equals(bare) || f.fileName().equals(bare + ".disabled"))
                .findFirst().orElse(null);
    }

    private interface ModAction {
        void run(ModManager manager) throws Exception;
    }

    private void withMods(ModAction action) {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        ModManager manager = new ModManager(context.instances().modsDir(instance.id()));
        try {
            action.run(manager);
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    private void importMod() {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            new ModManager(context.instances().modsDir(instance.id()))
                    .importMod(chooser.getSelectedFile().toPath());
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    // ------------------------------------------------------------------
    // Resource packs
    // ------------------------------------------------------------------

    private JPanel packsTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(packsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(button("Enable", e -> withPacks(m -> m.enable(packsList.getSelectedValue()))));
        buttons.add(button("Disable", e -> withPacks(m -> m.disable(stripState(packsList.getSelectedValue())))));
        buttons.add(button("Move up", e -> withPacks(m -> m.move(stripState(packsList.getSelectedValue()), -1))));
        buttons.add(button("Move down", e -> withPacks(m -> m.move(stripState(packsList.getSelectedValue()), 1))));
        buttons.add(button("Import…", e -> importPack()));
        panel.add(wrapButtons(buttons), BorderLayout.EAST);
        return panel;
    }

    private interface PackAction {
        void run(ResourcePackManager manager) throws Exception;
    }

    private void withPacks(PackAction action) {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        try {
            action.run(new ResourcePackManager(context.instances().resourcePacksDir(instance.id())));
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    private void importPack() {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            new ResourcePackManager(context.instances().resourcePacksDir(instance.id()))
                    .importPack(chooser.getSelectedFile().toPath());
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    // ------------------------------------------------------------------
    // Shaders
    // ------------------------------------------------------------------

    private JPanel shadersTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(shadersList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(button("Set active", e -> withShaders(m -> m.enable(stripState(shadersList.getSelectedValue())))));
        buttons.add(button("Disable", e -> withShaders(ShaderManager::disable)));
        buttons.add(button("Import…", e -> importShader()));
        panel.add(wrapButtons(buttons), BorderLayout.EAST);
        return panel;
    }

    private interface ShaderAction {
        void run(ShaderManager manager) throws Exception;
    }

    private void withShaders(ShaderAction action) {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        try {
            action.run(new ShaderManager(context.instances().shaderPacksDir(instance.id())));
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    private void importShader() {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            new ShaderManager(context.instances().shaderPacksDir(instance.id()))
                    .importShader(chooser.getSelectedFile().toPath());
        } catch (Exception e) {
            error(e);
        }
        refresh();
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    /** Reloads all three lists for the currently selected instance. */
    public void refresh() {
        Instance instance = owner.selectedInstance();
        modsModel.clear();
        packsModel.clear();
        shadersModel.clear();
        if (instance == null) {
            header.setText("Select an instance in the Play tab to manage its content");
            return;
        }
        header.setText("Content for '" + instance.id() + "'");

        ModManager mods = new ModManager(context.instances().modsDir(instance.id()));
        for (ModFile mod : mods.list()) {
            modsModel.addElement(mod.metadata().name()
                    + (mod.isEnabled() ? "" : "  [disabled]"));
        }

        ResourcePackManager packs = new ResourcePackManager(context.instances().resourcePacksDir(instance.id()));
        List<String> enabled = packs.enabledInOrder();
        for (String pack : packs.available()) {
            packsModel.addElement(pack + (enabled.contains(pack) ? "  [on]" : ""));
        }

        ShaderManager shaders = new ShaderManager(context.instances().shaderPacksDir(instance.id()));
        String active = shaders.active().orElse(null);
        for (String shader : shaders.available()) {
            shadersModel.addElement(shader + (shader.equals(active) ? "  [active]" : ""));
        }
    }

    private static String stripState(String value) {
        return value == null ? null
                : value.replace("  [disabled]", "").replace("  [on]", "").replace("  [active]", "");
    }

    private JButton button(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.addActionListener(a);
        return b;
    }

    private JPanel wrapButtons(JPanel buttons) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(buttons, BorderLayout.NORTH);
        return wrap;
    }

    private void error(Throwable e) {
        javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Content",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
