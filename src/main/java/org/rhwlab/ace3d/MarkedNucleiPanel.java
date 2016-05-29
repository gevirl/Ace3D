/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class MarkedNucleiPanel extends JPanel implements InvalidationListener {
    public MarkedNucleiPanel(){
        this.setLayout(new BorderLayout());
        this.add(new JLabel("Marked Nuclei"),BorderLayout.NORTH);
        list = new JList();
        JScrollPane scroll = new JScrollPane(list);
        this.add(scroll,BorderLayout.CENTER);
        
        JButton clear = new JButton("Clear All Marked Nuclei");
        this.add(clear,BorderLayout.SOUTH);
        clear.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo == null) return;
                for (int t=embryo.getMinTime() ; t<=embryo.getMaxTime();++t){
                    Set<Nucleus> marked = embryo.getMarkedNuclei(t);
                    for (Nucleus nuc : marked){
                        embryo.setMarked(nuc, false);
                    }
                }
                DefaultListModel model = new DefaultListModel();
                list.setModel(model);
                MarkedNucleiPanel.this.repaint();                 
            }
        });
    }

    @Override
    public void invalidated(Observable observable) {
        if (observable instanceof ImagedEmbryo){
            embryo = (ImagedEmbryo)observable;
            Nucleus sel = embryo.selectedNucleus();
            int firstTime = embryo.getMinTime();
            if (sel != null){
                firstTime = sel.getTime();
            }
            DefaultListModel model = new DefaultListModel();
            for (int t=firstTime ; t<=embryo.getMaxTime();++t){
                Set<Nucleus> marked = embryo.getMarkedNuclei(t);
                for (Nucleus nuc : marked){
                    model.addElement(nuc.getName());
                }
            }
            list.setModel(model);
            this.repaint();            
            
        }
    }
    JList list;
    ImagedEmbryo embryo;
}
