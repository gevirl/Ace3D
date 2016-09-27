/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import org.rhwlab.dispim.datasource.Voxel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.datasource.VoxelDataSource;
import org.rhwlab.variationalbayesian.GaussianMixture;

/**
 *
 * @author gevirl
 */
public class ClusteredDataSource implements VoxelDataSource {
    public ClusteredDataSource(String file)throws Exception {
        this.openFromClusters(file);
    }
    public ClusteredDataSource(VoxelClusterer[] clusterers,int background,int D){
        this.D = D;
        this.background = background;
        
        // how many total clusters and total points?
        int K = 0;
        int N = 0;
        for (VoxelClusterer clusterer : clusterers){
            List<CentroidCluster<Voxel>> clusterList = clusterer.result;
            K = K + clusterList.size();
            for (CentroidCluster cluster : clusterList ){
                N = N + cluster.getPoints().size();
            }
        }
  
        clusterMin = new int[K];
        clusterMax = new int[K];
        centers = new RealVector[K];
        X = new Voxel[N];
        z = new GaussianComponent[N];
        int n = 0;
        int k = 0;
        minIntensity = Integer.MAX_VALUE;
        maxIntensity = Integer.MIN_VALUE;
        for (VoxelClusterer clusterer : clusterers){
            List<CentroidCluster<Voxel>> clusterList = clusterer.result;
            for (CentroidCluster<Voxel> cluster : clusterList ){
                GaussianComponent comp = new GaussianComponent(this,k);
                gaussians.add(comp);            
                centers[k] = new ArrayRealVector(cluster.getCenter().getPoint());
                this.clusterMin[k] = Integer.MAX_VALUE;
                this.clusterMax[k] = Integer.MIN_VALUE;
                for (Voxel vox : cluster.getPoints()){
                    X[n] = vox;
                    if (vox.intensity < minIntensity){
                        minIntensity = vox.intensity;
                    }
                    if (vox.intensity > maxIntensity){
                        maxIntensity = vox.intensity;
                    }
                    if (vox.intensity < clusterMin[k]){
                        clusterMin[k] = vox.intensity;
                    }
                    if (vox.intensity > clusterMax[k]){
                        clusterMax[k] = vox.intensity;
                    }
                    comp.addPoint(n, false);
                    z[n] = comp;
                    ++n;
                }
                ++k;
                comp.calculateStatistics();
            } 
        }
    }

    final public void openFromClusters(String xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();
        int K = Integer.valueOf(root.getAttributeValue("NumberOfClusters"));
        D = Integer.valueOf(root.getAttributeValue("Dimensions"));
        clusterMin = new int[K];
        clusterMax = new int[K];
        centers = new RealVector[K];
        int N = Integer.valueOf(root.getAttributeValue("NumberOfPoints"));
        minIntensity = Integer.valueOf(root.getAttributeValue("MinimumIntensity"));
        maxIntensity = Integer.valueOf(root.getAttributeValue("MaximumIntensity"));
        background = Integer.valueOf(root.getAttributeValue("Background"));
        List<Element> clusterElements = root.getChildren("Cluster");
        X = new Voxel[N];
        z = new GaussianComponent[N];
        int n = 0;
        int k = 0;
        for (Element clusterElement : clusterElements){

            String[] tokens = clusterElement.getAttributeValue("Center").split(" ");
            double[] v = new double[tokens.length];
            for (int i=0 ; i<v.length ; ++i){
                v[i] = Double.valueOf(tokens[i]);
            }
            centers[k] = new ArrayRealVector(v);
            
            GaussianComponent comp = new GaussianComponent(this,k);
            gaussians.add(comp);            
            this.clusterMin[k] = Integer.valueOf(clusterElement.getAttributeValue("MinimumIntensity"));
            this.clusterMax[k] = Integer.valueOf(clusterElement.getAttributeValue("MaximumIntensity"));
            List<Element> pointElements = clusterElement.getChildren("Point");
            for (Element pointElement : pointElements){
                tokens = pointElement.getTextNormalize().split(" ");
                v = new double[tokens.length];
                for (int i=0 ; i<v.length ; ++i){
                    v[i] = Double.valueOf(tokens[i]);
                }
                int in = Integer.valueOf(pointElement.getAttributeValue("Intensity"));
                double adj = Double.valueOf(pointElement.getAttributeValue("Adjusted"));
                X[n] = new Voxel(new ArrayRealVector(v),in,adj);
                comp.addPoint(n, false);
                z[n] = comp;
                ++n;
            }
            ++k;
            comp.calculateStatistics();

        }

    }

