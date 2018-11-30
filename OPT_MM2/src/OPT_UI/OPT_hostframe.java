/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OPT_UI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import org.micromanager.Studio;

import javax.swing.JOptionPane;
import mmcorej.TaggedImage;

import org.micromanager.data.Coords;
import org.micromanager.data.Image;
import org.micromanager.data.Datastore;
import org.micromanager.data.Metadata;
import org.micromanager.data.ImageJConverter;
import org.micromanager.data.RewritableDatastore;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.DisplayWindow;
//import org.micromanager.display.DisplaySettings.DisplaySettingsBuilder;
import org.micromanager.internal.utils.ImageUtils;

/**
 *
 * @author Fogim
 */
public class OPT_hostframe extends javax.swing.JFrame {
    static OPT_hostframe frame_;  
    public static Studio gui_ = null;
    private CMMCore core_ = null;   
    Gson gson = new GsonBuilder().create();
    FileWriter fw;
    boolean aborted_ = false;
    boolean calib_running = false;
    DisplayWindow calib_display;
    ImagePlus calib_implus;
    ImageProcessor improc;
    ColorProcessor calib_RGBproc;
    ShortProcessor calib_shortproc;
    ByteProcessor calib_byteproc;
    String rotstagename = "RotStage";
    
    public Thread calibThread;
    
