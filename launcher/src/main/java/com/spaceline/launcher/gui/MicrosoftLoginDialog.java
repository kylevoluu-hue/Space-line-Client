package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.account.Account;
import com.spaceline.launcher.account.MicrosoftAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal dialog driving the Microsoft device-code sign-in.
 *
 * <p>On open it requests a device code, shows the user the short code and the
 * verification URL (with copy + "open browser" buttons), then polls in the
 * background until the user finishes authorising. On success the resulting
 * {@link Account} is exposed via {@link #signedInAccount()}.
 */
public final class MicrosoftLoginDialog extends JDialog {

    private static final Logger LOG = LoggerFactory.getLogger(MicrosoftLoginDialog.class);

    private final transient LauncherContext context;
    private final JLabel statusLabel = new JLabel("Requesting a sign-in code…");
    private final JTextField codeField = new JTextField();
    private final JButton openButton = new JButton("Open Microsoft sign-in");
    private final JButton copyButton = new JButton("Copy code");

    private transient Account signedInAccount;
    private transient String verificationUri;

    public MicrosoftLoginDialog(Frame owner, LauncherContext context) {
        super(owner, "Sign in with Microsoft", true);
        this.context = context;

        setLayout(new BorderLayout(0, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setMinimumSize(new Dimension(440, 240));
        setLocationRelativeTo(owner);

        add(statusLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        JLabel instructions = new JLabel("<html>Go to the sign-in page and enter this code:</html>");
        center.add(instructions, BorderLayout.NORTH);

        codeField.setEditable(false);
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        center.add(codeField, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        openButton.setEnabled(false);
        openButton.addActionListener(e -> openBrowser());
        copyButton.setEnabled(false);
        copyButton.addActionListener(e -> copyCode());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        buttons.add(openButton);
        buttons.add(copyButton);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);

        startFlow();
    }

    public Account signedInAccount() {
        return signedInAccount;
    }

    private void startFlow() {
        new SwingWorker<Account, String>() {
            @Override
            protected Account doInBackground() throws Exception {
                MicrosoftAuthenticator authenticator = context.microsoftAuthenticator();
                MicrosoftAuthenticator.DeviceCodePrompt prompt = authenticator.requestDeviceCode();
                verificationUri = prompt.verificationUri();
                SwingUtilities.invokeLater(() -> showPrompt(prompt));
                return authenticator.pollForToken(prompt, this::publish);
            }

            @Override
            protected void process(java.util.List<String> messages) {
                if (!messages.isEmpty()) {
                    statusLabel.setText(messages.get(messages.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    signedInAccount = get();
                    dispose();
                } catch (Exception e) {
                    LOG.warn("Microsoft sign-in failed", e);
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    statusLabel.setText("Sign-in failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void showPrompt(MicrosoftAuthenticator.DeviceCodePrompt prompt) {
        statusLabel.setText("Waiting for you to finish signing in…");
        codeField.setText(prompt.userCode());
        openButton.setEnabled(true);
        copyButton.setEnabled(true);
    }

    private void openBrowser() {
        try {
            if (Desktop.isDesktopSupported() && verificationUri != null) {
                Desktop.getDesktop().browse(URI.create(verificationUri));
            }
        } catch (Exception e) {
            statusLabel.setText("Open " + verificationUri + " manually to continue.");
        }
    }

    private void copyCode() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(codeField.getText()), null);
        statusLabel.setText("Code copied to clipboard.");
    }
}
