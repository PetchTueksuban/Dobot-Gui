
import jssc.SerialPort;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConveyorOnly {

    private static final String CONVEYOR_PORT_NAME = "/dev/tty.usbserial-0001";
    private static final int NORMAL_CONVEYOR_SPEED = 10000; // int speed (signed)

    private SerialPort conveyorPort;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConveyorOnly().createAndShowGui());
    }

    private void createAndShowGui() {
        JFrame frame = new JFrame("Conveyor Only");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton btnConveyorOn = new JButton("Conveyor ON");
        JButton btnConveyorOff = new JButton("Conveyor OFF");

        String onPacket = setEMotorCmd(0, true, NORMAL_CONVEYOR_SPEED);
        String offPacket = setEMotorCmd(0, false, 0);

        JLabel lblOnPacket = new JLabel("ON packet:");
        JTextField txtOnPacket = new JTextField(onPacket, 28);
        txtOnPacket.setEditable(false);

        JLabel lblOffPacket = new JLabel("OFF packet:");
        JTextField txtOffPacket = new JTextField(offPacket, 28);
        txtOffPacket.setEditable(false);

        btnConveyorOn.addActionListener(e -> {
            System.out.println("Command ON packet: " + onPacket);
            sendPacket(onPacket);
        });

        btnConveyorOff.addActionListener(e -> {
            System.out.println("Command OFF packet: " + offPacket);
            sendPacket(offPacket);
        });

        JPanel packetGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        packetGrid.add(lblOnPacket);
        packetGrid.add(txtOnPacket);
        packetGrid.add(lblOffPacket);
        packetGrid.add(txtOffPacket);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.add(btnConveyorOn);
        buttons.add(btnConveyorOff);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(packetGrid, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);

        openSerialPort();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeSerialPort();
            }
        });

        frame.setVisible(true);
    }

    private void openSerialPort() {
        try {
            conveyorPort = new SerialPort(CONVEYOR_PORT_NAME);
            conveyorPort.openPort();
            conveyorPort.setParams(
                    SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE
            );
            System.out.println("Conveyor serial opened: " + CONVEYOR_PORT_NAME);
        } catch (SerialPortException ex) {
            System.err.println("Failed to open conveyor serial port: " + ex.getMessage());
        }
    }

    private void closeSerialPort() {
        if (conveyorPort != null) {
            try {
                if (conveyorPort.isOpened()) {
                    conveyorPort.closePort();
                    System.out.println("Conveyor serial closed");
                }
            } catch (SerialPortException ex) {
                System.err.println("Failed to close conveyor serial port: " + ex.getMessage());
            }
        }
    }

    private void sendPacket(String hexPacket) {
        if (conveyorPort == null) {
            System.err.println("Conveyor serial port not initialized.");
            return;
        }
        try {
            byte[] bytes = decodeHexString(hexPacket);
            conveyorPort.writeBytes(bytes);
            System.out.println("Sent: " + hexPacket);
        } catch (Exception ex) {
            System.err.println("Send failed: " + ex.getMessage());
        }
    }

    // EMotor command (ID 135 / 0x87)
    // Params: index (uint8) + isEnabled (uint8) + speed (int32 LE)
    public static String setEMotorCmd(int index, boolean isEnabled, int speed) {
        String header = "AAAA";
        String payloadLen = "08";
        String id = "87";
        String ctrl = "01"; // matches Sagar_Work8

        String paramIndex = String.format("%02X", index & 0xFF);
        String paramEnabled = isEnabled ? "01" : "00";
        String paramSpeed = intToHexLE(speed);

        String payload = id + ctrl + paramIndex + paramEnabled + paramSpeed;
        String checksum = checksum(payload);

        return header + payloadLen + payload + checksum;
    }

    public static String intToHexLE(int value) {
        return String.format(
                "%02X%02X%02X%02X",
                value & 0xFF,
                (value >> 8) & 0xFF,
                (value >> 16) & 0xFF,
                (value >> 24) & 0xFF
        );
    }

    // ---------- Hex utilities ----------

    public static int toDigit(char hexChar) {
        return Character.digit(hexChar, 16);
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException("Hex string length must be even.");
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    public static String checksum(String strHexPayload) {
        int sum = 0;
        byte[] byteArray = decodeHexString(strHexPayload);
        for (byte b : byteArray) {
            sum = sum + (b & 0xFF);
        }
        int chkSUM = (256 - (sum % 256)) & 0xFF;
        return String.format("%02X", chkSUM);
    }

    public static String floatToHexLE(float value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(value);
        byte[] b = bb.array();
        return String.format("%02X%02X%02X%02X", b[0], b[1], b[2], b[3]);
    }
}
