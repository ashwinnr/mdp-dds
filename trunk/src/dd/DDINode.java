package dd;

import graph.Graph;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import util.Pair;

import add.ADDRNode;

public abstract class DDINode<D extends DDNode, DR extends DDRNode<D>, C extends Collection<DR> > implements DDNode {

	protected static final int HASH_MULTIPLIER = 17;

	protected static final int HASH_INIT = 31;

	protected String testVariable = null;
	
	protected C children = null;
	
	//to avoid recursive computation of hashcode
	protected Integer _nHashCode = null;
	
	protected double _nMax = Double.NaN, _nMin = Double.NaN;
	
	@Override
	public int hashCode() {
		
		if( _nHashCode != null ){
			return _nHashCode;
		}
		
		int res = HASH_INIT;
		
		res = HASH_MULTIPLIER*res + children.hashCode();
		
		_nHashCode = res;
		
		return res;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof DDINode ){
		
			DDINode<D,DR,C> thing = (DDINode<D,DR,C>)obj;
					
			//internalized, so use ==
			return this.testVariable == thing.testVariable 
					&& this.children.equals( thing.children );
			
		}
		return false;
		
	}
	
	public abstract DDINode<D,DR,C> plugIn(final String testVar, final C child ) throws Exception;

	@Override
	public void nullify() {
		this._nHashCode = null;
		this._nMax = this._nMin = Double.NaN;
		this.children = null;
		this.testVariable = null;
	}
	
	@Override
	public double getMax(){
		
		if( Double.isNaN(_nMax) ){
			updateMinMax();
		}
		
		return _nMax;
		
	}
	
	@Override
	public double getMin(){
		
		if( Double.isNaN(_nMin) ){
			updateMinMax();
		}
		
		return _nMin;
		
	}

	@Override
	public void updateMinMax() {

		if( Double.isNaN(_nMax) || Double.isNaN(_nMin) ){
		
			Iterator<DR> iter = children.iterator();
			
			while( iter.hasNext() ){
			
				DR dr = iter.next();
				
				_nMax = Math.max( dr.getNode().getMax(), 
						_nMax);
				
				_nMin = Math.min( dr.getNode().getMin(), 
						_nMin);
				
			}
			
		}
		
	}
	
}
