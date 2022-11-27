package com.myPhotoApp.sedykh;

import java.awt.*;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

public class JViewer extends javax.swing.JFrame
{
    private JList<String> jList;
    private MyImage myImage;

    public JViewer()
    {
        initComponents();
        jList.setModel(new AbstractListModel<>()
        {
            final String[] photos = Photos.getPhotos();

            public int getSize()
            {
                return (photos != null) ? photos.length : 0;
            }

            public String getElementAt(int i)
            {
                return (photos != null) ? photos[i] : null;
            }
        });
    }

    private void initComponents()
    {
        JSplitPane jSplitPane = new JSplitPane();
        myImage = new MyImage();
        JScrollPane jScrollPane = new JScrollPane();
        jList = new JList<>();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        GroupLayout myImageLayout = new GroupLayout(myImage);
        myImage.setLayout(myImageLayout);
        myImageLayout.setHorizontalGroup(
            myImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 468, Short.MAX_VALUE)
        );
        myImageLayout.setVerticalGroup(
            myImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 392, Short.MAX_VALUE)
        );

        jSplitPane.setRightComponent(myImage);

        jList.addListSelectionListener(this::jListValueChanged);
        jScrollPane.setViewportView(jList);

        jSplitPane.setLeftComponent(jScrollPane);

        getContentPane().add(jSplitPane, BorderLayout.CENTER);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(
            (screenSize.width - 518) / 2,
            (screenSize.height - 428) / 2, 518, 428);
    }

    private void jListValueChanged(ListSelectionEvent evt)
    {
        if (evt.getValueIsAdjusting()) {
            String element = jList.getSelectedValue();
            StringTokenizer st = new StringTokenizer(element);
            int id = Integer.parseInt(st.nextToken());

            myImage.setImage(Photos.getPhoto(id));
        }
    }

    public static void main(String[] args)
    {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info :
                javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager
                        .setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException |
                 UnsupportedLookAndFeelException ex)
        {
            java.util.logging.Logger.getLogger(JViewer.class.getName())
                .log(java.util.logging.Level.SEVERE, null, ex);
        }
         //Create and display the form
        java.awt.EventQueue.invokeLater(
            () -> new JViewer().setVisible(true));
    }
}
