/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.nucleus.NucleusFile;
import org.rhwlab.dispim.nucleus.TGMM_NucleusDirectory;
import org.rhwlab.dispim.nucleus.TGMM_NucleusFile;

/**
 *
 * @author gevirl
 */
public class BHCTreeCutDialog extends JDialog {
    public BHCTreeCutDialog(Ace3D_Frame owner,NucleusFile nucleusFile){
        this.nucleusFile = (TGMM_NucleusDirectory)nucleusFile;
        this.setTitle("Cut the BHC Tree");
        this.setSize(300, 500);
        this.getContentPane().setLayout(new BorderLayout());
        this.setLocationRelativeTo(owner);
        
        jList = new JList();
        jList.addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()){
                        countClusters();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(jList);
        this.getContentPane().add(scroll, BorderLayout.CENTER);
        
        JPanel button = new JPanel();
        JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                BHCTreeCutDialog.this.ok();
                BHCTreeCutDialog.this.setVisible(false);
            }
        });        
        button.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                BHCTreeCutDialog.this.setVisible(false);
            }
        });
        button.add(cancel);
        this.getContentPane().add(button,BorderLayout.SOUTH);
    }
    private void ok(){
        Object sel = jList.getSelectedValue();
        String xml = tree.getBaseName() + ".xml";
        if (sel != null){
            nucleusFile.removeNucleiAtTime(tree.getTime());
            double thresh = ((Posterior)sel).r;
            try {
                tree.saveCutAtThresholdAsXML(xml, thresh);
                TGMM_NucleusFile tgmm = new TGMM_NucleusFile();
                tgmm.open(tree.getTime(),new File(xml), nucleusFile);  
                this.nucleusFile.putFileForTime(tree.getTime(), tgmm);
            } catch (Exception exc){
                exc.printStackTrace();
            }
            nucleusFile.notifyListeners();
        }
    }
    public void countClusters(){
        Posterior selected = (Posterior)jList.getSelectedValue();
        if (selected == null){
            return;
        }
        if (selected.n == -1){
            Element ele = tree.formXML(selected.r);
            selected.n = ele.getChildren("GaussianMixtureModel").size();            
        }
        jList.repaint();
    }
    public void setBHCTree(BHCTree tree,double thresh){
        Posterior selected = null;
        this.tree = tree;
        DefaultListModel model = new DefaultListModel();
        Set<Double> posts = tree.allPosteriors();
        for (Double post : posts){
            Posterior posterior = new Posterior(post,-1);
            model.addElement(posterior);
            if (post == thresh){
                selected = posterior;
            }            
        }
        jList.setModel(model);
        jList.setSelectedValue(selected,true);
    }
    TGMM_NucleusDirectory nucleusFile;
    BHCTree tree;
    JList jList;
    class Posterior {
        public Posterior(double r,int n){
            this.r = r;
            this.n = n;
        }
        
        @Override
        public String toString(){
            if (n ==-1){
                return String.format("%f",r);
            }
            return String.format("(%d) %f",n,r);
        }
        
        double r;
        int n;
    }
}