    /**
     * Creates new form OPT_hostframe
     */
    public OPT_hostframe(Studio gui_ref) {
        frame_ = this;            
//        frame_.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("HCAicon.png")));
//        frame_.setTitle("OpenHCA controller for Micro-manager 2");        
//        frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        frame_.addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent we) {
//                if (true == confirmQuit()){
//                    dispose();
//                } else {
//                    frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//                }
//            }
//        });          
        gui_ = gui_ref;
        core_ = gui_.getCMMCore();
        initComponents();
        //Pass reference to this frame to children to allow callbacks
        rotation_control1.initialise(frame_);
        save_details1.initialise(frame_);
        abort1.initialise(frame_);
        
        try {
            core_.setExposure(100.0);
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
        gui_.live().setLiveMode(true);
        
        //calib_implus = new ImageProcessor();
    }
    
    public void abort(){
        calib_running = false;
        progress_indicator1.set_working(false);
        //Set to 100%, or leave at progress when aborted?
        //progress_indicator1.set_progress(100);
        progress_indicator1.set_aborted(true);
        aborted_ = true;
        //Maybe go live?
        gui_.live().setLiveMode(true);
        calib_display.requestToClose();
        //###Reenable acq and calib buttons here
    }
    
    public void reset_abort(){
        set_working(false);
        progress_indicator1.set_aborted(false);
        aborted_ = false;
    }
    
    public void set_working(boolean working){
        progress_indicator1.set_progress(0);
        progress_indicator1.set_working(working);
    }

    public void run_acquisition_threaded() throws Exception{
        Thread acqThread = new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       run_acquisition();
                   } catch (Exception ex) {
                       Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
                   }
               }
            });
        acqThread.start();     
    }
    
    public void run_acquisition() throws Exception{
        //Make sure we can press the abort button...
        reset_abort();
        //###NEED TO FIX THE FORCED-PATH CASE
        String basedir = save_details1.get_savepath();
        String samplename = save_details1.get_samplename();
        String savedetails = save_details1.get_savechoice();
        System.out.println(savedetails);
        String filterset = "XFP";
        String fullpath = "";
        if (savedetails.equalsIgnoreCase("Auto-path")){
            fullpath = basedir+"\\"+samplename+"\\"+filterset;
        } else {
            System.out.println("Forcepath");
        }
        String append = "\\X";
        File saveloc = new File(fullpath);
//        while(saveloc.exists()){
//            System.out.println(fullpath);
//            if (savedetails.equalsIgnoreCase("Auto-path")){
//                fullpath = basedir+"\\"+samplename+"\\"+filterset+append;
//            } else {
//                fullpath = basedir+append;
//            }
//            append = append+"X";
//            saveloc = new File(fullpath);
//            saveloc.mkdirs();
//        }
        if(saveloc.exists()){
            String titleBar = "ABORTING!";
            String infoMessage = "FILE EXISTS!";
            JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
            abort();
        }
        int numproj = rotation_control1.get_numproj();
        //Kill live mode if running
        gui_.live().setLiveMode(false);
        //Setup a datastore
        Datastore store = gui_.data().createSinglePlaneTIFFSeriesDatastore(fullpath);
        DisplayWindow OPT_display = gui_.displays().createDisplay(store);
        gui_.displays().manage(store);
        //Setup co-ordinates
        Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
        builder.z(numproj);
        Coords coords = builder.build();      

        Image curr_img;
        int stepsize = (rotation_control1.zdist_per_revolution/rotation_control1.get_numproj());       
        for(int pos = 0; pos<numproj; pos++){
            if(aborted_){
                break;
            } else {
                coords = coords.copy().z(pos).build();
                double oldpos = core_.getPosition(rotstagename);
                core_.snapImage();
                core_.setPosition(rotstagename,oldpos+stepsize);
                //convertTaggedImage takes (IMG/COORDS/METADATA)
                curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
                Image newimg = curr_img.copyAtCoords(coords);
                store.putImage(curr_img);
                progress_indicator1.set_progress((int) (100*(((double)pos+1.0)/(double)numproj)));
                core_.waitForDevice(rotstagename);
            }
        }
        String titleBar = "Threaded popup";
        String infoMessage = "Runs after the wimdow should appear...";
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
        gui_.live().setLiveMode(true);
    }
            
    public void run_calibration_threaded() {                                                    
    // starts sequence in new thread
        calibThread = new Thread(new Calibrationthread(this));
        calibThread.start();
    }      
    
    public void run_test(){
        gui_.live().setLiveMode(false);
        int imwidth = (int)core_.getImageWidth();
        int imheight = (int)core_.getImageHeight();
        set_working(true);        
        try {
            core_.snapImage();
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Exception caught on snap", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
        Image curr_img = null;
        Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
        builder.channel(2);
        Coords coords = builder.build();      
        try {
            curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
            JOptionPane.showMessageDialog(null, "Img size: "+imwidth+","+imheight, "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(OPT_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Exception caught on show", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
//        try {
            calib_shortproc = new ShortProcessor(imwidth,imheight);
//        } catch (Exception ex) {
//            JOptionPane.showMessageDialog(null, "Exception caught on create processor", "Oops!", JOptionPane.INFORMATION_MESSAGE);
//        }
//        try {
            calib_shortproc.setPixels(curr_img.getRawPixels());
//        } catch (Exception ex) {
//            JOptionPane.showMessageDialog(null, "Exception caught on set pixels", "Oops!", JOptionPane.INFORMATION_MESSAGE);
//        }        
        try {
            //calib_implus.
            //calib_implus.setProcessor("LALALA",calib_shortproc);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Exception caught on set processor", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }
        try {
            calib_implus.updateAndDraw();        
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Exception caught on draw", "Oops!", JOptionPane.INFORMATION_MESSAGE);
        }        
        abort();
    }
    
    public void run_calibration() throws Exception{
        if(!calib_running){
            gui_.live().setLiveMode(false);
            set_working(true);
            RewritableDatastore store = gui_.data().createRewritableRAMDatastore();        
            calib_display = gui_.displays().createDisplay(store);
            Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
            builder.channel(2);
            Coords coords = builder.build();      
            int l_r = 0;
            Image curr_img;
            
            DisplaySettings cds = calib_display.getDisplaySettings();
            DisplaySettings.DisplaySettingsBuilder dsb = cds.copy();
            dsb.channelColorMode(DisplaySettings.ColorMode.COMPOSITE);
            Color[] chan_cols = new Color[] {Color.RED,Color.GREEN};
            dsb.channelColors(chan_cols);
            calib_display.setDisplaySettings(dsb.build());
            
            core_.snapImage();
            TaggedImage tmp = core_.getTaggedImage();
            Image newimg = gui_.data().convertTaggedImage(tmp);
            int img_width = newimg.getWidth();
            int img_height = newimg.getHeight();
            int b_p_p = newimg.getBytesPerPixel();
            ImagePlus ip1 = new ImagePlus();
            ImageStack is1 = new ImageStack();
            ip1.createImagePlus();

            while(aborted_ == false){
                int chan_num = l_r%2;
                coords = coords.copy().channel(chan_num).build();
                double oldpos = core_.getPosition();
                core_.waitForDevice(rotstagename);
                core_.snapImage();
                core_.setPosition(oldpos+(rotation_control1.zdist_per_revolution/2));
                tmp = core_.getTaggedImage();
                if(chan_num==0){
                    Image image1 = gui_.data().convertTaggedImage(tmp);
                    image1 = image1.copyAtCoords(coords);
                    store.putImage(image1);
                } else {
                    Image image2 = gui_.data().convertTaggedImage(tmp);
                    image2 = image2.copyAtCoords(coords);                    
                    store.putImage(image2);
                }
                l_r += 1;                
            }            
            
//            core_.snapImage();
//            TaggedImage tmp = core_.getTaggedImage();
//            Image newimg = gui_.data().convertTaggedImage(tmp);
//            ImageProcessor proc1 = new ShortProcessor(newimg.getWidth(), newimg.getHeight());
//            ImageProcessor proc2 = new ShortProcessor(newimg.getWidth(), newimg.getHeight());
//            while(aborted_ == false){
//                int chan_num = l_r%2;
//                coords = coords.copy().channel(chan_num).build();
//                double oldpos = core_.getPosition();
//                core_.waitForDevice(rotstagename);
//                core_.snapImage();
//                core_.setPosition(oldpos+(rotation_control1.zdist_per_revolution/2));
//                tmp = core_.getTaggedImage();
//                if(l_r%2==0){
//                    Image image1 = gui_.data().convertTaggedImage(tmp);
//                    image1 = image1.copyAtCoords(image1.getCoords().copy().channel(0).build());
//                    store.putImage(image1);
//                } else {
//                    Image image2 = gui_.data().convertTaggedImage(tmp);
//                    proc2.setPixels(image2.getRawPixelsCopy());
//                    proc2.flipVertical();
//                    newimg = gui_.data().createImage(proc2.getPixelsCopy(), image2.getWidth(), image2.getHeight(), image2.getBytesPerPixel(), image2.getNumComponents(), coords, image2.getMetadata());
//                    image2 = image2.copyAtCoords(image2.getCoords().copy().channel(1).build());                    
//                    store.putImage(image2);
//                }
//                l_r += 1;                
//            }
            store.deleteAllImages();
            calib_display.forceClosed();
            reset_abort();
        } else {
            JOptionPane.showMessageDialog(null, "System already in calibration mode!", "Hang on!", JOptionPane.INFORMATION_MESSAGE);
            abort();
        }
    }
    
    public void run_calibration_old() throws Exception{
        if(!calib_running){
            boolean cip = false;
            calib_running = true;
            //Kill live mode if running
            gui_.live().setLiveMode(false);
            set_working(true);
            RewritableDatastore store = gui_.data().createRewritableRAMDatastore();
            calib_display = gui_.displays().createDisplay(store);
            Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
            builder.channel(2);
            Coords coords = builder.build();      
            int l_r = 0;
            Image curr_img;
            System.out.println("init_calib");
            DisplaySettings cds = calib_display.getDisplaySettings();
            DisplaySettings.DisplaySettingsBuilder dsb = cds.copy();
            dsb.channelColorMode(DisplaySettings.ColorMode.COLOR);
            Color[] chan_cols = new Color[] {Color.RED,Color.GREEN};
            dsb.channelColors(chan_cols);
            calib_display.setDisplaySettings(dsb.build());
            //Get img for ref size etc
            core_.snapImage();
            curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
            int width = curr_img.getWidth();
            int height = curr_img.getHeight();
            int ijType = curr_img.getImageJPixelType();            
            ImageProcessor proc_L = ImageUtils.makeProcessor(ijType, width, height, curr_img.getRawPixelsCopy());            
            ImageProcessor proc_R = ImageUtils.makeProcessor(ijType, width, height, curr_img.getRawPixelsCopy());            
            while(aborted_ == false){
                coords = coords.copy().channel(l_r%2).build();
                double oldpos = core_.getPosition();
                core_.setPosition(oldpos+(rotation_control1.zdist_per_revolution/2));
                core_.waitForDevice(rotstagename);
                core_.snapImage();
                //convertTaggedImage takes (IMG/COORDS/METADATA)
                curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
//                if(l_r%2==0){
//                    proc_R = ImageUtils.makeProcessor(ijType, width, height, curr_img.getRawPixelsCopy());            
//                    if(rotation_control1.is_axis_horizontal()){
//                        proc_R.flipVertical();
//                    } else {
//                        proc_R.flipHorizontal();
//                    }
//                    Metadata MD = curr_img.getMetadata();
//                    curr_img = gui_.data().getImageJConverter().createImage(proc_R, coords, MD);
//                    Image ci_copy = curr_img.copyAtCoords(coords);
//                    store.putImage(ci_copy);
//                } else {
                    store.putImage(curr_img);
//                }
                calib_implus.getProcessor().setPixels(curr_img);
                if(cip == false){
                    calib_implus.show();
                    cip = true;
                }
                l_r += 1;
            }
            store.deleteAllImages();
            calib_display.forceClosed();
            reset_abort();
        } else {
             abort();
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

        rotation_control1 = new OPT_UI.Rotation_control();
        save_details1 = new OPT_UI.Save_details();
        abort1 = new OPT_UI.Abort();
        progress_indicator1 = new OPT_UI.progress_indicator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(save_details1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rotation_control1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progress_indicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(abort1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rotation_control1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(abort1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(progress_indicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(save_details1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OPT_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OPT_hostframe(gui_).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private OPT_UI.Abort abort1;
    private OPT_UI.progress_indicator progress_indicator1;
    private OPT_UI.Rotation_control rotation_control1;
    private OPT_UI.Save_details save_details1;
    // End of variables declaration//GEN-END:variables
}
