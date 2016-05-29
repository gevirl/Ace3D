/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

/**
 *
 * @author gevirl
 */
public class Cell  implements Comparable {
    public Cell(String name){
        this.name = name;
    }

    public Cell(JsonObject jsonObj,Cell parent,Map<String,Nucleus> nucMap){
        this(jsonObj.getJsonString("Name").getString()); 
        this.parent = parent;
        
        JsonObject child = jsonObj.getJsonObject("Child0");
        if (child != null){
            children.add(new Cell(child,this,nucMap));
        }
        child = jsonObj.getJsonObject("Child1");
        if (child != null){
            children.add(new Cell(child,this,nucMap));
        }
        JsonArray jsonNuclei = jsonObj.getJsonArray("Nuclei");
        for (int i=0 ; i<jsonNuclei.size() ; ++i){
            String nucID = ((JsonString)jsonNuclei.get(i)).getString();
            Nucleus nuc = nucMap.get(nucID);
            if (nuc != null){
                nuclei.put(nuc.getTime(),nuc);
            }
        }
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        if (parent != null){
            builder.add("Parent", parent.name);
        }
   
        if (children != null){
            for (int i=0 ; i<children.size() ; ++i){
                builder.add(String.format("Child%d",i), children.get(i).asJson());
            }
        }
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Nucleus nuc : nuclei.values()){
            arrayBuilder.add(nuc.getName());
        }
        builder.add("Nuclei", arrayBuilder);
        return builder;
    }
    public String getName(){
        return this.name;
    }
    public Cell getParent(){
        return this.parent;
    }
    public Cell[] getChildren(){
        return children.toArray(new Cell[0]);
    }
    public void addNucleus(Nucleus nuc){
        nuclei.put(nuc.getTime(),nuc);
        nuc.setCell(this);
    }
    public void addChild(Cell child){
        children.add(child);
        child.setParent(this);
    }
    public void setParent(Cell parent){
        this.parent = parent;
    }
    public Nucleus getNucleus(int time){
        return nuclei.get(time);
    }
    // split this cell into two at the given time
    // return the later cell
    public Cell split(int time){
        Nucleus nuc = nuclei.get(time);
        
        // put all the distal nuclei into the new cell
        Cell ret = new Cell(nuc.getName());
        int t= time;
        while (nuc != null){
            ret.addNucleus(nuc);
            ++t;
            nuc = nuclei.get(t);
        }
        
        TreeMap<Integer,Nucleus> prox = new TreeMap<>();
        for (t=this.firstTime() ; t<time;++t){
            prox.put(t,nuclei.get(t));
        }
        nuclei = prox;
        
        // relink children cells to the distal cell
        for (Cell child : children){
            ret.addChild(child);
        }
        this.children.clear();
        
        return ret;
    }
    // the time of the last nucleus in the cell
    public int lastTime(){
        return nuclei.lastKey();
    }
    public void clearChildren(){
        children.clear();
    }
    public int firstTime(){
        return nuclei.firstKey();
    }
    public Nucleus firstNucleus(){
        return nuclei.firstEntry().getValue();
    }
    public Nucleus lastNucleus(){
        return nuclei.lastEntry().getValue();
    }
    // unlink this cell from its parent
    public void unlink(){
        if (parent != null){
            Cell[] parentsChildren = parent.getChildren();
            parent.clearChildren();
            for (Cell child : parentsChildren){
                if (!child.getName().equals(this.getName())){
                    parent.addChild(child);
                }
            }
        }
        this.parent = null;
    }
    public void combineWith(Cell other){
        for (Nucleus nuc : other.nuclei.values()){
            this.addNucleus(nuc);
        }
        for (Cell child : other.getChildren()){
            this.addChild(child);
        }
        other.clearChildren();
        
    }
    public Cell getSister(){
        Cell ret = null;
        if (parent != null){
            for (Cell c : parent.children){
                if (c.getName() != this.name){
                    ret = c;
                    break;
                }
            }
        }
        return ret;
    }
    // the maximum time this cell and its descents reach
    public int maxTime(){
        if (children.isEmpty()){
            return this.lastTime();
        } else {
            int ret = Integer.MIN_VALUE;
            for (Cell child : children){
                int t = child.maxTime();
                if (t >ret){
                    ret = t;
                }
            }
            return ret;
        }
    }
    // all the leaves of this cell
    public List<Cell> leaves(){
        ArrayList<Cell> ret = new ArrayList<>();
        if (!this.children.isEmpty()){
            for (Cell child : children){
                ret.addAll(child.leaves());
            }
        }
        ret.add(this);
        return ret;
    }

    // find the leaves of a cell up to a max time
    public List<Cell> leaves(int maxTime){
        ArrayList<Cell> ret = new ArrayList<>();
        if (this.lastTime() >= maxTime || this.isLeaf()){
            ret.add(this);
        }
        else if (!this.children.isEmpty()){
            for (Cell child : children){
                ret.addAll(child.leaves(maxTime));
            }
        }
        
        return ret;        
    }
    public int getMaxExpression(){
        int ret = Integer.MIN_VALUE;
        for (Cell child : children){
            int v = child.getMaxExpression();
            if (v > ret){
                ret = v;
            }
        }
        for (Nucleus nuc : nuclei.values()){
            int v = (int)nuc.getExpression();
            if (v > ret){
                ret = v;
            }
        }
        return ret;
    }
    public int getMinExpression(){
        int ret = Integer.MAX_VALUE;
        for (Cell child : children){
            int v = child.getMinExpression();
            if (v < ret){
                ret = v;
            }
        }
        for (Nucleus nuc : nuclei.values()){
            int v = (int)nuc.getExpression();
            if (v < ret){
                ret = v;
            }
        }
        return ret;        
    }
    @Override
    public int compareTo(Object o) {
        return name.compareTo(((Cell)o).name);
    }  
    public boolean isLeaf(){
        return children.isEmpty();
    }

    String name;
    Cell parent;  // the parent cell - can be null
    List<Cell> children = new ArrayList<>();  // children after division of this cell - can be empty
    TreeMap<Integer,Nucleus> nuclei =  new TreeMap<>();  // the time-linked nuclei in this cell


    
}
