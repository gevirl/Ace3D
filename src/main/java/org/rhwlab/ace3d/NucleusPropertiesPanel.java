/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.GridLayout;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class NucleusPropertiesPanel extends JPanel implements InvalidationListener  {
    public NucleusPropertiesPanel(){
        this.setLayout(new GridLayout(10,2));
        this.add(new JLabel("Selected Nucleus"));
        this.add(name);
        this.add(new JLabel("Parent Nucleus"));
        this.add(parent);
        this.add(new JLabel("Child1 Nucleus"));
        this.add(child1);
        this.add(new JLabel("Child2 Nucleus"));
        this.add(child2);        
        this.add(new JLabel("Center"));
        this.add(center);
        this.add(new JLabel("a Radius"));
        this.add(aRadius);
        this.add(new JLabel("b Radius"));
        this.add(bRadius);
        this.add(new JLabel("c Radius"));
        this.add(cRadius);
        this.add(new JLabel("In Cell"));
        this.add(cell);
        this.add(new JLabel("Expression"));
        this.add(express);
        
    }

    @Override
    public void invalidated(Observable observable) {
        if (observable instanceof ImagedEmbryo ){
            ImagedEmbryo embryo = (ImagedEmbryo)observable;
            Nucleus selected = embryo.selectedNucleus();
            if (selected == null) {
                return;
            }
            name.setText(selected.getName());
            express.setText(String.format("%.2f",selected.getExpression()));
            long[] c = selected.getCenter();
            center.setText(String.format("(%d,%d,%d)",c[0],c[1],c[2]));
            aRadius.setText(selected.getRadiusLabel(0));
            bRadius.setText(selected.getRadiusLabel(1));
            cRadius.setText(selected.getRadiusLabel(2));
            if (selected.getCell() != null){
                cell.setText(selected.getCell().getName());
                // is it the first nucleus in the cell
                Nucleus firstNuc = selected.getCell().firstNucleus();
                Nucleus lastNuc = selected.getCell().lastNucleus();
                Cell[] children = selected.getCell().getChildren();
                Cell parentCell = selected.getCell().getParent();
                
                if (!firstNuc.getName().equals(selected.getName())){
                    parent.setText(selected.getCell().getNucleus(selected.getTime()-1).getName());
                } else {
                    if (parentCell != null){
                        parent.setText(parentCell.lastNucleus().getName());
                    } else {
                        parent.setText("Not linked");
                    }
                }
                
                if (!lastNuc.getName().equals(selected.getName())){
                    child1.setText(selected.getCell().getNucleus(selected.getTime()+1).getName());
                    child2.setText("Not linked");

                } else {
                    if (children.length != 0){
                        child1.setText(children[0].firstNucleus().getName());
                        child2.setText(children[1].firstNucleus().getName());
                    } else {
                        child1.setText("Not linked");
                        child2.setText("Not linked");
                    }
                   
                }
            } else {
                cell.setText("No cell");
                parent.setText("Not linked");
                child1.setText("Not linked");
                child2.setText("Not linked");
            }
        }
    }
    public String getChild1(){
        return child1.getText();
    }
    public String getChild2(){
        return child2.getText();
    }
    static String initial = "None Selected";
    JLabel name = new JLabel(initial);
    JLabel center = new JLabel(initial);
    JLabel aRadius = new JLabel(initial);
    JLabel bRadius = new JLabel(initial);
    JLabel cRadius = new JLabel(initial); 
    JLabel cell = new JLabel(initial);
    JLabel parent = new JLabel(initial);
    JLabel child1 = new JLabel(initial);
    JLabel child2 = new JLabel(initial);
    JLabel express = new JLabel(initial);
}