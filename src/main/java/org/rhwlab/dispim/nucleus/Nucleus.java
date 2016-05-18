/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.rhwlab.ace3d.SingleSlicePanel;
import org.rhwlab.starrynite.TimePointNucleus;

/**
 *
 * @author gevirl
 */
public class Nucleus implements Comparable {

    public Nucleus(JsonObject jsonObj){
        this.time = jsonObj.getInt("Time");
        this.name = jsonObj.getString("Name");
        this.radius = jsonObj.getJsonNumber("Radius").doubleValue();
        this.xC = jsonObj.getJsonNumber("X").longValue();
        this.yC = jsonObj.getJsonNumber("Y").longValue();
        this.zC = jsonObj.getJsonNumber("Z").longValue();
    }
    public Nucleus(TimePointNucleus data){
        this.time = data.getTime();
        this.name = data.getName();
        this.xC = data.getX();
        this.yC = data.getY();
        this.zC = (long)data.getZ();
        this.radius = data.getRadius();
    }
    public Nucleus(String[] headings,String[] data){
        for (int i=0 ; i<headings.length ; ++i){
            if (headings[i].equalsIgnoreCase("Time")){
                time = Integer.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Name")){
                name = data[i];
            } else if (headings[i].equalsIgnoreCase("X")){
                xC = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Y")){
                yC = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Z")){
                zC = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Radius")){
                radius = Double.valueOf(data[i]);
            }
        }
    }
    public Nucleus (int time,long[] center,double radius){
        this(time,randomName(),center,radius);
    }
    public Nucleus (int time,String name,long[] center,double radius){
        this.time = time;
        this.name = name;
        this.xC = center[0];
        this.yC = center[1];
        this.zC = center[2];
        this.radius = radius;
    } 
    static public String randomName(){
        if (rnd == null){
            rnd = new Random();
        }
        return String.format("Nuc_%d",(new Date()).getTime());
    }
    static public void saveHeadings(PrintStream stream){
        stream.println("Time,Name,X,Y,Z,Radius,Child1,Child2");
    }
    public void saveNucleus(PrintStream stream){
//        stream.printf("%d,%s,%d,%d,%d,%f,%s,%s\n",time,getName(),x,y,z,radius,getChild1(),getChild2());
    }
    public int getTime(){
        return this.time;
    }
    public double getRadius(){
        return radius;
    }
    public void setRadius(double r){
        this.radius = r;
    }
    public long[] getCenter(){
        long[] center = new long[3];
        center[0] = xC;
        center[1] = yC;
        center[2] = zC;
        return center;
    }
    public void setCenter(long[] c){
        xC = c[0];
        yC = c[1];
        zC = c[2];
    }
    public String getName(){
        if (name == null){
            return this.toString();
        }
        return name;
    }
/*
    public void setSelected(boolean s){
        this.selected = s;
    }
    public boolean getSelected(){
        return this.selected;
    }
*/
    public double distanceSqaured(long[] p){
        double d = 0.0;
        long[] c = this.getCenter();
        for (int i=0 ; i<p.length ; ++i){
            long delta = p[i]-c[i];
            d = d + delta*delta;
        }
        return d;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        builder.add("Time", time);
        builder.add("X", xC);
        builder.add("Y", yC);
        builder.add("Z", zC);
        builder.add("Radius", radius);
        if (cell != null){
            builder.add("Cell", cell.getName());
        }
        return builder;
    }
    public void setCell(Cell cell){
        this.cell = cell;
    }
    public Cell getCell(){
        return this.cell;
    }
    @Override
    public int compareTo(Object o) {
        return this.name.compareTo(((Nucleus)o).name);
    }  
    public boolean getLabeled(){
        return this.labeled;
    }
    public void setLabeled(boolean lab){
        this.labeled = lab;
    }
        public int getExpression(){
        return 100;
    }
    public boolean isVisible(long slice,int dim){
        switch(dim){
            case 0:
                return Math.abs(slice-xC)<radius;
            case 1:
                return Math.abs(slice-yC)<radius;
        }
       return Math.abs(slice-zC)<radius;
    }
    
    public Shape getShape(long slice,int dim,int bufW,int bufH){
            long[] center = getCenter();  // image corrdinates
            double r = getRadius();   // image corrdinates
            double delta = Math.abs(slice-center[dim]);   // image corrdinates
            if (isVisible(slice, dim)){
                double rad = Math.sqrt(r*r-delta*delta);  //image coordinates
                int ix = imageXDirection(dim);
                int iy = imageYDirection(dim);
                long[] low = new long[center.length];
                long[] high = new long[center.length];
                low[dim] = slice;
                high[dim] = slice;
                low[ix] = center[ix] - (long)rad;
                low[iy] = center[iy] - (long)rad;
                high[ix] = center[ix] + (long)rad;
                high[iy] = center[iy] + (long)rad;  
                int scrX = SingleSlicePanel.screenX(low,dim,bufW);
                int scrY = SingleSlicePanel.screenY(low,dim,bufH);
                int scrHighX = SingleSlicePanel.screenX(high,dim,bufW);
                int scrHighY = SingleSlicePanel.screenY(high,dim,bufH);
                return new Ellipse2D.Double(scrX,scrY,scrHighX-scrX,scrHighY-scrY); 
            }
        return null;
    }
    public int imageXDirection(int dim){
        if (dim==0){
            return 1;
        }
        return 0;
    }    
    public int imageYDirection(int dim){
        if (dim==2){
            return 1;
        }
        return 2;
    } 
    public void setAdjustment(Object o){
        
    }
    public Object getAdjustment(){
        return null;
    }
    public String getRadiusLabel(int i){
        switch(i){
            case 0:
                return "X";
            case 1:
                return "Y";
            default:
                return "Z";
        }
    }
    int time;
    String name;
    long xC;
    long yC;
    long zC;
    double radius;
    Cell cell;  // the cell to which this nucleus belongs - can be null
    
    boolean selected = false;
    boolean labeled = false;
    static Random rnd;


}
