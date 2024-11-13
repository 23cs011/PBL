import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;

public class NotesApp extends JFrame {
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JTable notesTable;
    private DefaultTableModel tableModel;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/notes_db";
    private static final String USER = "root";
    private static final String PASS = "1234";

    public NotesApp() {
        setTitle("Notes Taking App");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        initUI();
        loadNotes();
    }

    private void initUI() {
        GradientPanel gradientPanel = new GradientPanel();
        gradientPanel.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 1));

        titleField = new JTextField();
        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);

        descriptionArea = new JTextArea();
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(new JScrollPane(descriptionArea));

        JButton addButton = new JButton("Add Note");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNote();
            }
        });

        JButton deleteButton = new JButton("Delete Note");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteNote();
            }
        });

        inputPanel.add(addButton);
        inputPanel.add(deleteButton);
        gradientPanel.add(inputPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[] { "ID", "Title", "Description" }, 0);
        notesTable = new JTable(tableModel);
        gradientPanel.add(new JScrollPane(notesTable), BorderLayout.CENTER);

        add(gradientPanel);
    }

    private void loadNotes() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM notes")) {

            tableModel.setRowCount(0);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("description"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addNote() {
        String title = titleField.getText();
        String description = descriptionArea.getText();

        if (title.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both title and description.");
            return;
        }

        String query = "INSERT INTO notes (title, description) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.executeUpdate();
            loadNotes();
            titleField.setText("");
            descriptionArea.setText("");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteNote() {
        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a note to delete.");
            return;
        }

        int noteId = (int) tableModel.getValueAt(selectedRow, 0);

        String query = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, noteId);
            pstmt.executeUpdate();
            loadNotes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NotesApp app = new NotesApp();
            app.setVisible(true);
        });
    }

    class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            Color startColor = new Color(173, 216, 230);
            Color endColor = new Color(70, 130, 180);

            GradientPaint gradientPaint = new GradientPaint(0, 0, startColor, 0, height, endColor);
            g2d.setPaint(gradientPaint);
            g2d.fillRect(0, 0, width, height);
        }
    }
}
