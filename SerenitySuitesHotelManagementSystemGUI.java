import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.Vector;

public class SerenitySuitesHotelManagementSystemGUI {
    private JFrame frame;
    private JTable roomTable;
    private DefaultTableModel roomTableModel;
    private Vector<Room> rooms = new Vector<>();

    public SerenitySuitesHotelManagementSystemGUI() {
        frame = new JFrame("Serenity Suites Hotel Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        loadRoomsFromDatabase();
        initializeGUI();
        frame.setVisible(true);
    }

    private void initializeGUI() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Welcome to Serenity Suites Hotel", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 24));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Room Table
        roomTableModel = new DefaultTableModel(new Object[]{"Room Number", "Type", "Available", "Price"}, 0);
        roomTable = new JTable(roomTableModel);
        JScrollPane scrollPane = new JScrollPane(roomTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton bookRoomButton = new JButton("Book Room");
        JButton viewRoomsButton = new JButton("View Available Rooms");
        JButton exitButton = new JButton("Exit");

        bookRoomButton.addActionListener(this::handleBookRoom);
        viewRoomsButton.addActionListener(e -> loadAvailableRooms());
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(bookRoomButton);
        buttonPanel.add(viewRoomsButton);
        buttonPanel.add(exitButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(panel);
        loadAvailableRooms();
    }

    private void loadRoomsFromDatabase() {
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {

            while (rs.next()) {
                rooms.add(new Room(
                        rs.getInt("room_number"),
                        rs.getString("type"),
                        rs.getBoolean("available"),
                        rs.getDouble("price")
                ));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading rooms: " + e.getMessage());
        }
    }

    private void loadAvailableRooms() {
        roomTableModel.setRowCount(0);
        rooms.stream().filter(Room::isAvailable).forEach(room -> roomTableModel.addRow(new Object[]{
                room.getRoomNumber(), room.getType(), room.isAvailable() ? "Yes" : "No", room.getPrice()
        }));
    }

    private void handleBookRoom(ActionEvent event) {
        JPanel bookingPanel = new JPanel(new GridLayout(6, 2));
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();

        bookingPanel.add(new JLabel("Customer Name:"));
        bookingPanel.add(nameField);
        bookingPanel.add(new JLabel("Contact:"));
        bookingPanel.add(contactField);
        bookingPanel.add(new JLabel("Address:"));
        bookingPanel.add(addressField);
        bookingPanel.add(new JLabel("Email:"));
        bookingPanel.add(emailField);

        int result = JOptionPane.showConfirmDialog(frame, bookingPanel, "Enter Booking Details", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Vector<Room> selectedRooms = new Vector<>();
            double totalAmount = 0;

            while (true) {
                String roomNumberStr = JOptionPane.showInputDialog(frame, "Enter room number to book (or cancel to finish):");
                if (roomNumberStr == null || roomNumberStr.isEmpty()) break;

                try {
                    int roomNumber = Integer.parseInt(roomNumberStr);
                    Room room = rooms.stream().filter(r -> r.getRoomNumber() == roomNumber && r.isAvailable()).findFirst().orElse(null);
                    if (room != null) {
                        selectedRooms.add(room);
                        room.setAvailable(false);
                        totalAmount += room.getPrice();
                        JOptionPane.showMessageDialog(frame, "Room " + roomNumber + " booked successfully.");
                    } else {
                        JOptionPane.showMessageDialog(frame, "Room not available or invalid room number.");
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(frame, "Invalid room number format.");
                }
            }

            if (!selectedRooms.isEmpty()) {
                saveBookingToDatabase(nameField.getText(), contactField.getText(), addressField.getText(), emailField.getText(), selectedRooms);
                loadAvailableRooms();
                JOptionPane.showMessageDialog(frame, "Booking Complete! Total Amount: " + totalAmount);
            } else {
                JOptionPane.showMessageDialog(frame, "No rooms booked. Please book at least one room.");
            }
        }
    }

    private void saveBookingToDatabase(String name, String contact, String address, String email, Vector<Room> bookedRooms) {
        try (Connection conn = DBHelper.getConnection()) {
            for (Room room : bookedRooms) {
                String insertBooking = "INSERT INTO bookings (customer_name, contact, address, email, room_number) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement bookingStmt = conn.prepareStatement(insertBooking);
                bookingStmt.setString(1, name);
                bookingStmt.setString(2, contact);
                bookingStmt.setString(3, address);
                bookingStmt.setString(4, email);
                bookingStmt.setInt(5, room.getRoomNumber());
                bookingStmt.executeUpdate();

                String updateRoom = "UPDATE rooms SET available = FALSE WHERE room_number = ?";
                PreparedStatement roomStmt = conn.prepareStatement(updateRoom);
                roomStmt.setInt(1, room.getRoomNumber());
                roomStmt.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error saving booking: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SerenitySuitesHotelManagementSystemGUI::new);
    }
}

// DBHelper class remains the same as before.
