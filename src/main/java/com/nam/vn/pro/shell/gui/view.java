/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nam.vn.pro.shell.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

/**
 *
 * @author loda
 */
public class view extends javax.swing.JFrame {

    private String columnName[] = {"name", "isDirectory"};
    private String root = "/";
    private String dic = "/";
    private List<FileStatus> data;
    private Map<String, String> servers;
    private FileSystem fs;
    private int rowRightClickSelect = 0;
    private JavaSparkContext sc;
    /**
     * Creates new form view
     */
    public view() throws FileNotFoundException, IOException {
        initComponents();
        initSize();
        initTable();
        serverConfig();
        fileSystemConfig("localhost");
        System.out.println(command("ls", root));
        initData();
        initContext();
        this.pack();

    }
    /***
     * NEW
     */
    private void initContext(){
        SparkConf conf = new SparkConf();
        conf.setAppName("Shell");
        conf.setMaster("local[*]");
        sc = new JavaSparkContext(conf);
    }

    private void initSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double swidth = screenSize.getWidth();
        double sheight = screenSize.getHeight();
        int width = (int) (swidth / 2);
        int height = (int) sheight;
        this.setSize(width, height - 100);
    }

    private void initTable() {
        table.setModel(new DefaultTableModel(null, columnName) {

            public boolean isCellEditable(int row, int column) {
                return false;//This causes all cells to be not editable
            }

        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                try {
                    // do some actions here, for example
                    // print first column value from selected row
                    if (event.getValueIsAdjusting() || table.getSelectedRow() == -1) {
                        return;
                    }

                    System.out.println(table.getSelectedRow());
                    changeFolder(table.getSelectedRow());
                } catch (IOException ex) {
                    Logger.getLogger(view.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (rowRightClickSelect > 0) {
                    System.out.println("DELETE " + rowRightClickSelect);
                }

            }
        });
        popupMenu.add(deleteItem);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        rowRightClickSelect = table.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table));
                        if (rowRightClickSelect > -1) {
//                            table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                            System.out.println(rowRightClickSelect);
                        }
                    }
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }
        });
        table.setComponentPopupMenu(popupMenu);
    }

    private void initData() throws IOException {
        if (fs == null) {
            fileSystemConfig("localhost");
        }
        loadData();
    }

    private void loadData() throws IOException {
        try{
        FileStatus[] files = fs.listStatus(new Path(dic));
        data = Arrays.asList(files);
        String vec[][] = new String[files.length + 1][2];
        vec[0][0] = "..";
        for (int i = 0; i < files.length; i++) {
            vec[i + 1][0] = files[i].getPath().getName();
            vec[i + 1][1] = String.valueOf(files[i].isDirectory());
        }
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setDataVector(vec, columnName);
        model.fireTableDataChanged();
        }catch(Exception e){
            dic = dic.substring(0,dic.lastIndexOf("/"));
            if(dic.equals("")) dic = "/";
            e.printStackTrace();
        }
    }
    
       /***
     * NEW
     */
    private void loadFile(String path){
        List<String> list = sc.textFile(path)
                .take(10);
        updateConsole(list);
        updateConsole("gaga","fafe");
    }
    private void updateConsole(List<String> arg){
        for(String s: arg){
            console.append(s+"\n");
        }
    }
    private void updateConsole(String... arg){
        for(String s: arg){
            console.append(s+"\n");
        }
    }
    
    
    
    private void changeFolder(int index) throws IOException {
        if (index != 0) {
            FileStatus file = data.get(index - 1);
            if (file.isDirectory()) {
                if (!dic.equals(root)) {
                    dic += "/" + file.getPath().getName();
                }else{
                    dic += file.getPath().getName();
                }
                loadData();
            } else {
                txt_input.setText(file.getPath().toString());
                loadFile(file.getPath().toString());
            }
        } else {
            if (!dic.equals(root)) {
                dic = dic.substring(0, dic.lastIndexOf("/"));
                if(dic.equals("")){
                    dic = "/";
                }
                loadData();
            }
        }
    }

    private void serverConfig() throws FileNotFoundException {
        Scanner sc = new Scanner(new File("server.con"));
        if (servers == null) {
            servers = new HashMap<String, String>();
        }
        while (sc.hasNextLine()) {
            String tokens[] = sc.nextLine().split(":");
            servers.put(tokens[0].trim(), tokens[1].trim());
        }
    }

    private void fileSystemConfig(String serverName) throws IOException {
        Configuration conf = new Configuration();
        if (!serverName.equals("localhost")) {
            conf.set("fs.defaultFS", servers.get(serverName));
        }
        fs = FileSystem.newInstance(conf);
        System.out.println("Root: " + root);
    }

    public static List<String> command(final String cmdline,
            final String directory) {
        try {

            Process process
                    = new ProcessBuilder(new String[]{"bash", "-c", cmdline})
                            .redirectErrorStream(true)
                            .directory(new File(directory))
                            .start();

            List<String> output = new ArrayList<String>();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }

            //There should really be a timeout here.
            if (0 != process.waitFor()) {
                return null;
            }

            return output;

        } catch (Exception e) {
            //Warning: doing this is no good in high quality applications.
            //Instead, present appropriate error messages to the user.
            //But it's perfectly fine for prototyping.

            return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txt_input = new javax.swing.JTextField();
        console = new java.awt.TextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        txt_folder = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        console.setBackground(new java.awt.Color(0, 43, 54));
        console.setForeground(new java.awt.Color(131, 148, 150));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(table);

        jButton1.setText("Go");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("jButton2");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(console, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 686, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txt_folder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1))
            .addGroup(layout.createSequentialGroup()
                .addComponent(txt_input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_folder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(console, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            String folder = txt_folder.getText();
            if (folder != null && !folder.equals("")) {
                Path path = new Path(folder);
                dic = path.toString();
                if (dic.charAt(0) != '/') {
                    dic = "/" + dic;
                }
                if(dic.equals("")){
                    dic = "/";
                }
                txt_folder.setText(dic);
            }

            loadData();
            // TODO add your handling code here:
        } catch (IOException ex) {
            Logger.getLogger(view.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new view().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(view.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.TextArea console;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    private javax.swing.JTextField txt_folder;
    private javax.swing.JTextField txt_input;
    // End of variables declaration//GEN-END:variables
}
