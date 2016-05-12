/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.rhwlab.ace3d.SingleSlicePanel;

/**
 *
 * @author gevirl
 */
public class TGMMNucleus extends Nucleus {
    public TGMMNucleus(int time,Element gmm){
        super(time,name(time,gmm),center(gmm),10.0);  // for now make all radii the same
        id = gmm.getAttributeValue("id");
        parent = gmm.getAttributeValue("parent");
        a = precision(gmm);

        
        scale = scale(gmm);
        double yz = a[y][z] + a[z][y];
        double xz = a[x][z] + a[z][x];
        double xy = a[x][y] + a[y][x];
        denom = 4.0*a[x][x]*a[y][y]*a[z][z] + xy*xz*yz - a[x][x]*yz*yz - a[z][z]*xy*xy - a[y][y]*xz*xz;
        delZ = Math.sqrt((4.0*a[x][x]*a[y][y] - xy*xy)/denom);
        delY = Math.sqrt((4.0*a[x][x]*a[z][z] - xz*xz)/denom);
        delX = Math.sqrt((4.0*a[y][y]*a[z][z] - yz*yz)/denom);
        int iuhsfdui=0;
    }
    @Override
    public Shape getShape(long slice,int dim,int bufW,int bufH){
        System.out.printf("%s dim=%d  slice=%d\n",this.getName(),dim,slice);
        System.out.printf("Ellipsoid center = (%d,%d,%d)\n",this.xC,this.yC,this.zC);
        Ellipse2d e;
        switch(dim){
            case 0:
                e = xPlaneEllipse((double)slice);
                break;
            case 1:
                e = yPlaneEllipse((double)slice);
                break;            
            default:
                e = zPlaneEllipse((double)slice);
                break;                 
        }
        if (e != null){
            AffineTransform toOrigin = AffineTransform.getTranslateInstance(-e.x,-e.y);
            AffineTransform back = AffineTransform.getTranslateInstance(e.x, e.y);
            AffineTransform xform = AffineTransform.getRotateInstance(e.cosine, e.sine);
            int scrX = SingleSlicePanel.screenX(e.low,dim,bufW);
            int scrY = SingleSlicePanel.screenY(e.low,dim,bufH);
            int scrHighX = SingleSlicePanel.screenX(e.high,dim,bufW);
            int scrHighY = SingleSlicePanel.screenY(e.high,dim,bufH);
            Shape shape = new Ellipse2D.Double(scrX,scrY,scrHighX-scrX,scrHighY-scrY); 
            shape = toOrigin.createTransformedShape(shape);
            shape =  xform.createTransformedShape(shape);
            shape = back.createTransformedShape(shape);
System.out.printf("scrX:%d scrY:%d e.x:%f e.y:%f\n",scrX,scrY,e.x,e.y);
            return shape;
        }
        return null;
    }
    public Ellipse2d zPlaneEllipse(double v){
        if (Math.abs(v-this.zC) < delZ){
            return ellipse(0,1,2,v);
        }
        return null;
    }
    public Ellipse2d yPlaneEllipse(double v){
        if (Math.abs(v-this.yC) < delY){
            return ellipse(0,2,1,v);
        }
        return null;
    }
    public Ellipse2d xPlaneEllipse(double v){
        if (Math.abs(v-this.xC) < delX){
            return ellipse(1,2,0,v);
        }
        return null;
    }
    public Ellipse2d ellipse(int xi,int yi,int zi,double v){
        Coeff coef = coef(xi,yi,zi,v);
        return ellipse(xi,yi,zi,coef);
    }
    /*
    public Coeff zPlaneCoeff(double v){
        return coef(0,1,2,v);
    }
    public Coeff yPlaneCoeff(double v){
        return coef(0,2,1,v);
    }
    public Coeff xPlaneCoeff(double v){
        return coef(1,2,0,v);
    }
    */
    public Coeff coef(int xi,int yi,int zi,double v){
        Coeff c = new Coeff();
        long[] ce = this.getCenter();
        
        v = v-ce[zi];
        c.A = a[xi][xi];
        c.B = a[xi][yi] + a[yi][xi];
        c.C = a[yi][yi];
        c.D = v*(a[xi][zi] + a[zi][xi]);
        c.E = v*(a[yi][zi] + a[zi][yi]);
        c.F = a[zi][zi]*v*v-1.0;
        
        c.x = ce[xi];
        c.y = ce[yi];
        
        c.d = c.D - (2*a[xi][xi]*ce[xi] + (a[yi][xi]+a[xi][yi])*ce[yi]);
        c.e = c.E - (2*a[yi][yi]*ce[yi] + (a[yi][xi]+a[xi][yi])*ce[xi]);
        c.f = c.F + a[xi][xi]*ce[xi]*ce[xi] + (a[yi][xi]+a[xi][yi])*ce[xi]*ce[yi] - v*(a[zi][xi]*ce[xi]+a[zi][yi]*ce[yi]+a[xi][zi]*ce[xi]+a[yi][zi]*ce[yi]);
        
        System.out.printf("Coef: A=%f,B=%f,C=%f,D=%f,E=%f,F=%f,d=%f,e=%f,f=%f\n",c.A,c.B,c.C,c.D,c.E,c.F,c.d,c.e,c.f);
        return c;
    }
    public  Ellipse2d ellipse(int xi,int yi,int zi,Coeff coef){
        Q =  new Array2DRowRealMatrix(3,3);
        Q.setEntry(0,0,coef.A);
        Q.setEntry(1,0,coef.B/2);
        Q.setEntry(0,1,coef.B/2);
        Q.setEntry(1,1,coef.C);
        Q.setEntry(2,0,coef.D/2);
        Q.setEntry(0,2,coef.D/2);
        Q.setEntry(2,1,coef.E/2);
        Q.setEntry(1,2,coef.E/2);
        Q.setEntry(2,2,coef.F);
        EigenDecomposition ed = new EigenDecomposition(Q);
        detQ = ed.getDeterminant();
        
        RealMatrix rm = new Array2DRowRealMatrix(2,2);
        rm.setEntry(0,0, coef.A);
        rm.setEntry(1,1, coef.C);
        rm.setEntry(0,1,coef.B/2.0);
        rm.setEntry(1,0,coef.B/2.0);
        EigenDecomposition eigenDecomp = new EigenDecomposition(rm);
        double detA33 = eigenDecomp.getDeterminant();
        double[] eigenValues = eigenDecomp.getRealEigenvalues();
System.out.printf("Eigenvalues: %f,%f\n",eigenValues[0],eigenValues[1])        ;
        RealVector eigenvector0 = eigenDecomp.getEigenvector(0);
        RealVector eigenvector1 = eigenDecomp.getEigenvector(1)       ;
        
        
        Ellipse2d e = new Ellipse2d();
        double cot2theta  = (coef.A-coef.C)/coef.B;
        if (Double.isFinite(cot2theta)){
            double d= Math.sqrt(1.0+cot2theta*cot2theta);
            double cos2theta = cot2theta/d;
            e.cosine = Math.sqrt((1.0 + cos2theta)/2.0);
            e.sine = Math.sqrt((1.0 - cos2theta)/2.0);
        } else {
            e.cosine = 1.0;
            e.sine = 0.0;            
        }
/*        
        e.a = 1.0/Math.sqrt(eigenValues[0]);
        e.b = 1.0/Math.sqrt(eigenValues[1]);
        e.cosine = eigenvector0.getEntry(0);
        e.sine = eigenvector0.getEntry(1);   
*/      
        double dd =  coef.B*coef.B - 4.0*coef.A*coef.C;
        double xc = ( 2.0*coef.C*coef.D - coef.B*coef.E)/dd;
        double xcn = ( 2.0*coef.C*coef.d - coef.B*coef.e)/dd;
        double yc = ( 2.0*coef.A*coef.E - coef.B*coef.D)/dd;
        double ycn = ( 2.0*coef.A*coef.e - coef.B*coef.d)/dd;
System.out.printf("dd=%f,xc=%f,xcn=%f,yc=%f,ycn%f\n",dd,xc,xcn,yc,ycn);
        e.x = coef.x + xc;
        e.y = coef.y + yc;
        
//        double G = coef.A*xcn*xcn + coef.B*xcn*ycn + coef.C*ycn*ycn - coef.f;
//        double H = coef.A + coef.C;
 //       double b2 = (H + Math.sqrt(H*H-4.0*G))/2.0;
        double gamma = 0.5*coef.B/(e.cosine*e.sine);
        
//        double a2 = G/b2;
        double b2 = 0.5*(coef.A + coef.C + gamma);
        double bn = Math.sqrt(b2);
        double a2 = coef.A + coef.C - b2;
        double an = Math.sqrt(a2);
System.out.printf("Test: gamma=%f, a2=%f, b2=%f , a=%f , b=%f\n",gamma,a2,b2,an,bn);
        double T = e.sine*e.cosine;
        if (T != 0.0) {
            e.a = 1.0/Math.sqrt(coef.A/(-coef.F)+coef.C/(-coef.F) + coef.B/((-coef.F)*T) );
            e.b = 1.0/Math.sqrt(coef.A/(-coef.F)+coef.C/(-coef.F) + coef.B/(-coef.F*T));
        } else {
            e.a = 1.0/Math.sqrt(coef.A/(-coef.F)+coef.C/(-coef.F)  );
            e.b = 1.0/Math.sqrt(coef.A/(-coef.F)+coef.C/(-coef.F) );            
        }
        double f = -detQ/detA33;
        e.a = 1.0/Math.sqrt(eigenValues[0]/f);
        e.b = 1.0/Math.sqrt(eigenValues[1]/f);
        e.low[xi] = (long)(e.x - e.a);
        e.low[yi] = (long)(e.y - e.b);
        e.low[zi] = 0;
        e.high[xi] = (long)(e.x + e.a);
        e.high[yi] = (long)(e.y + e.b);
        e.high[zi] = 0;
System.out.printf("Ellipse: a=%f,b=%f,x=%f,y=%f\n", e.a,e.b,e.x,e.y);
        return e;
    }
    static long[] center(Element gmm){
        long[] ret = new long[3];
        String[] tokens = gmm.getAttributeValue("m").split(" ");
        for (int i =0 ; i<ret.length ; ++i) {
            ret[i] = (long)(Double.parseDouble(tokens[i])+.5);
        }
        return ret;
    }
    static double[][] precision(Element gmm){
        
        double [][] ret = new double[3][3];
        String[] tokens = gmm.getAttributeValue("W").split(" ");
        for (int i=0 ; i<3 ; ++i){
            for (int j=0 ; j<3 ; ++j){
                ret[i][j] = Double.valueOf(tokens[3*i+j])*R;
            }
        }
        return ret;
    }
    static float[] scale(Element gmm){
        float[] ret = new float[3];
        String[] tokens = gmm.getAttributeValue("scale").split(" ");
        for (int i=0 ; i<3 ; ++i){
            ret[i] = Float.valueOf(tokens[i]);
        }
        return ret;        
    }
    static String name(int time,String id){
        int n = Integer.valueOf(id);
        return String.format("%03d_%03d", time,n);
    }
    static String name(int time,Element gmm){
        return name(time,gmm.getAttributeValue("id"));
    }
    public String getID(){
        return id;
    }
    public String getParent(){
        return name(this.time-1,parent);
    }
    public class Coeff {
        double A;
        double B;
        double C;
        double D;
        double E;
        double F; 
        long x;
        long y;
        double d;
        double e;
        double f;
    }
    public class Ellipse2d {
        double x;
        double y;
        double sine;
        double cosine;
        double a;
        double b;
        long[] low = new long[3];
        long[] high = new long[3];
    }
    static double R=40.0;
    static int x=0;
    static int y=1;
    static int z=2;
    double denom;
    double delX;
    double delY;
    double delZ;
    String id;
    String parent;
    double[][] a;
    float[] scale;
    RealMatrix Q;
    double detQ;
}

