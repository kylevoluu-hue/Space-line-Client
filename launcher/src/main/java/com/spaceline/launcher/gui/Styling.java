package com.spaceline.launcher.gui;

import java.awt.Insets;

import javax.swing.UIManager;

/**
 * Central place for the launcher's "bubble smooth" look. These UIManager defaults
 * must be re-applied every time the FlatLaf look-and-feel is (re)installed,
 * because {@code FlatXxxLaf.setup()} resets the defaults — so both startup and
 * {@code applyAppearance()} call {@link #applyDefaults()}.
 */
public final class Styling {

    private Styling() {
    }

    public static void applyDefaults() {
        // Rounded everything.
        UIManager.put("Button.arc", 22);
        UIManager.put("Component.arc", 18);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("CheckBox.arc", 8);

        // Roomier, softer controls.
        UIManager.put("Button.margin", new Insets(7, 16, 7, 16));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Button.innerFocusWidth", 1);

        // Slim, pill-shaped scrollbars.
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", new java.awt.Color(0, 0, 0, 0));
        UIManager.put("ScrollPane.smoothScrolling", true);

        // Cleaner lists, tabs and tables.
        UIManager.put("List.selectionArc", 12);
        UIManager.put("List.cellMargins", new Insets(4, 8, 4, 8));
        UIManager.put("TabbedPane.tabArc", 14);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TitlePane.unifiedBackground", true);

        // Rounded window corners on Windows 11.
        UIManager.put("TitlePane.useWindowDecorations", false);
    }
}
