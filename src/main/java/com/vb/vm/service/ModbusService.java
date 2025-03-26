package com.vb.vm.service;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.msg.WriteRegisterRequest;

/**
 * Modbus connection service. 
 */

public class ModbusService {
    private final SerialPort serialPort;
    private ModbusMaster master;

    // Get and serial port and initialize modbus connection
    public ModbusService(SerialPort serialPort) {
        this.serialPort = serialPort;
        initModbusMaster();
    }

    private void initModbusMaster() {
        try {
            SerialPortWrapperImpl wrapper = new SerialPortWrapperImpl(serialPort);
            master = new ModbusFactory().createRtuMaster(wrapper);
            master.init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Modbus master: " + e.getMessage());
        }
    }

    // Read Modbus registers
    public int readRegister(int slaveId, int registerAddress, int numberOfRegisters) {
        if (master == null) {
            throw new IllegalStateException("Modbus master is not initialized.");
        }

        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, registerAddress, numberOfRegisters);
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
            if (numberOfRegisters == 1) {
                return response.getShortData()[0]; // If only one register, return it directly
            } else if (numberOfRegisters == 2) {
                // Else if 2 registers, combine the two 16-bit values into a 32-bit integer
                return ((response.getShortData()[0] & 0xFFFF) << 16) | (response.getShortData()[1] & 0xFFFF);
            } else {
                return -1;
            }
        } catch (ModbusTransportException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Write Modbus register
    public boolean writeRegister(int value) {
        if (master == null) {
            throw new IllegalStateException("Modbus master is not initialized.");
        }

        try {
            WriteRegisterRequest request = new WriteRegisterRequest(32, 2, value);
            master.send(request);
            return true;
        } catch (ModbusTransportException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Close Modbus Connection
    public void close() {
        if (master != null) {
            master.destroy();
        }
        serialPort.closePort();
    }
}
