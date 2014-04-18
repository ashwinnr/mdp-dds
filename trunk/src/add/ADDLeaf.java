package add;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import util.MySoftReference;
import util.Pair;
import dd.DDLeaf;

public class ADDLeaf extends DDLeaf< Pair<Double, Double> > implements ADDNode, Comparable<ADDLeaf> {

    	public final static double PRECISION = 1e-9;
    
	@Override
	public ADDLeaf getNullDD() {
		this.leafValues = new Pair<Double, Double>(Double.NaN, Double.NaN);
		return this;
	}

	@Override
	public ADDLeaf plugIn( final Pair<Double, Double> leafVals ) {
//		if( leafVals._o1 > leafVals._o2 ){
//			try{
//				throw new Exception("lower bound higher than upper bound");
//			}catch( Exception e ){
//				e.printStackTrace();
////				System.exit(1);
//			}
//		}
//		final double max = Math.max( leafVals._o1, leafVals._o2 );
//		final double min = Math.min( leafVals._o1, leafVals._o2 );
		this.leafValues.copy( leafVals._o1, leafVals._o2 );
		return this;
	}

	@Override
	public void updateMinMax() {
		return;
	}

	@Override
	public double getMax() {
		return Math.max(this.leafValues._o1,  this.leafValues._o2);
	}

	@Override
	public double getMin() {
		return Math.min(this.leafValues._o1,  this.leafValues._o2);	
	}

	@Override
	public int compareTo(ADDLeaf o) {
		
		if( o instanceof ADDLeaf ){
			
			return leafValues.compareTo(((ADDLeaf) o).leafValues);
			
		}

		try{
			throw new UnsupportedOperationException("addleaf comparing to not one " + o.getClass().getName() );
		}catch( UnsupportedOperationException e ){
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		
		return -1;
	}

	public void printHash(){
		int h1 = leafValues._o1.hashCode();
		int h2 = leafValues._o2.hashCode();
		
		System.out.println( h1 + " " + h2  + " " + (h1^h2) );
	}
	
	@Override
	public int hashCode() {
		int ret;
		if( this.leafValues._o1 == Double.NEGATIVE_INFINITY ){
			ret = -1;
		}else{
			ret = super.hashCode();
//			double avg = ( leafValues._o1 + leafValues._o2 ) / 2d;
//			double sign1 = Math.abs( leafValues._o1 );
//			double sign2 = Math.abs( leafValues._o2 );
		}
		
//		System.out.println( "Leaf hashcode : " + this + " " + ret );
		return ret;
	}
	
	@Override
	public boolean equals(Object obj) {
//	    System.out.println("====");
//	    System.out.println(this.toString() );
//	    System.out.println( obj.toString() );
	    
	    final ADDLeaf leaf = (ADDLeaf) obj;
	    if( (leaf.getMax() == Double.NEGATIVE_INFINITY && this.getMax()!=  Double.NEGATIVE_INFINITY) 
		    || (leaf.getMax() != Double.NEGATIVE_INFINITY && this.getMax() == Double.NEGATIVE_INFINITY ) ){
		return false;
	    }else if( leaf.getMax() == Double.NEGATIVE_INFINITY && this.getMax() == Double.NEGATIVE_INFINITY ){
		return true;
	    }
	   
	    return super.equals(obj);
	}

	public void plugIn(Double low, Double high) {
	    if( high < low ){
		try{
		    throw new Exception("invalid leaf " + low + high );
		}catch( Exception e ){
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	    leafValues._o1 = low;
	    leafValues._o2 = high;
	}

}
