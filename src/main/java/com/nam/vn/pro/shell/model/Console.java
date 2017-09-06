/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nam.vn.pro.shell.model;

import javax.swing.JTextArea;

/**
 *
 * @author loda
 */
public class Console extends JTextArea{

    public Console() {
    }

    @Override
    public void append(String string) {
        super.append(string+"\n"); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
