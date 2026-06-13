package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.account.Account;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.instance.ModLoader;
import com.spaceline.launcher.launch.GameLauncher;
import com.spaceline.launcher.process.GameProcess;
import com.spaceline.launcher.version.MinecraftVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main launcher window: an account picker, an instance list and a prominent
 * Play/Stop control with a live log console — the "menu before launching" that
 * mirrors modern clients.
 *
 * <p>All long-running work (manifest fetches, version installs, the launch
 * itself) runs on {@link SwingWorker}s so the UI never freezes; results are
 * marshalled back onto the Swing event thread.
 */
public final class LauncherFrame extends JFrame {

    private static final Logger LOG = LoggerFactory.getLogger(LauncherFrame.class);

    private final transient LauncherContext context;

    private final JComboBox<Account> accountCombo = new JComboBox<>();
    private final DefaultListModel<Instance> instanceModel = new DefaultListModel<>();
    private final JList<Instance> instanceList = new JList<>(instanceModel);
    private final JTextArea console = new JTextArea();
    private final JProgressBar progress = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton playButton = new JButton("PLAY");
    private final JButton stopButton = new JButton("Stop");
    private final JLabel instanceTitle = new JLabel("Select an instance to play");
    private final JSlider ramSlider = new JSlider(1024, 16384, 4096);
    private final JLabel ramValue = new JLabel("4096 MB");
    private boolean darkTheme = true;

    private final java.awt.CardLayout pages = new java.awt.CardLayout();
    private final JPanel pageHost = new JPanel(pages);
    private ContentPanel contentPanel;
    private ParticlePanel backgroundLayer;
    private FadeOverlay fadeOverlay;

    private final javax.swing.JCheckBox autoSaveLog = new javax.swing.JCheckBox("Auto-save");
    private transient GameProcess currentProcess;
    private transient java.io.BufferedWriter logWriter;

    public LauncherFrame(LauncherContext context) {
        super("Space~line Client");
        this.context = context;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        loadWindowIcon();

        // The whole window sits on a particle/background layer; the panels above
        // it are made transparent so the background shows through (Lunar-style).
        backgroundLayer = new ParticlePanel(90, new Color(0x6F, 0xB8, 0xFF));
        backgroundLayer.setLayout(new BorderLayout());
        backgroundLayer.add(buildHeader(), BorderLayout.NORTH);
        backgroundLayer.add(buildNavRail(), BorderLayout.WEST);
        backgroundLayer.add(buildPages(), BorderLayout.CENTER);
        backgroundLayer.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(backgroundLayer);
        fadeOverlay = new FadeOverlay();
        setGlassPane(fadeOverlay);

        transparentize(backgroundLayer);
        refreshAccounts();
        refreshInstances();
        wireProcessExit();
        applyAppearance();
        backgroundLayer.start();
    }

    /**
     * Recursively makes container panels and labels non-opaque so the particle /
     * image background shows through, while leaving interactive widgets (lists,
     * text areas, buttons, fields) with their solid backgrounds for readability.
     */
    private void transparentize(java.awt.Container container) {
        for (Component child : container.getComponents()) {
            if (child == backgroundLayer) {
                continue;
            }
            if (child instanceof JPanel || child instanceof JLabel) {
                ((javax.swing.JComponent) child).setOpaque(false);
            }
            if (child instanceof java.awt.Container c) {
                transparentize(c);
            }
        }
    }

