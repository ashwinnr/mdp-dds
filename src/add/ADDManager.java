package add;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

import dd.DD.DDOper;
import dd.DDManager;


public class ADDManager implements DDManager<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

	protected ConcurrentLinkedQueue< MySoftReference<ADDINode> > storeINodes 
		= new ConcurrentLinkedQueue< MySoftReference<ADDINode> >();
	
	protected ConcurrentLinkedQueue< MySoftReference<ADDLeaf> > storeLeaf 
		= new ConcurrentLinkedQueue< MySoftReference<ADDLeaf> >();
	
	//all nodes - soft references - cache has to be cleaned up from reference queue
	//keep nodes around as long as possible
	protected SetPair< MySoftReference< ADDINode > , MySoftReference< ADDLeaf > > allNodes 
		= new ConcurrentSetPair< MySoftReference< ADDINode >, MySoftReference< ADDLeaf > >();
	

//	protected ReferenceQueue< ADDINode > deletedINodes = new ReferenceQueue< ADDINode >();
	
//	protected ReferenceQueue< ADDLeaf > deletedLeafNodes = new ReferenceQueue< ADDLeaf>();
	
	//never deleted
	protected SetPair< ADDINode, ADDLeaf > permenantNodes = new ConcurrentSetPair< ADDINode, ADDLeaf >();
	
	private static FileHandler allHandler;
	
	//addnode -> addnode cache for general purpose - NOTE: cear after use always
	private ConcurrentHashMap<ADDINode, ADDNode> _tempICache = new ConcurrentHashMap<ADDINode, ADDNode>();
	
	private ConcurrentHashMap< ADDLeaf, ADDNode > _tempDCache = new ConcurrentHashMap<ADDLeaf, ADDNode>();
	
	//apply cache
	//if a+b not found, look for if exists c such that c-a=b? or c-b=a?
	//if a*b = ?, exists c s.t. c/a=b? or c/b=a?
	//apply will store only +,* 
	//cache : a+b=c can also be read backawards as c-b=a 
	//Map< Map< DDNode, <-> d
	// a,b -> c
	// d,e -> c
	// want to get c,b -> a
	
	
	protected ConcurrentHashMap< DDOper, ConcurrentHashMap< 
			Pair< MySoftReference<ADDINode>,  MySoftReference<ADDINode> >  , MySoftReference<ADDNode> > > 
		applyCacheINodes = new ConcurrentHashMap< 
		DDOper, ConcurrentHashMap< Pair< MySoftReference< ADDNode >, MySoftReference< ADDNode > >, MySoftReference<ADDNode> > > ();
	
	protected ConcurrentHashMap< MySoftReference< ADDNode >, MySoftReference< ADDNode > > reduceCache = new ConcurrentHashMap<
			MySoftReference< ADDNode >, MySoftReference< ADDNode > >();
	
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
		
		boolean ret = permenants.add(input);
		
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

	private synchronized void addPair( ADDNode a, ADDNode b, DDOper op, ADDNode res ){
	
		LOGGER.entering(this.getClass().getName(), "add pair");
		
		if( applyCache.get(op) == null ){
			applyCache.put(op, new ConcurrentHashMap< Pair< MySoftReference<ADDNode>, MySoftReference<ADDNode> >, 
					MySoftReference<ADDNode> >());
		}
		
		ConcurrentHashMap<Pair<MySoftReference<ADDNode>, MySoftReference<ADDNode>>, MySoftReference<ADDNode>> theMap =
				applyCache.get(op);
		
		theMap.putIfAbsent( new Pair< MySoftReference<ADDNode>, MySoftReference<ADDNode> >( new MySoftReference<ADDNode>(a),
				new MySoftReference<ADDNode>(b) ), 
				new MySoftReference<ADDNode>(res) );
	
		LOGGER.exiting(this.getClass().getName(), "add pair");
		
		
	}

	public synchronized void addToApplyCache( ADDNode a, ADDNode b, DDOper op, ADDNode res ){
		
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
	
	private ADDNode lookupPair( ADDNode a, ADDNode b, DDOper op ){
		
		ADDNode ret = null;
		
		if( applyCache.get(op) == null ){
			ret = null;
		}else{

			ConcurrentHashMap<Pair<MySoftReference<ADDNode>, MySoftReference<ADDNode>>, MySoftReference<ADDNode>> theMap =
					applyCache.get(op);
			
			MySoftReference<ADDNode> refa = new MySoftReference<ADDNode>(a);
			MySoftReference<ADDNode> refb = new MySoftReference<ADDNode>(b);

			Pair<MySoftReference<ADDNode>, MySoftReference<ADDNode>> key = new Pair<MySoftReference<ADDNode>, MySoftReference<ADDNode>>( refa, refb );
			
			if( theMap.containsKey( key ) ){
				ret = theMap.get(key).get();
			}
			
		}
		
		return ret;
				
	}

	public ADDNode lookupApplyCache( ADDNode a, ADDNode b, DDOper op ){
		
		ADDNode ret = null;
		
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

		
		
	}

}
