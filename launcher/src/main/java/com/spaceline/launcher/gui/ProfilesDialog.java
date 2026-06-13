package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.profile.ModProfile;

/**
 * The Mod Profiles page: apply a saved or pre-built profile to the selected
 * instance, snapshot the current instance as a new profile, and export / import
 * profiles as portable {@code .zip} files.
 */
public final class ProfilesDialog extends JDialog {

    private final transient LauncherContext context;
    private final transient LauncherFrame parentFrame;
    private final DefaultListModel<ModProfile> model = new DefaultListModel<>();
    private final JList<ModProfile> list = new JList<>(model);

    public ProfilesDialog(LauncherFrame owner, LauncherContext context) {
        super(owner, "Mod Profiles", true);
        this.context = context;
        this.parentFrame = owner;
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ProfileRenderer());
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(button("Apply to selected instance", e -> applyProfile()));
        buttons.add(button("Save current instance as profile…", e -> saveProfile()));
        buttons.add(button("Export profile (.zip)…", e -> exportProfile()));
        buttons.add(button("Import profile (.zip)…", e -> importProfile()));
        buttons.add(button("Delete profile", e -> deleteProfile()));
        JPanel side = new JPanel(new BorderLayout());
        side.add(buttons, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);

        refresh();
    }

    private JButton button(String text, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.addActionListener(action);
        return b;
    }

    private void refresh() {
        model.clear();
        context.profiles().list().forEach(model::addElement);
    }

    private Instance requireInstance() {
        Instance instance = parentFrame.selectedInstance();
        if (instance == null) {
            JOptionPane.showMessageDialog(this, "Select an instance in the launcher first.",
                    "No instance", JOptionPane.INFORMATION_MESSAGE);
        }
        return instance;
    }

    private void applyProfile() {
        ModProfile profile = list.getSelectedValue();
        Instance instance = requireInstance();
        if (profile == null || instance == null) {
            return;
        }
        Path instanceDir = context.instances().gameDir(instance.id());
        runAsync("Applying '" + profile.name() + "'…", () -> {
            if (!profile.modrinthSlugs().isEmpty()) {
                // Pre-built profile: install the newest compatible builds fresh.
                Path mods = context.instances().modsDir(instance.id());
                for (String slug : profile.modrinthSlugs()) {
                    context.modBrowser().installById("modrinth", slug,
                            instance.minecraftVersion(), instance.loader().id(), mods);
                }
            } else {
                context.profiles().applyToInstance(profile.id(), instanceDir);
            }
            return null;
        }, "Applied '" + profile.name() + "' to " + instance.id());
    }

    private void saveProfile() {
        Instance instance = requireInstance();
        if (instance == null) {
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Name this profile:", "Save profile",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        Path instanceDir = context.instances().gameDir(instance.id());
        runAsync("Saving profile…", () -> context.profiles().saveFromInstance(
                name.trim(), instance.minecraftVersion(), instance.loader().id(), instanceDir),
                "Saved profile '" + name.trim() + "'");
    }

    private void exportProfile() {
        ModProfile profile = list.getSelectedValue();
        if (profile == null || profile.builtIn()) {
            JOptionPane.showMessageDialog(this, "Pick a saved (non-built-in) profile to export.",
                    "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(profile.id() + ".zip"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path dest = chooser.getSelectedFile().toPath();
        runAsync("Exporting…", () -> {
            context.profiles().exportZip(profile.id(), dest);
            return null;
        }, "Exported to " + dest.getFileName());
    }

    private void importProfile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Profile zip", "zip"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path zip = chooser.getSelectedFile().toPath();
        runAsync("Importing…", () -> context.profiles().importZip(zip), "Imported profile");
    }

    private void deleteProfile() {
        ModProfile profile = list.getSelectedValue();
        if (profile == null || profile.builtIn()) {
            return;
        }
        try {
            context.profiles().delete(profile.id());
            refresh();
        } catch (Exception e) {
            error(e);
        }
    }

    /** Runs blocking IO off the EDT and refreshes on completion. */
    private void runAsync(String status, java.util.concurrent.Callable<?> task, String done) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.call();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                    parentFrame.reloadInstances();
                    JOptionPane.showMessageDialog(ProfilesDialog.this, done, "Profiles",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    error(e.getCause() != null ? e.getCause() : e);
                }
            }
        }.execute();
    }

    private void error(Throwable e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Profiles", JOptionPane.ERROR_MESSAGE);
    }

    private static final class ProfileRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                      boolean sel, boolean focus) {
            super.getListCellRendererComponent(l, value, index, sel, focus);
            if (value instanceof ModProfile p) {
                String tag = p.builtIn() ? " · built-in" : "";
                setText("<html><b>" + p.name() + "</b>" + tag
                        + "<br><small>" + p.description() + "</small></html>");
            }
            return this;
        }
    }
}
