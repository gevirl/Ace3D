/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.util.Set;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Navigation_Frame extends JFrame implements PlugIn,InvalidationListener {
    public Navigation_Frame(ImagedEmbryo emb,SynchronizedMultipleSlicePanel p){
        super();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setTitle("Navigation Trees");
        this.embryo = emb;
        this.panel = p;
        this.getContentPane().setLayout(new BorderLayout());
        
        rootsRoot = new DefaultMutableTreeNode("Roots at Each Time",true);
        rootsTree = new JTree(rootsRoot);
        rootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        rootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootsTree.getLastSelectedPathComponent();
                if (node == null){
                    return;
                }
                String cellName = (String)node.getUserObject();
                NucleusFile nucFile = embryo.getNucleusFile();
                Cell cell = nucFile.getCell(cellName);                
                if (cell != null){
                    int time = cell.firstTime();
                    Nucleus nuc = cell.getNucleus(time);
                    emb.setSelectedNucleus(nuc);
                    panel.changeTime(time);
                    panel.changePosition(nuc.getCenter());
                }
            }
        });
        JScrollPane rootsScroll = new JScrollPane(rootsTree);
        this.getContentPane().add(rootsScroll,BorderLayout.WEST);
 
        nucsRoot = new DefaultMutableTreeNode("All Nuclei at Each Time",true);
        nucsTree = new JTree(nucsRoot);
        nucsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        nucsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)nucsTree.getLastSelectedPathComponent();
                if (node.isLeaf()){
                    String nuc = (String)node.getUserObject();
                    int time = Integer.valueOf((String)((DefaultMutableTreeNode)node.getParent()).getUserObject());
                    NucleusFile nucFile = embryo.getNucleusFile();
                    embryo.setSelectedNucleus(nucFile.getNucleus(nuc));
                    panel.changeTime(time);
                    panel.changePosition(nucFile.getNucleus(nuc).getCenter());
                }
            }
        });
        JScrollPane nucsScroll = new JScrollPane(nucsTree);
        this.getContentPane().add(nucsScroll,BorderLayout.EAST);  
        
        NavigationTreePanel treePanel = new NavigationTreePanel(embryo);
        this.add(treePanel,BorderLayout.CENTER);
        
        NavigationHeaderPanel headPanel = new NavigationHeaderPanel(treePanel);
        this.add(headPanel,BorderLayout.NORTH);
        pack();
        
    }

    @Override
    public void run(String arg) {
        this.setSize(800,400);
        this.setVisible(true);
    }


    @Override
    public void invalidated(Observable observable) {
        nucsRoot.removeAllChildren();
        rootsRoot.removeAllChildren();
        NucleusFile nucFile = (NucleusFile)observable;
        Set<Integer> times = nucFile.getAllTimes();
        for (Integer time : times){
            Set<Cell> roots = nucFile.getRoots(time);
            if (roots!=null){
                DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(Integer.toString(time));
                rootsRoot.add(timeNode);
                for (Cell cell : roots){
                    addCellToNode(cell,timeNode);
    
                }
            }
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(Integer.toString(time));
            nucsRoot.add(timeNode);
            for (Nucleus nuc : nucs){
                DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc.getName());
                timeNode.add(nucNode);                
            }
        }
        
    }
    private void addCellToNode(Cell cell,DefaultMutableTreeNode node){
        DefaultMutableTreeNode cellNode = new DefaultMutableTreeNode(cell.getName());
        node.add(cellNode);
        for (Cell child : cell.getChildren()){
            addCellToNode(child,cellNode);
        }
        
    }
    ImagedEmbryo embryo;
    SynchronizedMultipleSlicePanel panel;
    DefaultMutableTreeNode rootsRoot;
    DefaultMutableTreeNode nucsRoot;
    JTree rootsTree;
    JTree nucsTree;
}
