import jssc.SerialPort;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DobotOnly {

    private static final String DOBOT_PORT_NAME = "/dev/tty.usbserial-0001";

    private SerialPort serialPort;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DobotOnly().createAndShowGui());
    }

    private void createAndShowGui() {
        JFrame frame = new JFrame("Dobot Only");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel lblMode = new JLabel("PTP Mode (0-8):");
        JTextField txtMode = new JTextField("1", 8);

        JLabel lblX = new JLabel("X:");
        JTextField txtX = new JTextField("0", 8);

        JLabel lblY = new JLabel("Y:");
        JTextField txtY = new JTextField("0", 8);

        JLabel lblZ = new JLabel("Z:");
        JTextField txtZ = new JTextField("0", 8);

        JLabel lblR = new JLabel("R:");
        JTextField txtR = new JTextField("0", 8);

        JButton btnSendPTP = new JButton("Send PTP");
        JButton btnSetHome = new JButton("Set Home");
        JButton btnSuctionOn = new JButton("Suction ON");
        JButton btnSuctionOff = new JButton("Suction OFF");

        btnSendPTP.addActionListener(e -> {
            try {
                int mode = Integer.parseInt(txtMode.getText().trim());
                float x = Float.parseFloat(txtX.getText().trim());
                float y = Float.parseFloat(txtY.getText().trim());
                float z = Float.parseFloat(txtZ.getText().trim());
                float r = Float.parseFloat(txtR.getText().trim());

                String packet = setPTPCmd(mode, x, y, z, r);
                sendPacket(packet);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSetHome.addActionListener(e -> {
            String packet = setHomeCmd();
            sendPacket(packet);
        });

        btnSuctionOn.addActionListener(e -> {
            String packet = setSuctionCmd(true);
            sendPacket(packet);
        });

        btnSuctionOff.addActionListener(e -> {
            String packet = setSuctionCmd(false);
            sendPacket(packet);
        });

        JPanel grid = new JPanel(new GridLayout(5, 2, 8, 8));
        grid.add(lblMode);
        grid.add(txtMode);
        grid.add(lblX);
        grid.add(txtX);
        grid.add(lblY);
        grid.add(txtY);
        grid.add(lblZ);
        grid.add(txtZ);
        grid.add(lblR);
        grid.add(txtR);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(grid, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        buttonPanel.add(btnSendPTP);
        buttonPanel.add(btnSetHome);
        buttonPanel.add(btnSuctionOn);
        buttonPanel.add(btnSuctionOff);
        root.add(buttonPanel, BorderLayout.SOUTH);

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
            serialPort = new SerialPort(DOBOT_PORT_NAME);
            serialPort.openPort();
            serialPort.setParams(
                    SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE
            );
            System.out.println("Serial opened: " + DOBOT_PORT_NAME);
        } catch (SerialPortException ex) {
            System.err.println("Failed to open serial port: " + ex.getMessage());
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            try {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                    System.out.println("Serial closed");
                }
            } catch (SerialPortException ex) {
                System.err.println("Failed to close serial port: " + ex.getMessage());
            }
        }
    }

    private void sendPacket(String hexPacket) {
        if (serialPort == null) {
            System.err.println("Serial port not initialized.");
            return;
        }
        try {
            byte[] bytes = decodeHexString(hexPacket);
            serialPort.writeBytes(bytes);
            System.out.println("Sent: " + hexPacket);
        } catch (Exception ex) {
            System.err.println("Send failed: " + ex.getMessage());
        }
    }

    // ---------- Packet builders ----------

    // Example packet spec:
    // Header: AA AA
    // Length: 13
    // ID: 54
    // Ctrl: 03 (rw=1, isQueued=1)
    // Params: ptpMode (1 byte) + x + y + z + r (float32 LE)
    public static String setPTPCmd(int ptpMode, float x, float y, float z, float r) {
        String header = "AAAA";
        String payloadLen = "13";
        String id = "54";
        String ctrl = "03";

        String paramMode = String.format("%02X", ptpMode & 0xFF);
        String paramX = floatToHexLE(x);
        String paramY = floatToHexLE(y);
        String paramZ = floatToHexLE(z);
        String paramR = floatToHexLE(r);

        String payload = id + ctrl + paramMode + paramX + paramY + paramZ + paramR;
        String checksum = checksum(payload);

        return header + payloadLen + payload + checksum;
    }

    // Set Home command packet builder
    public static String setHomeCmd() {
        String header = "AAAA";
        String payloadLen = "06";
        String id = "1F";
        String ctrl = "01";
        String params = "00000000";
        String payload = id + ctrl + params;
        String checksum = checksum(payload);
        return header + payloadLen + payload + checksum;
    }

    // Suction command packet builder
    public static String setSuctionCmd(boolean on) {
        String header = "AAAA";
        String payloadLen = "04";
        String id = "3E";
        String ctrl = "01";
        String isCtrlEnabled = on ? "01" : "00";
        String isSucked = on ? "01" : "00";
        String payload = id + ctrl + isCtrlEnabled + isSucked;
        String checksum = checksum(payload);
        return header + payloadLen + payload + checksum;
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
            sum = (sum + (b & 0xFF)) & 0xFF; // keep 8-bit
        }
        int chk = (256 - sum) & 0xFF;
        return String.format("%02X", chk);
    }

    public static String floatToHexLE(float value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(value);
        byte[] b = bb.array();
        return String.format("%02X%02X%02X%02X", b[0], b[1], b[2], b[3]);
    }
}
