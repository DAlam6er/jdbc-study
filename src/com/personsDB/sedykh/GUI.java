package com.personsDB.sedykh;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.text.NumberFormat;

public class GUI
{
    private JPanel jpanMain;
    private JButton jbtnUpload;
    private JButton jbtnShowAll;
    private JButton jbtnSortByName;
    private JButton jbtnShowAtAge;
    private JTextField jtxtName;
    private JFormattedTextField jtxtAge;
    private JLabel jlblAge;
    private JLabel jlblName;
    private JScrollPane jscrlPane;
    private JTable jtblPersonData;
    private JFrame jFrame;
    private JMenuItem jMenuItemOpen;
    private JMenuItem jMenuItemClose;

    private Driver driver = null;
    private Connection connection = null;

    public void buildGUI()
    {
        jFrame = getFrame();
        jFrame.add(jpanMain);
        setPanelEnabled(jpanMain, false);
        addMenuBar();
        jFrame.setVisible(true);

        jtxtName.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }
        });
        jtxtName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jtxtName.transferFocus();
                }
            }
        });

        NumberFormat format = NumberFormat.getInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(0);
        formatter.setMaximum(150);
        formatter.setAllowsInvalid(true);
        // If you want the value to be committed on each keystroke instead of focus lost
        formatter.setCommitsOnValidEdit(false);
        jtxtAge.setFormatterFactory(new DefaultFormatterFactory(formatter));
        jtxtAge.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                checkTxtFieldsFull();
            }
        });
        jtxtAge.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jbtnUpload.requestFocusInWindow();
                }
            }
        });

        jFrame.getRootPane().setDefaultButton(jbtnUpload);
        jbtnUpload.addActionListener(e ->
        {
            try {
                JDBCModel.uploadData(
                    connection, "db",
                    jtxtName.getText(), jtxtAge.getText());
                jtxtName.setText(null);
                jtxtAge.setText(null);
                JOptionPane.showMessageDialog(
                    jFrame, "Record was successfully uploaded!",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(jFrame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        jbtnShowAll.addActionListener(e -> {
            try (ResultSet rs = JDBCModel.showAllData(connection, "db")) {
                if (rs != null) {
                    populateTable(rs);
                    jbtnSortByName.setEnabled(true);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(jFrame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        jbtnSortByName.addActionListener(e -> {
            try (ResultSet rs = JDBCModel.sortByName(connection, "db")) {
                if (rs != null) {
                    populateTable(rs);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(jFrame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        jbtnShowAtAge.addActionListener(e -> {
            try (ResultSet rs = JDBCModel.showAtAge(
                connection, "db", jtxtAge.getText())) {
                if (rs != null) {
                    populateTable(rs);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(jFrame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void populateTable(ResultSet rs) throws SQLException
    {
        DefaultTableModel tableModel = new DefaultTableModel();
        ResultSetMetaData metaData = rs.getMetaData();

        int columnCount = metaData.getColumnCount();
        //Get all column names from metadata and add columns to table model
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            tableModel.addColumn(metaData.getColumnLabel(columnIndex));
        }

        //Create array of Objects with size of column count from metadata
        Object[] row = new Object[columnCount];

        //Scroll through ResultSet
        while (rs.next()) {
            //Get object from column with specific index of result set to array of objects
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            // Add row to table model with that array of objects
            tableModel.addRow(row);
        }

        // Sets the data model for this table to dataModel
        // and registers with it for listener notifications from the new data model.
        jtblPersonData.setModel(tableModel);
    }

    private void setPanelEnabled(JPanel panel, boolean isEnabled)
    {
        panel.setEnabled(isEnabled);
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                setPanelEnabled((JPanel) component, isEnabled);
            }
            component.setEnabled(isEnabled);
            jbtnUpload.setEnabled(false);
            jbtnShowAtAge.setEnabled(false);
            jbtnSortByName.setEnabled(false);

            jtxtName.requestFocusInWindow();
        }
    }

    private void addMenuBar()
    {
        JMenuBar jMenuBar = new JMenuBar();
        JMenu jmnuFile = new JMenu("File");
        jmnuFile.setMnemonic('F');

        jMenuItemOpen = new JMenuItem("Connect to ...", 'O');
        jMenuItemOpen.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        jMenuItemOpen.addActionListener(e ->
            new DBLoadDriverDialog().setVisible(true));

        jMenuItemClose = new JMenuItem("Disconnect", 'D');
        jMenuItemClose.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
        jMenuItemClose.addActionListener(e -> actionsOnDisconnect());
        jMenuItemClose.setEnabled(false);

        JMenuItem jMenuItemExit = new JMenuItem("Exit", 'x');
        jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke("ctrl X"));
        jMenuItemExit.addActionListener(e -> jFrame.dispose());

        jmnuFile.add(jMenuItemOpen);
        jmnuFile.addSeparator();
        jmnuFile.add(jMenuItemClose);
        jmnuFile.add(jMenuItemExit);

        jMenuBar.add(jmnuFile);
        jFrame.setJMenuBar(jMenuBar);
    }

    private void actionsOnDisconnect()
    {
        try {
            connection.close();
            jtxtName.setText(null);
            jtxtAge.setText(null);
            DefaultTableModel model = (DefaultTableModel) jtblPersonData.getModel();
            model.setRowCount(0);
            JOptionPane.showMessageDialog(jFrame,
                "Connection was closed",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            setPanelEnabled(jpanMain, false);
            changeConnStatus();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(jFrame,
                ex.getMessage(),
                "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void changeConnStatus()
    {
        jMenuItemOpen.setEnabled(!jMenuItemOpen.isEnabled());
        jMenuItemClose.setEnabled(!jMenuItemClose.isEnabled());
    }

    private void checkTxtFieldsFull()
    {
        jbtnShowAtAge.setEnabled(!jtxtAge.getText().trim().isEmpty());

        jbtnUpload.setEnabled(!jtxtName.getText().trim().isEmpty() &&
            !jtxtAge.getText().trim().isEmpty());
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        jpanMain = new JPanel();
        jpanMain.setLayout(new BorderLayout(0, 0));
        jpanMain.setPreferredSize(new Dimension(400, 300));
        jpanMain.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        jpanMain.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.SOUTH);
        jbtnUpload = new JButton();
        jbtnUpload.setEnabled(false);
        jbtnUpload.setText("upload");
        panel2.add(jbtnUpload);
        jbtnShowAll = new JButton();
        jbtnShowAll.setText("show all");
        panel2.add(jbtnShowAll);
        jbtnSortByName = new JButton();
        jbtnSortByName.setText("sort by name");
        panel2.add(jbtnSortByName);
        jbtnShowAtAge = new JButton();
        jbtnShowAtAge.setEnabled(false);
        jbtnShowAtAge.setText("show at age");
        panel2.add(jbtnShowAtAge);
        jscrlPane = new JScrollPane();
        panel1.add(jscrlPane, BorderLayout.CENTER);
        jtblPersonData = new JTable();
        jscrlPane.setViewportView(jtblPersonData);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpanMain.add(panel3, BorderLayout.NORTH);
        jtxtAge = new JFormattedTextField();
        panel3.add(jtxtAge, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 20), null, 0, false));
        jtxtName = new JTextField();
        panel3.add(jtxtName, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 20), null, 0, false));
        jlblAge = new JLabel();
        jlblAge.setText("Age");
        panel3.add(jlblAge, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 5, false));
        jlblName = new JLabel();
        jlblName.setText("Name");
        panel3.add(jlblName, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 5, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 50), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 50), null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel3.add(spacer4, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel3.add(spacer5, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        jpanMain.add(spacer6, BorderLayout.EAST);
        final Spacer spacer7 = new Spacer();
        jpanMain.add(spacer7, BorderLayout.WEST);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return jpanMain;
    }

    private void createUIComponents()
    {
    }

    private class DBLoadDriverDialog extends JDialog
    {
        public DBLoadDriverDialog()
        {
            super(jFrame, "Choose DBMS", true);
            JPanel jPanelcomboBox =
                new JPanel(new GridLayout(2, 2, 0, 2));

            JComboBox<String> jComboBoxDBMS = new JComboBox<>();
            jComboBoxDBMS.addItem("Oracle");
            jComboBoxDBMS.addItem("MSSQL");
            jComboBoxDBMS.addItem("PostgreSQL");
            jComboBoxDBMS.addItem("MySQL");
            jComboBoxDBMS.addItem("Derby");
            jComboBoxDBMS.setSelectedIndex(-1);

            JLabel jlblDriverStatus = new JLabel();
            JButton jButtonConnectToDB = new JButton("OK");
            jButtonConnectToDB.setEnabled(false);

            jComboBoxDBMS.addActionListener(e -> {
                JComboBox<String> box = (JComboBox<String>) e.getSource();
                String dbmsName = (String) box.getSelectedItem();

                try {
                    driver = JDBCModel.loadDriver(dbmsName);
                    jlblDriverStatus.setText("Driver loaded successfully!");
                    jlblDriverStatus.setForeground(Color.green);
                    jButtonConnectToDB.setEnabled(true);
                } catch (ClassNotFoundException |
                         IllegalArgumentException ex) {
                    jlblDriverStatus.setText(ex.getMessage());
                    jlblDriverStatus.setForeground(Color.red);
                }
            });

            jPanelcomboBox.add(jComboBoxDBMS);
            jPanelcomboBox.add(jlblDriverStatus);
            add(jPanelcomboBox, BorderLayout.CENTER);

            JPanel jPanelButton = new JPanel();

            jPanelButton.add(jButtonConnectToDB);
            jButtonConnectToDB.addActionListener(e -> {
                dispose();
                new DBgetConnectionDialog().setVisible(true);
            });

            add(jPanelButton, BorderLayout.SOUTH);
            setSize(300, 150);
            setLocationRelativeTo(null);
        }
    }

    private class DBgetConnectionDialog extends JDialog
    {
        private final JTextField jtxtUserName = new JTextField(10);
        private final JPasswordField jpassPassword = new JPasswordField();
        private final JButton jbtnOK = new JButton("OK");

        public DBgetConnectionDialog()
        {
            super(jFrame, "Input user name and password", true);
            JPanel jpanTextFields = new JPanel(new GridLayout(4, 2, 5, 10));
            jpanTextFields.add(new JLabel("user name"));
            jpanTextFields.add(jtxtUserName);
            jpanTextFields.add(new JLabel("password"));
            jpanTextFields.add(jpassPassword);
            jpassPassword.setEnabled(false);

            jbtnOK.setEnabled(false);
            jbtnOK.addActionListener(e -> btnOKpressedHandling());
            txtFieldChangesHandling(jtxtUserName);
            txtFieldChangesHandling(jpassPassword);


            JPanel jpanButton = new JPanel();
            jpanButton.add(jbtnOK);

            add(jpanTextFields, BorderLayout.CENTER);
            add(jpanButton, BorderLayout.SOUTH);
            setSize(300, 200);
            setLocationRelativeTo(null);
        }

        private void txtFieldChangesHandling(JTextField jtxtField)
        {
            jtxtField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    checkTxtFieldsFull();
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    checkTxtFieldsFull();
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    checkTxtFieldsFull();
                }
            });
            jtxtField.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (!(jtxtField instanceof JPasswordField)) {
                            jtxtField.transferFocus();
                        } else {
                            btnOKpressedHandling();
                        }
                    }
                }
            });
        }

        private void checkTxtFieldsFull()
        {
            if (jtxtUserName.getText().trim().isEmpty()) {
                jpassPassword.setText(null);
            }
            jpassPassword.setEnabled(!jtxtUserName.getText().trim().isEmpty());
            jbtnOK.setEnabled(jpassPassword.getPassword().length != 0);
        }

        private void btnOKpressedHandling()
        {
            try {
                connection = JDBCModel.establishConnection(
                    driver,
                    jtxtUserName.getText(), jpassPassword.getPassword());
                JOptionPane.showMessageDialog(
                    jFrame,
                    "Connection with selected DBMS established successfully!",
                    "Connection successful", JOptionPane.INFORMATION_MESSAGE);
                changeConnStatus();
                setPanelEnabled(jpanMain, true);
                dispose();
            } catch (SQLException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(
                    jFrame, e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JFrame getFrame()
    {
        JFrame jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(450, 400);
        jFrame.setLocationRelativeTo(null);
        jFrame.setTitle("Persons small DB");
        return jFrame;
    }
}
