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

	protected static final int HASH_SHIFT = 4;

	protected static final int HASH_INIT = 29;

	protected String testVariable = null;
	
	protected C children = null;
	
	//to avoid recursive computation of hashcode
	protected int _nHashCode;
	
	protected boolean hashSet = false;
	
	protected double _nMax = Double.NaN, _nMin = Double.NaN;
	
	public String getTestVariable() {
		return testVariable;
	}

	public abstract int hashCode();
	
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
		this._nHashCode = -1;
		this._nMax = this._nMin = Double.NaN;
		this.children = null;
		this.testVariable = null;
		hashSet = false;
	}
	
	@Override
	public String toString() {
		return this.testVariable ;
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
	
	public abstract void setHashCode(int numb);
	
	public boolean equalChildren(){

		Iterator<DR> it = children.iterator();
		
		DR ex = null;
		
		while( it.hasNext() ){
			
			if( ex == null ){
				ex = it.next();
				continue;
			}
			
			DR thisone = it.next();
			
			if( ! ex.equals(thisone) ){
				return false;
			}
			
		}
		
		return true;

	}

	public C getChildren() {
		return children;
	}
	
}
