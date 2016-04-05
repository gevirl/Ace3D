/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class RealViewsExample2
{
	public static void main( String[] args ) throws ImgIOException
	{
		final Img< FloatType > img = new ImgOpener().openImg( "/net/waterston/vol2/home/gevirl/Downloads/advanced-imglib2/images/bee-1.tif", new ArrayImgFactory< FloatType >(), new FloatType() );

		RandomAccessible< FloatType > input = Views.extendValue( img, new FloatType( 128 ) );
		RealRandomAccessible< FloatType > interpolated = Views.interpolate( input, new NLinearInterpolatorFactory<FloatType>() );

		AffineTransform2D affine = new AffineTransform2D();
		affine.scale( 2.3 );
		affine.translate( -400.0, -200.0 );

		RealRandomAccessible< FloatType > realview = RealViews.affineReal( interpolated, affine );

		RandomAccessibleInterval< FloatType > view = Views.interval( Views.raster( realview ), img );

		ImageJFunctions.show( view );

		// alternatively using RealViews.affine():
		RandomAccessible< FloatType > view2 = RealViews.affine( interpolated, affine );
		ImageJFunctions.show( Views.interval( view2, img ) );
	}
}
