import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.border.Border;

class RoundedBorder implements Border {
    private int radius;

    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius / 2, radius, radius / 2, radius);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
}

public class NoteApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/note_taking_app";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    private JFrame frame;
    private JTextArea noteContentArea;
    private JTextField noteTitleField;
    private JList<String> notesList;
    private DefaultListModel<String> notesListModel;
    private List<Note> notes;
    private JTextField searchField;
    private Timer suggestionTimer;

    private static final String[] SUGGESTION_WORDS = {
            "important", "urgent", "priority", "focus", "deadline", "task", "reminder",
            "schedule", "agenda", "meeting", "note", "content", "title", "plan",

            "email", "call", "discuss", "reply", "forward", "message", "attachment",

            "project", "goal", "objective", "milestone", "deliverable", "assignment",
            "work", "review", "status", "progress", "update", "follow-up",

            "calendar", "event", "timeline", "strategy", "workflow", "checklist",
            "log", "tracker", "bullet", "outline", "overview",

            "introduction", "conclusion", "summary", "draft", "final", "paragraph",
            "edit", "revision", "highlight", "italic", "bold", "underline", "format",

            "research", "details", "reference", "documentation", "quote", "citation",
            "idea", "brainstorm", "concept", "solution", "analysis", "risk", "success",

            "implementation", "design", "module", "feature", "release", "debug",
            "deploy", "commit", "merge", "repository", "build", "version",

            "today", "tomorrow", "week", "month", "morning", "afternoon",
            "evening", "night", "holiday", "vacation", "trip", "event",

            "goal", "dream", "vision", "mission", "ambition", "achievement",
            "growth", "learning", "effort", "opportunity", "success", "teamwork",

            "write", "read", "create", "update", "delete", "select", "export",
            "filter", "save", "submit", "close", "open", "navigate"
    };

    private JPopupMenu suggestionPopup;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new NoteApp().createUI();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public NoteApp() throws SQLException {
        notes = loadNotesFromDatabase();
        suggestionPopup = new JPopupMenu();

        suggestionPopup.setFocusable(false);
    }

    public void createUI() throws SQLException {
        frame = new JFrame("Notes App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);

        Color darkBackground = new Color(45, 45, 45);
        Color lightText = new Color(230, 230, 230);
        Color accentColor = new Color(30, 144, 255);

        frame.getContentPane().setBackground(darkBackground);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(darkBackground);

        noteTitleField = new JTextField();
        noteTitleField.setBackground(darkBackground);
        noteTitleField.setForeground(lightText);
        noteTitleField.setCaretColor(lightText);
        noteTitleField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(accentColor), "Title",
                0, 0, new Font("Arial", Font.BOLD, 12), lightText));
        mainPanel.add(noteTitleField, BorderLayout.NORTH);

        noteContentArea = new JTextArea();
        noteContentArea.setBackground(darkBackground);
        noteContentArea.setForeground(lightText);
        noteContentArea.setCaretColor(lightText);
        noteContentArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(accentColor), "Content",
                0, 0, new Font("Arial", Font.BOLD, 12), lightText));
        mainPanel.add(new JScrollPane(noteContentArea), BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.setBackground(darkBackground);

        JButton createButton = createStyledButton("Create", accentColor, lightText);
        createButton.addActionListener(e -> createNote());
        buttonsPanel.add(createButton);

        JButton deleteButton = createStyledButton("Delete", accentColor, lightText);
        deleteButton.addActionListener(e -> deleteNote());
        buttonsPanel.add(deleteButton);

        JButton updateButton = createStyledButton("Update", accentColor, lightText);
        updateButton.addActionListener(e -> updateNote());
        buttonsPanel.add(updateButton);

        JButton deselectButton = createStyledButton("Deselect", accentColor, lightText);
        deselectButton.addActionListener(e -> deselectNote());
        buttonsPanel.add(deselectButton);

        JButton exportButton = createStyledButton("Export", accentColor, lightText);
        exportButton.addActionListener(e -> exportNotes());
        buttonsPanel.add(exportButton);

        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        notesListModel = new DefaultListModel<>();
        for (Note note : notes) {
            notesListModel.addElement(note.getTitle());
        }

        notesList = new JList<>(notesListModel);
        notesList.setBackground(new Color(60, 60, 60));
        notesList.setForeground(lightText);
        notesList.setSelectionBackground(accentColor);
        notesList.setSelectionForeground(lightText);
        notesList.setFont(new Font("Arial", Font.PLAIN, 14));
        notesList.addListSelectionListener(e -> loadSelectedNote());

        JScrollPane listScrollPane = new JScrollPane(notesList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, mainPanel);
        splitPane.setDividerLocation(150);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBackground(darkBackground);

        frame.add(splitPane, BorderLayout.CENTER);

        searchField = new JTextField();
        searchField.setBackground(new Color(60, 60, 60));
        searchField.setForeground(new Color(230, 230, 230));
        searchField.setCaretColor(new Color(230, 230, 230));
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(30, 144, 255)), "Search Notes",
                0, 0, new Font("Arial", Font.BOLD, 12), new Color(230, 230, 230)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterNotes();
            }

            public void removeUpdate(DocumentEvent e) {
                filterNotes();
            }

            public void changedUpdate(DocumentEvent e) {
                filterNotes();
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(45, 45, 45));
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(notesList), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(mainPanel);
        splitPane.setDividerLocation(150);
        splitPane.setOneTouchExpandable(true);
        frame.add(splitPane, BorderLayout.CENTER);

        noteContentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                showSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                showSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Not needed for plain-text fields
            }
        });

        frame.setVisible(true);
    }

    private JButton createStyledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBorder(new RoundedBorder(20));
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setMargin(new Insets(5, 10, 5, 10));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(background.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(background);
            }
        });

        return button;
    }

    private void deselectNote() {
        notesList.clearSelection();
        noteTitleField.setText("");
        noteContentArea.setText("");
    }

    private void createNote() {
        String title = noteTitleField.getText();
        String content = noteContentArea.getText();

        if (!title.isEmpty() && !content.isEmpty()) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    PreparedStatement stmt = conn
                            .prepareStatement("INSERT INTO notes (title, content) VALUES (?, ?)")) {

                stmt.setString(1, title);
                stmt.setString(2, content);
                stmt.executeUpdate();

                notes.add(new Note(title, content));
                notesListModel.addElement(title);

                noteTitleField.setText("");
                noteContentArea.setText("");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please fill in both title and content.");
        }
    }

    private void updateNote() {
        String title = noteTitleField.getText();
        String content = noteContentArea.getText();

        int selectedIndex = notesList.getSelectedIndex();
        if (selectedIndex != -1) {
            Note note = notes.get(selectedIndex);
            note.setTitle(title);
            note.setContent(content);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    PreparedStatement stmt = conn
                            .prepareStatement("UPDATE notes SET title = ?, content = ? WHERE id = ?")) {

                stmt.setString(1, title);
                stmt.setString(2, content);
                stmt.setInt(3, note.getId());
                stmt.executeUpdate();

                notesListModel.set(selectedIndex, title);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteNote() {
        int selectedIndex = notesList.getSelectedIndex();
        if (selectedIndex != -1) {
            Note note = notes.get(selectedIndex);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM notes WHERE id = ?")) {

                stmt.setInt(1, note.getId());
                stmt.executeUpdate();

                notes.remove(selectedIndex);
                notesListModel.remove(selectedIndex);

                noteTitleField.setText("");
                noteContentArea.setText("");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadSelectedNote() {
        int selectedIndex = notesList.getSelectedIndex();
        if (selectedIndex != -1) {
            Note note = notes.get(selectedIndex);
            noteTitleField.setText(note.getTitle());
            noteContentArea.setText(note.getContent());
        }
    }

    private void displaySuggestions() {
        String text = noteContentArea.getText();
        String[] words = text.split("\\s+");
        String lastWord = words.length > 0 ? words[words.length - 1] : "";

        suggestionPopup.removeAll();

        for (String suggestion : SUGGESTION_WORDS) {
            if (suggestion.startsWith(lastWord)) {
                JMenuItem suggestionItem = new JMenuItem(suggestion);
                suggestionItem.addActionListener(e -> {
                    // Replace the last word with the suggestion and add a space
                    String newText = text.substring(0, text.lastIndexOf(lastWord)) + suggestion + " ";
                    noteContentArea.setText(newText);

                    // Hide the popup after selecting the suggestion
                    suggestionPopup.setVisible(false);
                });

                suggestionPopup.add(suggestionItem);
            }
        }

        if (suggestionPopup.getComponentCount() > 0) {
            try {
                int position = noteContentArea.getCaretPosition();
                Point location = noteContentArea.modelToView2D(position).getBounds().getLocation();
                suggestionPopup.show(noteContentArea, location.x, location.y + 20);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        } else {
            suggestionPopup.setVisible(false);
        }
    }

    private void showSuggestions() {
        if(noteContentArea.getText().isBlank()) {
            suggestionPopup.setVisible(false);

            return;
        }

        if (suggestionTimer != null && suggestionTimer.isRunning()) {
            suggestionTimer.stop();
        }

        suggestionTimer = new Timer(300, e -> {
            suggestionTimer.stop(); // Stop the timer after execution
            displaySuggestions();
        });
        suggestionTimer.setRepeats(false);
        suggestionTimer.start();
    }

    private List<Note> loadNotesFromDatabase() throws SQLException {
        List<Note> noteList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM notes")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String content = rs.getString("content");
                noteList.add(new Note(id, title, content));
            }
        }

        return noteList;
    }

    private void filterNotes() {
        String query = searchField.getText().toLowerCase();
        notesListModel.clear();
        for (Note note : notes) {
            if (note.getTitle().toLowerCase().contains(query) || note.getContent().toLowerCase().contains(query)) {
                notesListModel.addElement(note.getTitle());
            }
        }
    }

    private void exportNotes() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Notes");
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Note note : notes) {
                    writer.newLine();
                    writer.write("Title: " + note.getTitle());
                    writer.newLine();
                    writer.write("Content: " + note.getContent());
                    writer.newLine();
                    writer.newLine();
                    writer.write("-----------------------------");
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Note {
    private int id;
    private String title;
    private String content;

    public Note(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public Note(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
