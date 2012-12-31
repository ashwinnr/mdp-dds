package add;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import util.MySoftReference;
import util.UniPair;
import util.UnorderedPair;
import dd.DDOperation;

/*
 * if( recurse ){
 * 	split
 * 	merge
 * }
 * 
 * finishLeaf
 * 
 * 
 * 11/8 - getINode doesn't respect ordering , it just makes an inode
 * reduce must also account for this case
 * there is going to be a higher level getINode(thatwill use apply) and getLeaf in ADD\
 * 
 * 
 * ADD.getINode will make sure things dont go out of order ever, by using apply and indicator diagrams
 * 
 * options:
 * -change getINode to respect the oredering - by cinstructing indiacator diagrams and using apply
 * -or better, use getINode as it is - reduce will be just a recursive procedure 
 * 
 * basically, reduce here assumes that DDs are always properly ordered and
 * no repetition of vars
 * 
 * both these assumptions are necessary to make sure we dont compse large diagrams 
 * then apply reduce
 * 
 * it is better to do the indicator diagram way and apply
 * 
 * so this procedure just take a well ordered no repetition DD and reduces places where true=false
 */

public class ADDReduceOper extends DDOperation<ADDNode, ADDRNode, ADDINode, ADDLeaf, ADDManager> {

	private ADDManager man;
	private ConcurrentHashMap< MySoftReference<ADDRNode>, MySoftReference<ADDRNode> > _cache;

	public ADDReduceOper( ADDManager man, 
			ConcurrentHashMap<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> cache ){
		
		this.man = man;
		_cache = cache;
		
	}
	
	@Override
	public boolean recurse(ADDRNode node) {

		MySoftReference<ADDRNode> thing = _cache.get(node );
		
		if( thing != null && thing.get() != null){
			return false;
		}else if( thing != null && thing.get() == null ){
			_cache.remove(thing);
		}
		
		ADDNode thenode = node.getNode();
		
		if( thenode != null && thenode instanceof ADDLeaf ){
			return false;
		}
		
		return true;
		
	}

	@Override
	public ADDRNode finishUpLeaf(ADDRNode rnode) {

		ADDNode node = rnode.getNode();
		if( node instanceof ADDLeaf ){
			return rnode;
		}
		
		//check cache
		MySoftReference<ADDRNode> lookup = _cache.get(rnode);
		
		ADDRNode got = null;
		
		try{
			got = lookup.get();
		}catch(NullPointerException e){
			got = null;
		}
		
		if( lookup != null && got != null ){
			return got;
		}
		
		try{
			throw new Exception("not supposed to be here... finishup leaf but not leaf neither in cache");
		}catch( Exception e){
			e.printStackTrace();
		}
		return null;
		
	}
	

	@Override
	public boolean recurse(ADDRNode node1, ADDRNode node2) {

		//not meaningful
		throwIt();
		
		return false;
		
	}

	@Override
	public UnorderedPair<UnorderedPair<String, Boolean>, UnorderedPair<Iterator<ADDRNode>, 
		Iterator<ADDRNode>>> split(
			ADDRNode node1, ADDRNode node2) {
		
		throwIt();
		
		return null;
		
	}

	private void throwIt() {
		try {
			throw new Exception("not supposed to be here... binary reduce call");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	@Override
	public ADDRNode merge(String testVar, boolean negate, List<ADDRNode> newlist) {

		ADDRNode trueBranch = newlist.get(0);
		
		ADDRNode falseBranch = newlist.get(1);
		
		//wiill return truebranch if true==falsebranch
		ADDRNode ret = man.getINode(testVar, trueBranch, falseBranch);
		
		if( negate ){
			//double negation, possibly
			ret = ret.getNegatedNode();
		}
		
		return ret;
		
	}

	@Override
	public ADDRNode finishUpLeaf(ADDRNode node1, ADDRNode node2) {
		
		throwIt();
		
		return null;
	}


	public static void main(String[] args) {
		testADDReduce();
	}

	public static void testADDReduce() {
		
		ADDManager man = new ADDManager(100);
		
		ADDRNode leaf1 = man.getLeaf(4.5, 4.5);
		
		ADDRNode inod = man.getINode("Y", leaf1, man.DD_ONE);
		
		ADDRNode inod1 = man.getINode("X", inod, man.DD_ONE);
		
		man.showGraph(leaf1, inod, inod1);
		
		
	}
	
}