    /** The left navigation rail that switches between pages. */
    private JPanel buildNavRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        rail.add(navButton("Play", "play"));
        rail.add(Box.createVerticalStrut(6));
        rail.add(navButton("Browse", "browse"));
        rail.add(Box.createVerticalStrut(6));
        rail.add(navButton("Content", "content"));
        rail.add(Box.createVerticalStrut(6));
        rail.add(navButton("Skins", "skins"));
        rail.add(Box.createVerticalStrut(6));
        rail.add(navButton("Appearance", "appearance"));
        rail.add(Box.createVerticalGlue());
        return rail;
    }

    private JButton navButton(String label, String card) {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(150, 42));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("FlatLaf.style", "arc: 14; borderWidth: 0; focusWidth: 0");
        b.addActionListener(e -> transitionTo(card));
        return b;
    }

    /** Switches pages with a short cross-fade animation. */
    private void transitionTo(String card) {
        java.awt.image.BufferedImage snapshot = null;
        if (pageHost.getWidth() > 0 && pageHost.getHeight() > 0) {
            snapshot = new java.awt.image.BufferedImage(
                    pageHost.getWidth(), pageHost.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = snapshot.createGraphics();
            pageHost.paint(g);
            g.dispose();
        }
        pages.show(pageHost, card);
        if ("content".equals(card) && contentPanel != null) {
            contentPanel.refresh();
        }
        if (snapshot != null && fadeOverlay != null) {
            java.awt.Point origin = SwingUtilities.convertPoint(pageHost, 0, 0, getRootPane());
            fadeOverlay.play(snapshot, origin);
        }
    }

    private JPanel buildPages() {
        JPanel playPage = new JPanel(new BorderLayout());
        playPage.add(buildSidebar(), BorderLayout.WEST);
        playPage.add(buildMain(), BorderLayout.CENTER);

        this.contentPanel = new ContentPanel(this, context);
        pageHost.add(playPage, "play");
        pageHost.add(new BrowsePanel(this, context), "browse");
        pageHost.add(contentPanel, "content");
        pageHost.add(new SkinPanel(context), "skins");
        pageHost.add(new AppearancePanel(this, context), "appearance");
        return pageHost;
    }

    /**
     * Re-applies persisted appearance settings (UI font, theme accent, light/dark)
     * to the whole window. Called at startup and whenever the user changes them.
     */
    public void applyAppearance() {
        var ui = context.uiSettings();
        javax.swing.UIManager.put("defaultFont",
                new Font(ui.fontFamily(), Font.PLAIN, ui.fontSize()));
        // Pick a light or dark base look from the active theme's background.
        int bg = context.themes().active().background();
        boolean light = ((bg >> 16) & 0xFF) + ((bg >> 8) & 0xFF) + (bg & 0xFF) > 384;
        darkTheme = !light;
        if (light) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        Styling.applyDefaults();
        SwingUtilities.updateComponentTreeUI(this);
        stylePlayButton();
        // updateComponentTreeUI resets opaqueness, so re-assert it and the bg.
        if (backgroundLayer != null) {
            transparentize(backgroundLayer);
            applyBackground();
        }
    }

    /** Applies the particle toggle and background image from saved UI settings. */
    private void applyBackground() {
        var ui = context.uiSettings();
        backgroundLayer.setParticlesActive(ui.particlesEnabled());
        String bg = ui.backgroundImage();
        if (bg != null && !bg.isBlank() && java.nio.file.Files.exists(java.nio.file.Path.of(bg))) {
            try {
                backgroundLayer.setBackgroundImage(javax.imageio.ImageIO.read(new java.io.File(bg)));
            } catch (Exception e) {
                backgroundLayer.setBackgroundImage(null);
            }
        } else {
            backgroundLayer.setBackgroundImage(null);
        }
    }

    /** Makes the PLAY button a large, rounded accent "pill". */
    private void stylePlayButton() {
        playButton.setBackground(accent());
        playButton.setForeground(Color.WHITE);
        playButton.putClientProperty("JButton.buttonType", "roundRect");
        playButton.putClientProperty("FlatLaf.style", "arc: 999; borderWidth: 0; focusWidth: 0");
        playButton.setBorderPainted(false);
        playButton.setFocusPainted(false);
    }

    // ------------------------------------------------------------------
    // UI construction
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Space~line");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(accent());
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accountCombo.setRenderer(new AccountRenderer());
        accountCombo.setPreferredSize(new Dimension(220, 30));
        accountCombo.addActionListener(e -> onAccountSelected());
        right.add(new JLabel("Account:"));
        right.add(accountCombo);

        JButton addAccount = new JButton("Add account");
        addAccount.addActionListener(e -> showAddAccountMenu(addAccount));
        right.add(addAccount);

        JButton profiles = new JButton("Profiles");
        profiles.addActionListener(e -> new ProfilesDialog(this, context).setVisible(true));
        right.add(profiles);

        JButton credits = new JButton("Credits");
        credits.addActionListener(e -> new CreditsDialog(this).setVisible(true));
        right.add(credits);

        JToggleButton themeToggle = new JToggleButton("Light");
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));
        right.add(themeToggle);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void toggleTheme(JToggleButton toggle) {
        darkTheme = !darkTheme;
        if (darkTheme) {
            FlatDarkLaf.setup();
            toggle.setText("Light");
        } else {
            FlatLightLaf.setup();
            toggle.setText("Dark");
        }
        Styling.applyDefaults();
        SwingUtilities.updateComponentTreeUI(this);
        stylePlayButton();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 8));
        sidebar.setPreferredSize(new Dimension(260, 0));

        JLabel heading = new JLabel("Instances");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        sidebar.add(heading, BorderLayout.NORTH);

        instanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instanceList.setCellRenderer(new InstanceRenderer());
        instanceList.addListSelectionListener(e -> {
            updateInstanceDetail();
            updatePlayState();
        });
        sidebar.add(new JScrollPane(instanceList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton create = new JButton("New");
        create.addActionListener(e -> showCreateInstanceDialog());
        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> deleteSelectedInstance());
        buttons.add(create);
        buttons.add(delete);
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        sidebar.add(buttons, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 16));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Selected-instance detail: title + RAM allocation slider.
        instanceTitle.setFont(instanceTitle.getFont().deriveFont(Font.BOLD, 16f));
        instanceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(instanceTitle);
        top.add(Box.createVerticalStrut(6));

        ramSlider.setMajorTickSpacing(4096);
        ramSlider.setMinorTickSpacing(1024);
        ramSlider.setSnapToTicks(true);
        ramSlider.setEnabled(false);
        ramSlider.addChangeListener(e -> onRamChanged());
        JPanel ramRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ramRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramRow.add(new JLabel("Memory:"));
        ramRow.add(ramSlider);
        ramRow.add(ramValue);
        top.add(ramRow);
        top.add(Box.createVerticalStrut(8));

        // Play / Stop controls.
        playButton.setFont(playButton.getFont().deriveFont(Font.BOLD, 18f));
        playButton.setBackground(accent());
        playButton.setForeground(Color.WHITE);
        playButton.setPreferredSize(new Dimension(160, 48));
        playButton.addActionListener(e -> launchSelected());
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSelected());
        JPanel playButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        playButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        playButtons.add(playButton);
        playButtons.add(stopButton);
        top.add(playButtons);

        main.add(top, BorderLayout.NORTH);

        // Console + log toolbar.
        JPanel logPanel = new JPanel(new BorderLayout(0, 4));
        JPanel logBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        logBar.add(new JLabel("Log"));
        JButton popOut = new JButton("Open in window");
        popOut.addActionListener(e -> openLogWindow());
        JButton exportLog = new JButton("Export…");
        exportLog.addActionListener(e -> exportLog());
        logBar.add(popOut);
        logBar.add(exportLog);
        logBar.add(autoSaveLog);
        autoSaveLog.setSelected(true);
        logPanel.add(logBar, BorderLayout.NORTH);

        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logPanel.add(new JScrollPane(console), BorderLayout.CENTER);
        main.add(logPanel, BorderLayout.CENTER);
        return main;
    }

    private void openLogWindow() {
        if (currentProcess == null) {
            JOptionPane.showMessageDialog(this, "Launch an instance first to view its log.",
                    "Log", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new LogWindow(currentProcess).setVisible(true);
    }

    private void exportLog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("spaceline-log.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), console.getText());
            setStatus("Log exported to " + chooser.getSelectedFile().getName());
        } catch (Exception e) {
            setStatus("Export failed: " + e.getMessage());
        }
    }

    private void onRamChanged() {
        ramValue.setText(ramSlider.getValue() + " MB");
        Instance selected = instanceList.getSelectedValue();
        if (selected != null && ramSlider.isEnabled()) {
            selected.setMaxMemoryMb(ramSlider.getValue());
            context.instances().save(selected);
        }
    }

    /** The instance currently selected in the sidebar, or null. */
    public Instance selectedInstance() {
        return instanceList.getSelectedValue();
    }

    /** Re-reads instances from disk (e.g. after a profile import). */
    public void reloadInstances() {
        refreshInstances();
    }

    private void updateInstanceDetail() {
        Instance selected = instanceList.getSelectedValue();
        if (selected == null) {
            instanceTitle.setText("Select an instance to play");
            ramSlider.setEnabled(false);
            return;
        }
        instanceTitle.setText(selected.name() + "  —  " + selected.minecraftVersion()
                + " · " + selected.loader().displayName());
        ramSlider.setEnabled(true);
        ramSlider.setValue(selected.maxMemoryMb());
        ramValue.setText(selected.maxMemoryMb() + " MB");
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 16, 8, 16));
        progress.setVisible(false);
        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(180, 16));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(progress, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    private void refreshAccounts() {
        DefaultComboBoxModel<Account> model = new DefaultComboBoxModel<>();
        List<Account> accounts = context.accounts().accounts();
        accounts.forEach(model::addElement);
        accountCombo.setModel(model);
        context.accounts().active().ifPresent(accountCombo::setSelectedItem);
        accountCombo.setRenderer(new AccountRenderer());
        updatePlayState();
    }

    private void onAccountSelected() {
        Account selected = (Account) accountCombo.getSelectedItem();
        if (selected != null) {
            context.accounts().setActive(selected.uuid());
        }
    }

    private void showAddAccountMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem offline = new JMenuItem("Offline account");
        offline.addActionListener(e -> addOfflineAccount());
        JMenuItem microsoft = new JMenuItem("Microsoft account");
        microsoft.addActionListener(e -> addMicrosoftAccount());
        menu.add(microsoft);
        menu.add(offline);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void addOfflineAccount() {
        String username = JOptionPane.showInputDialog(this,
                "Enter a username for the offline account:", "Offline account",
                JOptionPane.PLAIN_MESSAGE);
        if (username != null && !username.isBlank()) {
            context.accounts().addOffline(username.trim());
            refreshAccounts();
        }
    }

    private void addMicrosoftAccount() {
        MicrosoftLoginDialog dialog = new MicrosoftLoginDialog(this, context);
        dialog.setVisible(true);
        if (dialog.signedInAccount() != null) {
            context.accounts().add(dialog.signedInAccount());
            refreshAccounts();
            setStatus("Signed in as " + dialog.signedInAccount().username());
        }
    }

    // ------------------------------------------------------------------
    // Instances
    // ------------------------------------------------------------------

    private void refreshInstances() {
        instanceModel.clear();
        context.instances().list().forEach(instanceModel::addElement);
        updatePlayState();
    }

    private void showCreateInstanceDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        javax.swing.JTextField nameField = new javax.swing.JTextField("New Instance");
        JComboBox<String> versionCombo = new JComboBox<>();
        versionCombo.addItem("Loading versions…");
        versionCombo.setEnabled(false);
        JComboBox<ModLoader> loaderCombo = new JComboBox<>(ModLoader.values());

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Minecraft version:"));
        panel.add(versionCombo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Loader:"));
        panel.add(loaderCombo);

        // Load the stable version list in the background.
        new SwingWorker<List<MinecraftVersion>, Void>() {
            @Override
            protected List<MinecraftVersion> doInBackground() throws IOException {
                return context.versions().supportedVersions();
            }

            @Override
            protected void done() {
                try {
                    versionCombo.removeAllItems();
                    get().forEach(v -> versionCombo.addItem(v.id()));
                    versionCombo.setEnabled(true);
                } catch (Exception e) {
                    versionCombo.removeAllItems();
                    versionCombo.addItem("(failed to load — check internet)");
                }
            }
        }.execute();

        int result = JOptionPane.showConfirmDialog(this, panel, "New instance",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String version = (String) versionCombo.getSelectedItem();
        if (version == null || !version.matches("\\d+\\.\\d+.*")) {
            JOptionPane.showMessageDialog(this, "Pick a valid Minecraft version.",
                    "New instance", JOptionPane.WARNING_MESSAGE);
            return;
        }
        createInstance(nameField.getText().trim(), version, (ModLoader) loaderCombo.getSelectedItem());
    }

    private void createInstance(String name, String version, ModLoader loader) {
        setBusy(true, "Creating instance…");
        new SwingWorker<Instance, Void>() {
            @Override
            protected Instance doInBackground() throws IOException {
                Instance instance = context.instances().create(name, version, loader);
                if (loader == ModLoader.FABRIC) {
                    instance.setFabricLoaderVersion(context.fabric().latestStableLoader());
                    context.instances().save(instance);
                    // Always-updating essentials: Fabric API, Sodium, FerriteCore.
                    context.bundledMods().installEssentials(loader, version,
                            context.instances().modsDir(instance.id()));
                }
                return instance;
            }

            @Override
            protected void done() {
                setBusy(false, "Ready");
                try {
                    get();
                    refreshInstances();
                } catch (Exception e) {
                    showError("Could not create instance", e);
                }
            }
        }.execute();
    }

    private void deleteSelectedInstance() {
        Instance selected = instanceList.getSelectedValue();
        if (selected == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete instance '" + selected.id() + "' and all its files?",
                "Delete instance", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            context.instances().delete(selected.id());
            refreshInstances();
        }
    }

    // ------------------------------------------------------------------
    // Launch / stop
    // ------------------------------------------------------------------

    private void launchSelected() {
        Instance instance = instanceList.getSelectedValue();
        Account account = (Account) accountCombo.getSelectedItem();
        if (instance == null || account == null) {
            return;
        }
        console.setText("");
        setBusy(true, "Preparing " + instance.id() + " (first launch downloads Minecraft)…");
        playButton.setEnabled(false);

        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                GameProcess process = context.launcher().launch(instance, account);
                currentProcess = process;
                publish("Minecraft started (pid " + process.pid() + ")");
                process.logBuffer().addListener(this::publish);
                if (autoSaveLog.isSelected()) {
                    attachAutoSave(process, instance.id());
                }
                process.logBuffer().snapshot().forEach(this::publish);
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, "Running " + instance.id());
                    stopButton.setEnabled(true);
                });
                return process.waitFor();
            }

            @Override
            protected void process(List<String> lines) {
                lines.forEach(line -> console.append(line + "\n"));
                console.setCaretPosition(console.getDocument().getLength());
            }

            @Override
            protected void done() {
                stopButton.setEnabled(false);
                updatePlayState();
                try {
                    int code = get();
                    setStatus("Exited (code " + code + ")");
                } catch (Exception e) {
                    setBusy(false, "Launch failed");
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    showError("Launch failed", cause);
                }
            }
        }.execute();
    }

    /** Streams the running process's log to a timestamped file under logs/. */
    private void attachAutoSave(GameProcess process, String instanceId) {
        try {
            String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            java.nio.file.Path file = context.paths().logs().resolve(instanceId + "-" + stamp + ".log");
            logWriter = java.nio.file.Files.newBufferedWriter(file);
            setStatus("Auto-saving log to " + file.getFileName());
            process.logBuffer().addListener(line -> {
                try {
                    if (logWriter != null) {
                        logWriter.write(line);
                        logWriter.newLine();
                        logWriter.flush();
                    }
                } catch (java.io.IOException ignored) {
                    // best effort
                }
            });
        } catch (java.io.IOException e) {
            setStatus("Could not auto-save log: " + e.getMessage());
        }
    }

    private void stopSelected() {
        Instance instance = instanceList.getSelectedValue();
        if (instance != null) {
            context.processes().stop(instance.id(), Duration.ofSeconds(10));
            setStatus("Stopping " + instance.id() + "…");
        }
    }

    private void wireProcessExit() {
        context.processes().setExitListener((id, result, restarting) ->
                SwingUtilities.invokeLater(() -> {
                    setStatus(id + ": " + result.summary());
                    stopButton.setEnabled(false);
                    updatePlayState();
                }));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void updatePlayState() {
        boolean canPlay = instanceList.getSelectedValue() != null
                && accountCombo.getSelectedItem() != null;
        playButton.setEnabled(canPlay && !progress.isVisible());
    }

    private void setBusy(boolean busy, String status) {
        progress.setVisible(busy);
        setStatus(status);
        updatePlayState();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String title, Throwable error) {
        LOG.error(title, error);
        JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private Color accent() {
        return new Color(context.themes().active().accent(), true);
    }

    /** Loads the black-hole app icon from the classpath for the window/taskbar. */
    private void loadWindowIcon() {
        try (var in = getClass().getResourceAsStream("/branding/spaceline.png")) {
            if (in != null) {
                setIconImage(javax.imageio.ImageIO.read(in));
            }
        } catch (Exception e) {
            LOG.debug("Could not load window icon", e);
        }
    }

    /** Renders an account as "username (TYPE)". */
    private static final class AccountRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Account account) {
                setText(account.username() + "  (" + account.type() + ")");
            }
            return this;
        }
    }

    /** Renders an instance as "name — version loader". */
    private static final class InstanceRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Instance instance) {
                setText("<html><b>" + instance.name() + "</b><br><small>"
                        + instance.minecraftVersion() + " · " + instance.loader().displayName()
                        + "</small></html>");
            }
            return this;
        }
    }

    /**
     * Transparent glass-pane overlay used for the page cross-fade: it paints a
     * snapshot of the previous page on top of the freshly-shown one and fades it
     * out, producing a smooth transition.
     */
    private static final class FadeOverlay extends javax.swing.JComponent {
        private java.awt.image.BufferedImage image;
        private java.awt.Point origin = new java.awt.Point();
        private float alpha;
        private final javax.swing.Timer timer;

        FadeOverlay() {
            setOpaque(false);
            this.timer = new javax.swing.Timer(16, e -> {
                alpha -= 0.10f;
                if (alpha <= 0) {
                    alpha = 0;
                    ((javax.swing.Timer) e.getSource()).stop();
                    image = null;
                    setVisible(false);
                }
                repaint();
            });
        }

        void play(java.awt.image.BufferedImage snapshot, java.awt.Point at) {
            this.image = snapshot;
            this.origin = at;
            this.alpha = 1f;
            setVisible(true);
            timer.restart();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            if (image == null) {
                return;
            }
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(image, origin.x, origin.y, null);
            g2.dispose();
        }
    }
}
