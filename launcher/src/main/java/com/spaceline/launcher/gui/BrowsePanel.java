package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.browser.ProjectType;
import com.spaceline.launcher.browser.SearchResult;
import com.spaceline.launcher.instance.Instance;

/**
 * The content browser page: search Modrinth + CurseForge for mods, shaders,
 * resource packs and modpacks, filtered to the selected instance's Minecraft
 * version and loader, and one-click install into that instance with dependency
 * resolution.
 */
public final class BrowsePanel extends JPanel {

    private final transient LauncherContext context;
    private final transient LauncherFrame owner;

    private final JTextField query = new JTextField();
    private final JComboBox<ProjectType> typeCombo = new JComboBox<>(ProjectType.values());
    private final DefaultListModel<SearchResult> model = new DefaultListModel<>();
    private final JList<SearchResult> results = new JList<>(model);
    private final JLabel status = new JLabel("Search Modrinth & CurseForge");

    public BrowsePanel(LauncherFrame owner, LauncherContext context) {
        this.owner = owner;
        this.context = context;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(new JLabel("Search:"), BorderLayout.WEST);
        top.add(query, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        right.add(typeCombo);
        JButton search = new JButton("Search");
        search.addActionListener(e -> doSearch());
        right.add(search);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer(new ResultRenderer());
        add(new JScrollPane(results), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.WEST);
        JButton install = new JButton("Install to selected instance");
        install.addActionListener(e -> install());
        bottom.add(install, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        query.addActionListener(e -> doSearch());
    }

    private Instance instanceOrWarn() {
        Instance instance = owner.selectedInstance();
        if (instance == null) {
            JOptionPane.showMessageDialog(this, "Select an instance in the Play tab first.",
                    "No instance", JOptionPane.INFORMATION_MESSAGE);
        }
        return instance;
    }

    private void doSearch() {
        ProjectType type = (ProjectType) typeCombo.getSelectedItem();
        Instance instance = owner.selectedInstance();
        String mc = instance != null ? instance.minecraftVersion() : "";
        String loader = instance != null ? instance.loader().id() : "fabric";
        String text = query.getText().trim();

        status.setText("Searching…");
        model.clear();
        new SwingWorker<List<SearchResult>, Void>() {
            @Override
            protected List<SearchResult> doInBackground() {
                return context.modBrowser().search(text, type, mc, loader, 20);
            }

            @Override
            protected void done() {
                try {
                    List<SearchResult> hits = get();
                    hits.forEach(model::addElement);
                    status.setText(hits.size() + " result(s)"
                            + (mc.isBlank() ? "" : " for " + mc + " / " + loader));
                } catch (Exception e) {
                    status.setText("Search failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void install() {
        SearchResult selected = results.getSelectedValue();
        Instance instance = instanceOrWarn();
        if (selected == null || instance == null) {
            return;
        }
        Path target = switch (selected.type()) {
            case RESOURCE_PACK -> context.instances().resourcePacksDir(instance.id());
            case SHADER -> context.instances().shaderPacksDir(instance.id());
            default -> context.instances().modsDir(instance.id());
        };
        status.setText("Installing " + selected.title() + "…");
        new SwingWorker<List<Path>, Void>() {
            @Override
            protected List<Path> doInBackground() throws Exception {
                return context.modBrowser().install(selected,
                        instance.minecraftVersion(), instance.loader().id(), target);
            }

            @Override
            protected void done() {
                try {
                    int n = get().size();
                    status.setText("Installed " + selected.title() + " (" + n + " file(s))");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    status.setText("Install failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private static final class ResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                      boolean sel, boolean focus) {
            super.getListCellRendererComponent(l, value, index, sel, focus);
            if (value instanceof SearchResult r) {
                setText("<html><b>" + escape(r.title()) + "</b> "
                        + "<span style='color:#888;'>by " + escape(r.author()) + " · "
                        + r.provider() + " · " + r.downloads() + " downloads</span>"
                        + "<br><small>" + escape(trim(r.description())) + "</small></html>");
            }
            return this;
        }

        private static String trim(String s) {
            return s != null && s.length() > 110 ? s.substring(0, 110) + "…" : (s == null ? "" : s);
        }

        private static String escape(String s) {
            return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
