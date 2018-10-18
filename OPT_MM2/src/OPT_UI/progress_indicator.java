/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OPT_UI;

import java.awt.Color;

/**
 *
 * @author kumars2
 */
public class progress_indicator extends javax.swing.JPanel {

    /**
     * Creates new form progress_indicator
     */
    public progress_indicator() {
        initComponents();
    }
    
    public void set_progress(int progress){
        progressbar.setValue(progress);
    }
    
    public void set_working(boolean working){
        //stripy bar for indeterminate progress like in calibration
        progressbar.setIndeterminate(working);
        progressbar.setStringPainted(!working);
    }    
    
    public void set_aborted (boolean aborted){
        if (aborted){
            progressbar.setStringPainted(true);
            progressbar.setForeground(Color.red);
        } else {
            progressbar.setStringPainted(true);
            progressbar.setForeground(Color.blue);
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

        progressbar = new javax.swing.JProgressBar();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(progressbar, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(progressbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar progressbar;
    // End of variables declaration//GEN-END:variables
}