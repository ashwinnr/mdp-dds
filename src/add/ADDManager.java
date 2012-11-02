package add;

import graph.Graph;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import util.ConcurrentSetPair;
import util.MySoftReference;
import util.Pair;
import util.SetPair;
import util.UniPair;

import dd.DD.DDOper;
import dd.DDManager;


public class ADDManager implements DDManager<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

	//store of null nodes
	protected ConcurrentLinkedQueue< MySoftReference<ADDINode> > storeINodes 
		= new ConcurrentLinkedQueue< MySoftReference<ADDINode> >();
	
	protected ConcurrentLinkedQueue< MySoftReference<ADDLeaf> > storeLeaf 
		= new ConcurrentLinkedQueue< MySoftReference<ADDLeaf> >();

	protected Runtime _runtime = Runtime.getRuntime();
	//all nodes - soft references - cache has to be cleaned up from reference queue
	//keep nodes around as long as possible
	//don't do this since ADDRNode might have a bad hash performance
//	protected Map< Integer, MySoftReference< ADDRNode > > madeNodes 
//		= new ConcurrentHashMap< Integer, MySoftReference< ADDRNode > >();
	
	protected ConcurrentHashMap< String,
				ConcurrentHashMap< Integer, MySoftReference<ADDRNode> > > madeINodes 
			= new ConcurrentHashMap< String, ConcurrentHashMap<Integer, MySoftReference<ADDRNode> > >();
	
	protected ConcurrentHashMap< Integer, MySoftReference<ADDRNode> > madeLeaf = new ConcurrentHashMap< Integer, MySoftReference<ADDRNode> >();
	
	//main methods of getting RNode from INode and Leaf
	//gets the canonical copy - including negated edges
	public <T extends ADDNode> ADDRNode getRNode( T obj ){
		
		int hash = obj.hashCode();
		
		ADDRNode looked = null;
		
		if( obj instanceof ADDINode ){
			
			ADDINode inode = (ADDINode)obj;
			
			String testvar = inode.getTestVariable();
			
			ConcurrentHashMap<Integer, MySoftReference<ADDRNode>> lookin = madeINodes.get(testvar);
			
			if( lookin == null ){
				lookin = new ConcurrentHashMap<Integer, MySoftReference<ADDRNode>>();
				madeINodes.put(testvar, lookin);
			}
			
			looked = lookupMap(hash, lookin);
			
			if( looked == null ){
				
				int negHash = inode.getNegatedHashCode();
			
				looked = lookupMap( negHash, lookin );
				
				if( looked == null ){
					
					looked = new ADDRNode(inode);
					
					lookin.put( inode.hashCode(), new MySoftReference<ADDRNode>(looked, deletedNodes) );
					
				}else{
					looked = looked.getNegatedNode();					
				}
				
			}
			
		}else if( obj instanceof ADDLeaf ){
			
			ADDLeaf leaf = (ADDLeaf)obj;
			
			looked = lookupMap(hash, madeLeaf);
			
			if( looked == null ){
				
				looked = new ADDRNode(leaf);
				
				madeLeaf.put(leaf.hashCode(), new MySoftReference<ADDRNode>(looked, deletedNodes) );
				
			}
			
		}

		return looked;
		
	}

	//creates new null leaf
	//plugs in
	//getRNode
	public ADDRNode getLeaf( Double low, Double high ){
		
		ADDLeaf leaf = (ADDLeaf) getOneNullDD(true);
		
		ADDLeaf ret = null;
		
		ret = leaf.plugIn( new Pair<Double, Double>(low, high) );
		
		return getRNode( ret );
		
	}
	//creates new null inode
	//plugs into that inode
	//get Rnode - gets unique version of that node
	//returns falsebranch if truebranch.equals(falsebranch)
	//WARNING: assumes truebranch and falsebranch are reduced
	public ADDRNode getINode( String testVar, ADDRNode trueBranch, ADDRNode falseBranch ){
		
		//obvious reduction
		
		if( trueBranch.equals(falseBranch) ){
			return trueBranch;
		}
		
		ADDNode n1 = trueBranch.getNode();
		ADDNode n2 = falseBranch.getNode();
		
		//get from store
		ADDINode inode = (ADDINode) getOneNullDD(false);
		
		ADDINode ret = null;
		//plug in
		try {
			 ret = inode.plugIn( testVar, new UniPair<ADDRNode>( trueBranch, falseBranch ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return getRNode(ret);
		
	}
	
	public static void main(String[] args) {
//		testGetRNode();
		testGraph();
	}
	
	public <T extends Comparable<T>> T lookupMap( final int hash, Map<?,MySoftReference<T>> aMap ){
		
		MySoftReference<T> thing = aMap.get(hash);
		
		T gotten = null;
		
		try{
			gotten = thing.get();
		}catch( NullPointerException e ){
		}
		
		if( thing != null && gotten != null ){
			
			return gotten;
			
		}else{
			
			if( thing != null && gotten == null ){
				aMap.remove(hash);
			}
			
			return null;
			
		}
		
	}

//	protected ReferenceQueue< ADDINode > deletedINodes = new ReferenceQueue< ADDINode >();
	
//	protected ReferenceQueue< ADDLeaf > deletedLeafNodes = new ReferenceQueue< ADDLeaf>();
	
	//never deleted
	protected Set< ADDRNode > permenantNodes = Collections.newSetFromMap( 
			new ConcurrentHashMap< ADDRNode, Boolean >() );
	
	private static FileHandler allHandler;
	
	//addnode -> addnode cache for general purpose - NOTE: cear after use always
	private ConcurrentHashMap<ADDRNode, ADDRNode> _tempCache = new ConcurrentHashMap<ADDRNode, ADDRNode>();
	
	//apply cache
	//if a+b not found, look for if exists c such that c-a=b? or c-b=a?
	//if a*b = ?, exists c s.t. c/a=b? or c/b=a?
	//apply will store only +,* 
	//cache : a+b=c can also be read backawards as c-b=a 
	//Map< Map< DDNode, <-> d
	// a,b -> c
	// d,e -> c
	// want to get c,b -> a
	

	//caches can be in terms of RNodes
	protected ConcurrentHashMap< DDOper, ConcurrentHashMap< 
			Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> >  , MySoftReference<ADDRNode> > > 
		applyCache = new ConcurrentHashMap< 
		DDOper, ConcurrentHashMap< Pair< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > >, 
			MySoftReference<ADDRNode> > > ();
	
	//we will be reducing only INodes 
	//using RNodes here just to deal with negated edges
	protected ConcurrentHashMap< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > > reduceCache = 
			new ConcurrentHashMap<
			MySoftReference< ADDRNode >, MySoftReference< ADDRNode > >();
	
	static{
		try {
			allHandler = new FileHandler("./log/" + ADDManager.class.getName());
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	private final static Logger LOGGER = Logger.getLogger(ADDManager.class.getName()); 

	private final static ConsoleHandler consoleHandler = new ConsoleHandler();

	protected static int STORE_INCREMENT;
	
	private static ReferenceQueue<ADDRNode> deletedNodes = new ReferenceQueue<ADDRNode>();

	
	//TODO:
	//1. maps of Inode and Dnode separately
	//2. method for Inode, Dnode to RNode(must check for negation)
	//3. soft references to Rnodes
	//4. update clear null entries etc
	
	public ADDManager( final int numDDs ){
		
		addToStore( numDDs, true, true );
		
	}
	
	public <T extends ADDNode > MySoftReference<ADDRNode> getSoftRNode(T obj){
		
		return new MySoftReference<ADDRNode>( getRNode(obj) );
		
	}
	
	//parent is going to be ADD
	@Override
	public void createStore( final int NumDDs ) {
		
		this.STORE_INCREMENT = NumDDs;
		
		LOGGER.setUseParentHandlers(false);
		
		allHandler.setFormatter( new SimpleFormatter() );
		
		consoleHandler.setFormatter( new SimpleFormatter() );

		LOGGER.setLevel(null);//probably parent will have ALL
		
		allHandler.setLevel(Level.ALL);
		
		consoleHandler.setLevel(Level.WARNING);
		
		LOGGER.addHandler(allHandler);
		
		LOGGER.addHandler(consoleHandler);
		
		storeINodes = new ConcurrentLinkedQueue< MySoftReference<ADDINode> >();
		
		storeLeaf = new ConcurrentLinkedQueue< MySoftReference<ADDLeaf> >();

		addToStore(NumDDs, true, true);
		
		LOGGER.exiting( this.getClass().getName() , "create store");
		
	}

	@Override
	public void addPermenant(ADDNode input) {
		
		LOGGER.entering( this.getClass().getName(), "Add permenant");
		
		ADDRNode rnode = getRNode(input);
		
		boolean ret = permenantNodes.add( rnode );
		
		LOGGER.info(" adding permenant " + input + ". Was already in set " + ret );
		
		LOGGER.exiting( this.getClass().getName(),  "Add permenant");
		
	}

	@Override
	public void addToStore(final int NumDDs, final boolean leaf, final boolean node) {
		
		LOGGER.entering( this.getClass().getName(), "addtostore" );

		if( leaf ){

			int i = 0;
			
			for( i = 0 ; i < NumDDs; ++i ){
				
				try{
					
					storeLeaf.add( new ADDLeaf().getNullDD() );
				
				}catch(OutOfMemoryError e){
					
					LOGGER.severe("out of memory error. added " + i + " leaves." );
					return;
					
				}
				
			}	
	
		}
				
		if( node ){
		
			int i = 0;
			
			for( i = 0 ; i < NumDDs; ++i ){
				
				try{
					
					storeINodes.add( new ADDINode().getNullDD() );
					
				}catch(OutOfMemoryError e){
					
					LOGGER.severe("out of memory error. added " + i + " nodes." );
					return;
					
				}
				
			}	

		}
		
		LOGGER.info("added " + NumDDs + " leaves: " + leaf + " and nodes: " + node);
		
		LOGGER.exiting( this.getClass().getName(), "addtostore" );

	}

	@Override
	public void nullifyDD(ADDNode input) {

		LOGGER.entering( this.getClass().getName(), "nullifyDD");
		
		input.nullify();
		
		input = null;
		
		LOGGER.exiting( this.getClass().getName(), "nullifyDD");
		
	}

	@Override
	public ADDNode getOneNullDD( boolean leaf ) {

		LOGGER.entering( this.getClass().getName(), "getOneNullDD");
		
		ADDNode ret = null;
		
		if( leaf ){
		
			boolean empty = storeLeaf.isEmpty();
			
			if( empty ){
				LOGGER.warning("store has no more leaves. Adding " + STORE_INCREMENT + " leaves ");
				addToStore(STORE_INCREMENT, true, false);
			}
			
			ret = storeLeaf.poll().get();
			
		}else{
			
			boolean empty = storeINodes.isEmpty();
			
			if( empty ){
				LOGGER.warning("store has no more nodes. Adding " + STORE_INCREMENT + " nodes ");
				addToStore(STORE_INCREMENT, false, true);
			}
			
			ret = storeINodes.poll().get();
			
			
		}
		
		LOGGER.exiting(this.getClass().getName(), "getOneNullDD");
		
		return ret;
		
	}

	private synchronized void addPair( ADDRNode a, ADDRNode b, DDOper op, ADDRNode res ){
	
		LOGGER.entering(this.getClass().getName(), "add pair");
		
		if( applyCache.get(op) == null ){
			applyCache.put(op, new ConcurrentHashMap< Pair< MySoftReference<ADDRNode>, MySoftReference<ADDRNode> >, 
					MySoftReference<ADDRNode> >());
		}
		
		ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>, 
			MySoftReference<ADDRNode>> theMap =
				applyCache.get(op);
		
		theMap.putIfAbsent( new Pair< MySoftReference<ADDRNode>, MySoftReference<ADDRNode> >( 
				new MySoftReference<ADDRNode>(a),
				new MySoftReference<ADDRNode>(b) ), 
				new MySoftReference<ADDRNode>(res) );
	
		LOGGER.exiting(this.getClass().getName(), "add pair");
		
		
	}

	public synchronized void addToApplyCache( ADDRNode a, ADDRNode b, DDOper op, ADDRNode res ){
		
		addPair(a,b,op,res);
		
		if( ADD.isCommutative(op) ){
			addPair(b, a, op, res);
		}
		
		DDOper compOp = ADD.getComplement(op);
		//a+b=c => c-a=b, c-b=a, 
		//a-b=c => c+b=a, a-c=b, 
		//a*b=c, c/a=b, c/b=a
		//a/b=c => c*b=a, a/c=b
		
		addPair(res, b, compOp, a);
		if( ADD.isCommutative(op) ){
			addPair(res, a, compOp, b);
		}else{
			addPair(a, res, op, b);
		}
		
		
	}
	
	private ADDRNode lookupPair( ADDRNode a, ADDRNode b, DDOper op ){
		
		ADDRNode ret = null;
		
		if( applyCache.get(op) == null ){
			ret = null;
		}else{

			ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>, 
				MySoftReference<ADDRNode>> theMap =
					applyCache.get(op);
			
			MySoftReference<ADDRNode> refa = new MySoftReference<ADDRNode>(a);
			MySoftReference<ADDRNode> refb = new MySoftReference<ADDRNode>(b);

			Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> key = 
					new Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>( refa, refb );
			
			if( theMap.containsKey( key ) ){
				MySoftReference<ADDRNode> thing = theMap.get(key);
				if( thing != null ){
					ret = thing.get();
				}else{
					theMap.remove(key);
				}
			}
			
		}
		
		return ret;
				
	}

	public ADDRNode lookupApplyCache( ADDRNode a, ADDRNode b, DDOper op ){
		
		ADDRNode ret = null;
		
		ret = lookupPair(a, b, op);
		
		if( ret == null ){

			if( ADD.isCommutative(op) ){
				ret = lookupPair(b, a, op);
			}
			
		}
		
		return ret;
		
	}
	
	@Override
	public void flushCaches() {
		
		this._tempCache.clear();

		applyCache.clear();
		
		reduceCache.clear();
		
//		clearDeadNodes();
		
	}
	
	public static void testGetRNode(){
		
		ADDManager man = new ADDManager(100);
		
		ADDRNode leaf = man.getLeaf(1.32, 5.34);
		
		ADDRNode leaf2 = man.getLeaf( 4.3, 6.5 );
		
		System.out.println(leaf);
		
		System.out.println(leaf2);
		
		ADDRNode inod1 = man.getINode("X", leaf, leaf2);
		
		System.out.println(inod1);

		ADDRNode leaf3 = man.getLeaf( 4.3, 6.5 );
		
		System.out.println(leaf);
		
		ADDRNode inod3 = man.getINode("X", leaf, leaf3);
		
		System.out.println(inod3);
		
		man.memorySummary();
		
		ADDRNode aleaf = man.getINode("X", leaf, leaf);
		
		System.out.println(aleaf);
		
		man.memorySummary();
		
		ADDRNode inod4 = man.getINode("X", leaf3, leaf);
		
		System.out.println(inod4);
		
		man.memorySummary();
	
	}
	
	public static void testGraph(){
		
		ADDManager man = new ADDManager(100);
		
		ADDRNode leaf = man.getLeaf(1.32, 5.34);
		
//		man.showGraph(leaf);
		
		ADDRNode leaf2 = man.getLeaf( 4.3, 6.5 );
		
//		man.showGraph(leaf2);
		
		ADDRNode inod1 = man.getINode("X", leaf, leaf2);
		
		man.showGraph(inod1);
		
		ADDRNode inod4 = man.getINode("X", leaf2, leaf);
		
		man.showGraph(inod4);
		
		ADDRNode inod5 = man.getINode("Y", inod1, inod4);
		
		man.showGraph( inod5 );
		
		ADDRNode inod6 = man.getINode("Z", leaf2, leaf);
		
		ADDRNode inod7 = man.getINode("K", inod6, inod5);
		
		man.showGraph(inod6);
		
		man.showGraph(inod7);
		
	}
	
	public void showGraph(ADDRNode node) {
		
		Graph g = new Graph(true, false, false, false);
		node.toGraph(g);
		g.launchViewer();
		
	}

	public void memorySummary(){
		
		System.out.println( madeINodes.size() + " Inodes " );
		System.out.println( madeLeaf.size() + " leaves" );
		
		System.out.println("memory : " + (_runtime.totalMemory()-_runtime.freeMemory())/((double)_runtime.totalMemory()));
		
	}
	
//	public void clearDeadNodes(){
//
//		clearUnaryIntegerMap( madeINodes, madeLeaf );
//		
//	}
//	
//	public void clearUnaryIntegerMap( ConcurrentHashMap< Integer, MySoftReference >)
//
//	public void clearNullEntriesBinary(
//			ConcurrentHashMap< 
//				Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> >  
//					, MySoftReference<ADDRNode> >... caches ){
//	
//		for( Map< 
//				Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> >  
//		, MySoftReference<ADDRNode> >
//				cache : caches ){
//			
//			Set<Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>, 
//				MySoftReference<ADDRNode>>> entrydert = cache.entrySet();
//			
//			for( Map.Entry< Pair< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > >, 
//				MySoftReference<ADDRNode> > entry : entrydert ){
//			
//				if( entry.getKey()._o1 == null || entry.getKey()._o2 == null || entry.getValue() == null ){
//					entrydert.remove( entry );
//				}
//				
//			}
//		}
//	}
//	
//	
//	public void clearNullEntriesUnary( ConcurrentHashMap< MySoftReference< ADDRNode >, 
//			MySoftReference< ADDRNode > >... caches ){
//	
//		
//		for( Map< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > > cache : caches ){
//			Set<Entry<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>> entrydert = cache.entrySet();
//			
//			for( Map.Entry<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> entry :  entrydert ){
//				
//				if( entry.getKey() == null || entry.getValue() == null ){
//					entrydert.remove(entry);
//				}
//				
//			}
//		}
//		
//	}
//	
//	
//	public void clearDeadEntries(){
//		
//		for( DDOper op : applyCache.keySet() ){
//			
//			clearNullEntriesBinary( applyCache.get(op) );
//			
//		}
//		
//		clearNullEntriesUnary( reduceCache );
//		
//	}

}
