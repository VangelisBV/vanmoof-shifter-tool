package com.vb.vm.service;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper class for integrating jSerialComm with Modbus4J. (Modbus4J expects a serial port wrapper)
 */

public class SerialPortWrapperImpl implements SerialPortWrapper {
    private final SerialPort serialPort;

    // Initialize the wrapper with given serial port
    public SerialPortWrapperImpl(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public void close() throws Exception {
        serialPort.closePort();
    }

    @Override
    public void open() throws Exception {
        if (!serialPort.openPort()) {
            throw new Exception("Failed to open serial port: " + serialPort.getSystemPortName());
        }
    }
 
    @Override
    public int getBaudRate() {
        return serialPort.getBaudRate();
    }

    @Override
    public int getFlowControlIn() {
        return SerialPort.FLOW_CONTROL_DISABLED;
    }

    @Override
    public int getFlowControlOut() {
        return SerialPort.FLOW_CONTROL_DISABLED;
    }

    @Override
    public int getDataBits() {
        return serialPort.getNumDataBits();
    }

    @Override
    public int getStopBits() {
        return serialPort.getNumStopBits();
    }

    @Override
    public int getParity() {
        return serialPort.getParity();
    }

    @Override
    public InputStream getInputStream() {
        return serialPort.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return serialPort.getOutputStream();
    }
}

