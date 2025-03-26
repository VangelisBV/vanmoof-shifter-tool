package com.vb.vm;

import com.vb.vm.controller.SwingUI;

import javax.swing.*;

/**
 * Main entry point for the Swing application.
 */

public class MainApp {
    public static void main(String[] args) {
        // Run UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new SwingUI().setVisible(true));
    }
}

