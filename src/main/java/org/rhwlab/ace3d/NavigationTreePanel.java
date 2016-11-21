/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.process.LUT;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.TreeMap;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.CellImage;
import org.rhwlab.dispim.nucleus.CellImage.CellLocation;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class NavigationTreePanel extends JPanel implements ChangeListener{
    public NavigationTreePanel(ImagedEmbryo emb){
        lut = LUT.createLutFromColor(Color.GREEN);
        embryo = emb;
        lut.min = 0;
        lut.max = 255;
        
        this.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                int x = e.getX();
                int y = e.getY();
                int offset = 0;
                int index = -1;
                for (int i=0 ; i<cellImage.length ; ++i){
                    if (x < offset + buffered[i].getWidth()) {
                        index = i;
                        break;
                    }
                    offset = offset + buffered[i].getWidth();
                }
                
                CellLocation cellLoc = cellImage[index].cellAtLocation(x-offset, y);
                if (cellLoc != null){
                    Nucleus firstNuc = cellLoc.firstNuc;
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        TreeMap<Integer,Nucleus> desc = new TreeMap<>();
                        firstNuc.descedentsInCell(desc);
                        int t0 = desc.firstKey();
                        int t1 = desc.lastKey();
                        double f = (y-cellLoc.y0)/(cellLoc.y1-cellLoc.y0);
                        
                        int t = t0 + (int)(f*(t1-t0));
                        Nucleus nuc = desc.get(t);
                        embryo.setSelectedNucleus(nuc);
                    } else if (e.getButton()==MouseEvent.BUTTON3){
                        Nucleus nuc = firstNuc.lastNucleusOfCell();
                        embryo.setSelectedNucleus(nuc);                        
                    }
                }
                
            }
        });
 /*       
        this.addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseMoved(MouseEvent e){
                int x = e.getX();
                int y = e.getY();
                if (cellImage != null){
                    CellLocation cellLoc = cellImage.cellAtLocation(x, y);
                    if (cellLoc != null){
                        Cell cell = embryo.getNucleusFile().getCell(cellLoc.name);
                        if (cell != null){
                            double f = (y-cellLoc.y0)/(cellLoc.y1-cellLoc.y0);
                            int t = cell.firstTime() + (int)(f*(cell.lastTime()-cell.firstTime()));
                            Nucleus nuc = cell.getNucleus(t);
                            nucName = nuc.getName();
                            System.out.printf("%s\n",nuc.getName());
                        }
                    }  
                }
            }
        });
*/
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof JTree){
            JTree jTree = (JTree)e.getSource();
            treePaths = jTree.getSelectionPaths();
            if (treePaths == null) {
                return;
            }            
        } 
        if (treePaths  == null) {
            return;
        }

        cellImage = new CellImage[treePaths.length];
        buffered = new BufferedImage[treePaths.length];
        for (int i=0 ; i<treePaths.length ; ++i){
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePaths[i].getPathComponent(1);
            Nucleus nuc = (Nucleus)node.getUserObject();
            cellImage[i] = new CellImage();
            buffered[i] = cellImage[i].getImage(nuc,headPanel.getMaxTime(),lut,headPanel.labelNodes(),headPanel.labelLeaves(),
                    headPanel.getTimeScale(),headPanel.getCellWidth());
        }
 /*       
        NavigationHeaderPanel headPanel = (NavigationHeaderPanel)e.getSource();
        rootNuc = headPanel.getRoot();
        if (rootNuc == null)return;
        cellImage = new CellImage();
        buffered = cellImage.getImage(rootNuc,headPanel.getMaxTime(),lut,headPanel.labelNodes(),headPanel.labelLeaves(),
                headPanel.getTimeScale(),headPanel.getCellWidth());
        int h = buffered.getHeight();
        int w = buffered.getWidth(); 

        this.setSize(w,h);
        this.setPreferredSize(new Dimension(w,h));
*/
        this.invalidate();
        this.repaint();
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (embryo == null){
            return;
        }
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null){
            return;
        }
        if (buffered == null){
            return;
        }
        Graphics2D g2 = (Graphics2D) g;

        Dimension d = this.getSize();
//        double scale = Math.min((double)d.width/(double)w, (double)d.height/(double)h);
  //      System.out.printf("Scale=%f,d.w=%d,d.h=%d,w=%d.h=%d)",scale,d.width,d.height,w,h);
        // clear the panel
        Color save = g2.getColor();
        g2.setColor(Color.white);
 
        g2.fillRect(0,0,d.width,d.height);
        g2.setColor(save);
        
//        AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
        AffineTransform xForm = new AffineTransform();
        int xPos =0;
        for (BufferedImage bufImage : buffered){
            g2.drawImage(bufImage,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),xPos,0);    
            xPos = xPos + bufImage.getWidth();
        }
    }
    public void setHeadPanel(NavigationHeaderPanel headPanel){
        this.headPanel=headPanel;
    }
    TreePath[] treePaths;
    NavigationHeaderPanel headPanel;
    LUT lut;
    ImagedEmbryo embryo;
    BufferedImage[] buffered;
    CellImage[] cellImage;
    String nucName;
}