    public void saveAsGMMFormatXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("ClusteredVoxels");      
        for (int c=0 ; c<gaussians.size() ; ++c){
            GaussianComponent comp = gaussians.get(c);
            Element ele = new Element("GaussianMixtureModel");
            
            RealVector mu = comp.mean();
            double[] center = mu.toArray();
            StringBuilder builder = new StringBuilder();
            for (int i=0 ; i<center.length ; ++i){
                if (i >0 ){
                    builder.append(" ");
                }
                builder.append(center[i]);
            }
            ele.setAttribute("id ",Integer.toString(c));
            ele.setAttribute("parent","-1");
            ele.setAttribute("count", Integer.toString(comp.getN()));
            ele.setAttribute("m", builder.toString());
            
            RealMatrix W  = comp.precision(mu);
            builder = new StringBuilder();
            for (int row=0 ; row<W.getRowDimension() ; ++row){
                for (int col=0 ; col<W.getColumnDimension() ; ++col){
                    if (row>0 || col>0){
                        builder.append(" ");
                    }
                    builder.append(W.getEntry(row, col));
                }
            }
            ele.setAttribute("W", builder.toString());            
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close(); 
    }
    
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("KMeansClustering");
        root.setAttribute("NumberOfClusters",Integer.toString(centers.length));
        root.setAttribute("Dimensions",Integer.toString(D));
        root.setAttribute("NumberOfPoints",Long.toString(this.getN()));
        root.setAttribute("Background",Integer.toString(background));
        root.setAttribute("MinimumIntensity",Integer.toString(minIntensity));
        root.setAttribute("MaximumIntensity",Integer.toString(maxIntensity));
        for (int c=0 ; c<gaussians.size() ; ++c){
            GaussianComponent comp = gaussians.get(c);
            Element ele = new Element("Cluster");
            
            double[] center = comp.getMean().toArray();
            StringBuilder builder = new StringBuilder();
            for (int i=0 ; i<center.length ; ++i){
                if (i >0 ){
                    builder.append(" ");
                }
                builder.append(center[i]);
            }
            ele.setAttribute("Center", builder.toString());
            
            ele.setAttribute("PointCount", Integer.toString(comp.getN()));
            ele.setAttribute("MinimumIntensity", Integer.toString(this.clusterMin[c]));
            ele.setAttribute("MaximumIntensity", Integer.toString(this.clusterMax[c]));
            for (int n : comp.getIndexes()){
                Element pointEle = new Element("Point");
                double[] point = this.X[n].coords.toArray();
                int intensity = this.X[n].intensity;
                pointEle.setAttribute("Intensity", Integer.toString(intensity));
                pointEle.setAttribute("Adjusted", Double.toString(this.X[n].getAdjusted()));
                builder = new StringBuilder();
                for (int d=0 ; d<point.length ; ++d){
                    if (d > 0 ){
                        builder.append(" ");
                    }
                    builder.append(point[d]);
                } 
                pointEle.addContent(builder.toString());
                ele.addContent(pointEle);
            }
            root.addContent(ele);
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
    }

    // return the cluster of the ith data point
    public int getCluster(int i){
        return z[i].id;
    }    

    public GaussianComponent getGaussian(int i){
        return z[i];
    }

    @Override
    public int getN() {
        return X.length;
    }

    @Override
    public int getD() {
        return D;
    }

    @Override
    public Voxel get(long i) {
        return X[(int)i];
    }
    public RealVector getAsVector(int i){
        return X[i].getAsVector();
    }
    
    // returns the number of clusters
    public int getClusterCount(){
        return gaussians.size();
    }
    
   
    public RealVector getDataMean(){
        ArrayRealVector ret = new ArrayRealVector(getD());
        for (int k=0 ; k<centers.length ; ++k){
            ret = ret.add(centers[k].mapMultiply(gaussians.get(k).getN()));
        }
        return ret.mapDivide(X.length);
    }
    public int getBackground(){
        return background;
    }
    // normalize all the cluster intensities to the same range
    public void normalizeIntensity(double minI,double maxI){
        for (int c=0 ; c<gaussians.size() ; ++c){
            GaussianComponent comp = gaussians.get(c);
            double f = (maxI-minI)/(clusterMax[c]-clusterMin[c]);
            for (int i : comp.getIndexes()){
                X[i].intensity = (int)(minI + (int)(f*(X[i].intensity-clusterMin[c])));
            }
        }
    }
    public List<GaussianComponent> getAllGaussians(){
        return this.gaussians;
    }
    public RealVector getCenter(int cl){
        return centers[cl];
    }
    // return all the vectors (voxel coordinates)  in this cluster
    public RealVector[] getClusterVectors(int cl){
        GaussianComponent comp = gaussians.get(cl);
        Set<Integer> indexes = comp.getIndexes();
        RealVector[] ret = new RealVector[indexes.size()];
        int j = 0;
        for (int i :indexes){
            ret[j] = this.getAsVector(i);
            ++j;
        }
        return ret;
    }

    int D;
    Voxel[] X;
    GaussianComponent[] z;  // the Gaussian component that each voxel is currently assigned
    List<GaussianComponent> gaussians = new ArrayList<>();
    int[] clusterMin;
    int[] clusterMax;
    RealVector[] centers;
    int minIntensity;
    int maxIntensity;
    int background;

    
    public static void main(String[] args) throws Exception{
        ClusteredDataSource source = new ClusteredDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentation0075.xml");

        GaussianMixture gm = new GaussianMixture();
        gm.setSource(source);
        gm.setIterationMax(600);
        gm.setAlpha0(0.001);
        gm.setBeta0(0.000001);
        gm.setNu0(400.0);
        gm.setMu0(source.getDataMean());
        RealMatrix W0 = MatrixUtils.createRealIdentityMatrix(source.getD());
        W0 = W0.scalarMultiply(0.00001);
        gm.setW0(W0);        
        gm.init(source.getClusterCount());
        gm.run(); 
        gm.saveAsXML("/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation_GMM0075.xml");
    }    
}