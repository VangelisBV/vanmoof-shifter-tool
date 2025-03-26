package com.vb.vm.controller;

import com.fazecast.jSerialComm.SerialPort;
import com.vb.vm.service.ModbusService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Swing-based UI for communication with the shifter.
 */

public class SwingUI extends JFrame {
    private JComboBox<String> portSelection;
    private JLabel gearActual;
    private JLabel shiftsActual;
    private JTextField inputField;
    private JButton connectButton, sendButton;

    private ModbusService modbusService;
    private SerialPort selectedPort;

    private int currentGear;
    private int gearInput;


    public SwingUI() {
        setTitle("VM Shifter Tool");
        setSize(500, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top panel (serial port selection)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        // Label
        JLabel portLabel = new JLabel("Select Serial Port:");
        portLabel.setFont(new Font("Arial", Font.BOLD, 16)); 
        // ComboBox
        portSelection = new JComboBox<>();
        portSelection.setFont(new Font("Arial", Font.BOLD, 15));
        // Get serial ports
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portSelection.addItem(port.getSystemPortName());
        }
        // Connect button
        connectButton = new JButton("Connect");

        // Add components to top panel
        topPanel.add(portLabel);
        topPanel.add(portSelection);
        topPanel.add(connectButton);

        // Center panel (shifter values + input gear field)
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        // Total shifts
        shiftsActual = new JLabel("Total shifts: -", SwingConstants.CENTER);
        shiftsActual.setFont(new Font("Arial", Font.BOLD, 20));
        // Gear
        gearActual = new JLabel("Gear: -", SwingConstants.CENTER);
        gearActual.setFont(new Font("Arial", Font.BOLD, 20));
        // Gear input panel 
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        // Label
        JLabel inputLabel = new JLabel("Select gear (1-4):");
        inputLabel.setFont(new Font("Arial", Font.BOLD, 16));
        // Gear input field
        inputField = new JTextField(3); 
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setFont(new Font("Arial", Font.BOLD, 15)); 
        inputField.setPreferredSize(new Dimension(25, 25)); 
        // Up/Down arrow buttons
        JButton upButton = new JButton("▲");
        JButton downButton = new JButton("▼");

        // Add components to input panel
        inputPanel.add(inputLabel);
        inputPanel.add(downButton);
        inputPanel.add(inputField);
        inputPanel.add(upButton);

        // Add components to center panel
        centerPanel.add(shiftsActual);
        centerPanel.add(gearActual);
        centerPanel.add(inputPanel);

        // Bottom panel (Swift button + about)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        // Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sendButton = new JButton("Switch");
        buttonPanel.add(sendButton);
        // Label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
        JLabel infoLabel = new JLabel("<html><center>Version: 1.0<br>VB<br>"
        + "⚠ Use at your own risk ⚠<br>"
        + "The authors will not be liable for any damages you may suffer<br>"
        + "in connection with using, modifying or distributing this software.<br>"
        + "GitHub: VangelisBV/vanmoof-shifter-tool</center></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        labelPanel.add(infoLabel); 

        // Add components to bottom Panel
        bottomPanel.add(buttonPanel);
        bottomPanel.add(labelPanel);

        // Add panels to the frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        connectButton.addActionListener(event -> connectToPort());
        sendButton.addActionListener(event -> sendNewValue());

        // Up arrow click (Increment Value)
        upButton.addActionListener(event -> changeInputFromArrows(1));

        // Down arrow click (Decrement Value)
        downButton.addActionListener(event -> changeInputFromArrows(-1));
        
        // Handle window closing to release resources
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (modbusService != null) {
                    modbusService.close(); 
                }
                System.exit(0);
            }
        });
    }


    // Connect to the selected port
    private void connectToPort() {
        String selectedPortName = (String) portSelection.getSelectedItem();
        if (selectedPortName == null) {
            showMessage("Error", "Please select a serial port.");
            return;
        }

        selectedPort = Arrays.stream(SerialPort.getCommPorts())
                .filter(p -> p.getSystemPortName().equals(selectedPortName))
                .findFirst().orElse(null);

        if (selectedPort != null) {
            try {
                modbusService = new ModbusService(selectedPort);
                updateRegisterValue();
            } catch (Exception e) {
                showMessage("Error", "Failed to connect to port.");                
            }
        } else {
            showMessage("Error", "Failed to connect to port.");
        }
    }


    // Get user gear input
    private void getInputGearValue() {
        try {
            gearInput = Integer.parseInt(inputField.getText().trim());
        } catch (NumberFormatException e) {}
    }


    // Send value to shifter
    private void sendNewValue() {
        if (modbusService == null) {
            showMessage("Error", "No active Modbus connection.");
            return;
        }

        getInputGearValue();
        try {
            if (gearInput < 1 || gearInput > 4 || gearInput == currentGear) {
                throw new NumberFormatException();
            } 
            } catch (NumberFormatException e) {
                showMessage("Invalid Input", "Enter a number between 1 and 4 (not the current gear).");
                return;
            }
        
        if (modbusService.writeRegister(gearInput)) {
            try {
                // Sleep to let (wait for) gear to go to possition before poll 
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateRegisterValue();
            validateGearChange();
        } else {
            showMessage("Error", "Failed to send value.");
        }
    }


    // Read data from shifter
    private void updateRegisterValue() {
        if (modbusService != null) {
            currentGear = modbusService.readRegister(32,2,1);
            int shifts = modbusService.readRegister(32,15,2);

            if (currentGear == -1) {
                showMessage("Error", "Failed to read values: \n 1) Check your connections. \n 2) Check your power supply. \n 3) Check L1, L5 and L20 (000 resistors). \n 4) Check R17 and R18 (22 ohm).");    
                modbusService.close();             
            }

            gearActual.setText("Gear: " + currentGear);
            shiftsActual.setText("Total shifts: " + shifts);
        }
    }


    // Change input from arrows
    private void changeInputFromArrows(int delta) {
        getInputGearValue();
        int newValue = gearInput + delta;
    
        // Ensure the value is between 1-4 
        if (newValue < 1) newValue = 1;
        if (newValue > 4) newValue = 4;
    
        inputField.setText(String.valueOf(newValue));
        gearInput = newValue;
    }    


    // Show messages
    private void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }


    // Check if gear changed
    private void validateGearChange() {
        if (gearInput != currentGear) {
            showMessage("Error", "The gear did not change: \n 1) Check if the small gears inside the shifter are moving freely. \n 2) Check if shifter's motor works. \n 3) Check if the shifter motor's drive gets power supply. \n \n The gear is not in the correct position: \n 1) Check R12 and R26 (100 ohm). \n 2) Check R7 and R25 (10 kohm). \n 3) Check hall senors.");
        }
    }

}
