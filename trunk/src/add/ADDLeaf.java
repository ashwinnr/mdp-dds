package add;

import util.MySoftReference;
import util.Pair;
import dd.DDLeaf;

public class ADDLeaf extends DDLeaf< Pair<Double, Double> > implements ADDNode, Comparable<ADDLeaf> {

	@Override
	public MySoftReference<ADDLeaf> getNullDD() {
		this.leafValues = new Pair<Double, Double>(Double.NaN, Double.NaN);
		return new MySoftReference<ADDLeaf>(this);
	}

	@Override
	public ADDLeaf plugIn( final Pair<Double, Double> leafVals ) {
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

	@Override
	public int hashCode() {
		int ret;
		if( this.leafValues._o1 == Double.NEGATIVE_INFINITY ){
			ret = -1;
		}else{
			ret = super.hashCode();
		}
		
//		System.out.println( "Leaf hashcode : " + this + " " + ret );
		return ret;
	}

}
