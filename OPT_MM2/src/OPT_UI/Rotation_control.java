/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OPT_UI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Fogim
 */
public class Rotation_control extends javax.swing.JPanel {

    /**
     * Creates new form Rotation_control
     */
    OPT_hostframe parent_;
    int usteps_per_revolution = 12800;
    int zdist_per_revolution = 2000;
    boolean axis_horizontal = true;
    
    public Rotation_control() {
        initComponents();
    }

    public void initialise(OPT_hostframe parent_ref){
        parent_ = parent_ref;
        calc_numproj_options();
        set_axis_horizontal(true);
        cam_orient.setSelected(true);
    }
    
    public int get_numproj(){
        return Integer.parseInt(num_proj.getSelectedItem().toString());
    }
    
    public boolean is_axis_horizontal(){
        if (axis_horizontal){
            return true;
        } else {
            return false;
        }
    }
    
    public void set_axis_horizontal(boolean is_horz){
        axis_horizontal = is_horz; 
    }    
    
    public void calc_numproj_options(){
        usteps_per_revolution = Integer.parseInt(usteps_per_rev.getText());
        //Maybe try to reset value to nearest old one when I have time
        //int old_val = Integer.parseInt(num_proj.getSelectedItem().toString());
        num_proj.removeAllItems();
        //No point in checking when the divisor is <2...
        for (int i=1;i<=(usteps_per_revolution/2)+1;i++){
            if (usteps_per_revolution%i==0){
                num_proj.addItem(Integer.toString(i));
            }
        }
        //Add the whole number, for completeness
        num_proj.addItem(Integer.toString(usteps_per_revolution));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        num_proj = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        usteps_per_rev = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        run_acq = new javax.swing.JButton();
        run_calib = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        cam_orient = new javax.swing.JCheckBox();

        num_proj.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        num_proj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                num_projActionPerformed(evt);
            }
        });

        jLabel1.setText("# projections");

        usteps_per_rev.setText("12800");
        usteps_per_rev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usteps_per_revActionPerformed(evt);
            }
        });

        jLabel2.setText("usteps/rev");

        run_acq.setText("Run acquisition");
        run_acq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_acqActionPerformed(evt);
            }
        });

        run_calib.setText("Calibration");
        run_calib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_calibActionPerformed(evt);
            }
        });

        jTextField1.setText("2000");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel3.setText("z-dist/rev");

        cam_orient.setText("Rotation axis looks horizontal?");
        cam_orient.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cam_orientActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(num_proj, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(usteps_per_rev, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(jLabel3)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(run_acq, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                            .addComponent(run_calib, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cam_orient)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(num_proj, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(run_acq))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cam_orient)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(usteps_per_rev, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(run_calib)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void usteps_per_revActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usteps_per_revActionPerformed
        usteps_per_revolution =  Integer.parseInt(usteps_per_rev.getText()); 
    }//GEN-LAST:event_usteps_per_revActionPerformed

    private void run_acqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_acqActionPerformed
        try {
            parent_.run_acquisition_threaded();
        } catch (Exception ex) {
            Logger.getLogger(Rotation_control.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_run_acqActionPerformed

    private void run_calibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_calibActionPerformed
        try {
            parent_.run_calibration_threaded();
        } catch (Exception ex) {
            Logger.getLogger(Rotation_control.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_run_calibActionPerformed

    private void num_projActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_num_projActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_num_projActionPerformed

    private void cam_orientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cam_orientActionPerformed
        set_axis_horizontal(cam_orient.isSelected());
    }//GEN-LAST:event_cam_orientActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        zdist_per_revolution = Integer.parseInt(jTextField1.getText());
    }//GEN-LAST:event_jTextField1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox cam_orient;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JComboBox num_proj;
    private javax.swing.JButton run_acq;
    private javax.swing.JButton run_calib;
    private javax.swing.JTextField usteps_per_rev;
    // End of variables declaration//GEN-END:variables
}
