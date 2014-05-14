package add;

import graph.Graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections4.map.ReferenceMap;

import util.InternedArrayList;
import util.Pair;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dd.DDManager;
import dd.DDManager.DDQuantify;


public class ADDManager implements DDManager<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

//	private static FileHandler allHandler;
//	private static final String blanky = "".intern();
	private static final double BytesToMB = 1024*1024;
	private static final boolean LOGGING_ON = false;
//	private final static ConsoleHandler consoleHandler = new ConsoleHandler();
	//all nodes - soft references - cache has to be cleaned up from reference queue
	//keep nodes around as long as possible
	//don't do this since ADDRNode might have a bad hash performance
	//	protected Map< Integer, MySoftReference< ADDRNode > > madeNodes 
	//		= new ConcurrentHashMap< Integer, MySoftReference< ADDRNode > >();
//	private final static Logger LOGGER = Logger.getLogger(ADDManager.class.getName());addadd
//	private static final long APPLY_CACHE_SIZE = (long)1e4;
	private static final boolean EXHAUSTIVE_CACHING = false;
	protected long STORE_INCREMENT;
	private static final long leaf_cache_max_size = 100000;
	
	
//	private static final CacheBuilderSpec  temp_unary_cache_spec = 
//			CacheBuilderSpec.parse("concurrencyLevel=1");
	
//	Cache< ADDRNode, ADDRNode > _tempUnaryCache 
//	= CacheBuilder.from( temp_unary_cache_spec ).build();
	
	private static final CacheBuilderSpec  made_leaf_cache_spec = 
		CacheBuilderSpec
			.parse("concurrencyLevel=1,softValues" );
	private static final CacheBuilderSpec  made_inode_cache_spec = 
			CacheBuilderSpec
				.parse("concurrencyLevel=1,softValues");
//	private static final CacheBuilderSpec  apply_cache_spec = 
//			CacheBuilderSpec
//				.parse("concurrencyLevel=1, maximumSize = " +
//						APPLY_CACHE_SIZE + ", expireAfterAccess=2m");
		
//	static{
//		try {
//			allHandler = new FileHandler("./log/" + ADDManager.class.getName());
//			LOGGER.addHandler(allHandler);
//		} catch (SecurityException | IOException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//	}

	public static DDOper getCompliment( final DDOper op ){

//		Objects.requireNonNull(op, "DDOper was null" );
		
		DDOper ret = null;

		switch( op ){
		case ARITH_DIV: ret = DDOper.ARITH_PROD; break;
		case ARITH_PROD: ret = DDOper.ARITH_DIV; break;
		case ARITH_MINUS: ret = DDOper.ARITH_PLUS; break;
		case ARITH_PLUS: ret = DDOper.ARITH_MINUS; break;
		case ARITH_MAX: ret = DDOper.ARITH_MIN; break;
		case ARITH_MIN : ret = DDOper.ARITH_MAX; break;
		default : System.err.println("unknown operator in getCompliment");
		System.exit(1);
		}

		return ret;

	}
	
	public ADDRNode doApricodd( final ADDRNode input, 
			final boolean do_apricodd, 
			final double apricodd_epsilon, final APPROX_TYPE apricodd_type ){
		
		if( !do_apricodd ){
			return input;
		}
		//traverse top-down and compress any internal node within epsilon
		//using the bounds stored in the internal node
		
		//fix intervals to be consistent
		//may not give more compression necessarily
		//fix : interval contained in another interval - take bigger one
		//two intervals whose combined span is <= epsilon
				
		//collect the leaves
		//sort by starting point
		//for each leaf
			//see if can be merged with next leaf
			//maintain map of remap
		//finally, remap leaves in DD
//		final double actual_epsilon = apricodd_epsilon/2.0d;
		
		SortedSet<ADDLeaf> leaves = getLeaves(input);
		//already sorted
		Map<ADDLeaf,ADDLeaf> remaps = mergeLeaves( leaves, apricodd_epsilon, 
				apricodd_type );
		return remapLeaves( input, remaps );
		
//		final Map<ADDRNode, ADDRNode> mergedNodes = new HashMap<ADDRNode,ADDRNode>();
//		traverseAndMerge( input, apricodd_epsilon, apricodd_type, mergedNodes );
//		final Map<ADDLeaf,ADDLeaf> finalDest = fixIntervals( mergedNodes, apricodd_epsilon, apricodd_type );

//		return remapNodes( input, mergedNodes, finalDest );
	}
	
	
	
	private Map<ADDLeaf, ADDLeaf> mergeLeaves( final SortedSet<ADDLeaf> leaves,
			final double apricodd_epsilon,
			final APPROX_TYPE appricodd_type ){
		
		final Map<ADDLeaf, ADDLeaf> ret = new HashMap<ADDLeaf, ADDLeaf>();
		final SortedSet<ADDLeaf> mapped_set = new TreeSet<ADDLeaf>();
		
		ADDLeaf cur = null;
		for( final ADDLeaf next : leaves ){
			if( cur == null ){
				cur = next;
				mapped_set.add( cur );
				continue;
			}
			
			if( combinedSpan(cur, next) <= apricodd_epsilon ){
//				mapped_set.add( cur );
				mapped_set.add( next );
//				cur = map_to;
			}else{
				final ADDLeaf map_to = mergeLeavesInt( mapped_set, appricodd_type );
				for( final ADDLeaf src : mapped_set ){
					ret.put( src, map_to );
				}
				mapped_set.clear();
				cur = next;
				mapped_set.add( cur );
			}
		}
		if( !mapped_set.isEmpty() ){
			final ADDLeaf map_to = mergeLeavesInt( mapped_set, appricodd_type );
			for( final ADDLeaf src : mapped_set ){
				ret.put( src, map_to );
			}
			mapped_set.clear();
		}
		return ret;
	}

	private ADDLeaf mergeLeavesInt(final SortedSet<ADDLeaf> mapped_set, 
			final APPROX_TYPE apricodd_type) {
		switch( apricodd_type ){
		case LOWER : return mapped_set.first();
		case UPPER : return mapped_set.last();
//		case RANGE : return (ADDLeaf) getLeaf( mapped_set.first().getMin(), mapped_set.last().getMax() ).getNode();
		case AVERAGE : ADDLeaf avg = (ADDLeaf) DD_ZERO.getNode();
			for( final ADDLeaf in : mapped_set ){
				avg = (ADDLeaf) applyLeafOp( avg, in, DDOper.ARITH_PLUS ).getNode();
			}
			avg = (ADDLeaf) applyLeafOp(avg, (ADDLeaf) 
					getLeaf((double)mapped_set.size()).getNode(), DDOper.ARITH_DIV ).getNode();
			return avg;
		}
		return null;
	}

	private ADDRNode remapLeaves( final ADDRNode input, 
			final Map<ADDLeaf, ADDLeaf> remaps ){
		Map< ADDRNode, ADDRNode > _tempUnaryCache  = new HashMap< ADDRNode, ADDRNode>();
		final ADDRNode replaced = remapLeavesInt( input, remaps, _tempUnaryCache );
		_tempUnaryCache = null;
		return replaced;
	}
	
	private ADDRNode remapLeavesInt( final ADDRNode input,
			final Map<ADDLeaf, ADDLeaf> remaps,
			final Map<ADDRNode, ADDRNode> _tempUnaryCache) {
		
		final ADDNode theNode = input.getNode();
		if( theNode instanceof ADDLeaf ){
			final ADDLeaf new_leaf = remaps.get((ADDLeaf)theNode);
			if( new_leaf == null ){
				try{
					throw new Exception("Remap did not have a mapping ");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			return getRNode( ADDLeaf.class, true );
		}else{
			
			final ADDRNode lookup = _tempUnaryCache.get( input );
			if( lookup != null ){
				return lookup;
			}
			
			final ADDRNode input_true = input.getTrueChild();
			final ADDRNode merged_true = remapLeavesInt(input_true, remaps, _tempUnaryCache);
			final ADDRNode input_false = input.getFalseChild();
			final ADDRNode merged_false = remapLeavesInt(input_false, remaps, _tempUnaryCache);
			final ADDRNode ret = getINode(input.getTestVariable(), merged_true, merged_false);
			_tempUnaryCache.put( input, ret );
			return ret;
		}
	}

	private ADDRNode remapNodes(final ADDRNode input,
			final Map<ADDRNode, ADDRNode> mergedNodes, final Map<ADDLeaf, ADDLeaf> finalDest) {
		//always remaps to a leaf
		//or recurse
//		for( final Entry<ADDRNode, ADDRNode> entry : mergedNodes.entrySet() ){
//			showGraph( entry.getKey(), entry.getValue() );
//		}
		
		final ADDRNode mergedTo = mergedNodes.get( input );
		if( mergedTo == null ){
			//recurse
			final ADDNode theNode = input.getNode();
			if( theNode instanceof ADDLeaf ){
				return input;
			}else{
				final ADDRNode input_true = input.getTrueChild();
				final ADDRNode merged_true = remapNodes(input_true, mergedNodes, finalDest);
				final ADDRNode input_false = input.getFalseChild();
				final ADDRNode merged_false = remapNodes(input_false, mergedNodes, finalDest);
				return getINode(input.getTestVariable(), merged_true, merged_false);
			}
		}else{
			final ADDLeaf leafNode = (ADDLeaf)mergedTo.getNode();
			final ADDLeaf finalLeaf = (finalDest == null ) ? leafNode : finalDest.get(leafNode);
			return getRNode( ADDLeaf.class, false);
		}
	}

//	private Map<ADDLeaf, ADDLeaf> fixIntervals(
//			final Map<ADDRNode, ADDRNode> mergedNodes,
//			final double apricodd_epsilon,
//			final APPROX_TYPE apricodd_type ) {
//		final TreeSet<ADDLeaf> leaves = new TreeSet<ADDLeaf>();
//		final Collection<ADDRNode> leafNodes = mergedNodes.values();
//		for( ADDRNode leafNode : leafNodes ){
//			final ADDLeaf theLeaf = (ADDLeaf)leafNode.getNode();
//			leaves.add(theLeaf);
//		}
//		final Map<ADDLeaf,ADDLeaf> fixedIntervals = new HashMap<ADDLeaf, ADDLeaf>();
//		ADDLeaf cur = null;
//		for( final ADDLeaf sortedLeaf : leaves ){
//			if( cur == null ){
//				cur = sortedLeaf;
//				fixedIntervals.put( cur ,cur );
//				continue;
//			}
//			if( combinedSpan( mapped_,sortedLeaf) <= apricodd_epsilon ){
//				final ADDLeaf newLeaf = mergeLeafPair( cur, sortedLeaf, apricodd_type );
//				fixedIntervals.put( cur, newLeaf );
//				fixedIntervals.put( sortedLeaf, newLeaf );
//				cur = newLeaf;
//			}else{
//				fixedIntervals.put( sortedLeaf, sortedLeaf );
//				cur = sortedLeaf;
//			}
//		}
//		return fixedIntervals;
//	}
	
	private ADDLeaf mergeLeafPair(final ADDLeaf leaf1, final ADDLeaf leaf2,
			final APPROX_TYPE apricodd_type) {
		final double min = Math.min( leaf1.getMin(), leaf2.getMin() );
		final double max = Math.max( leaf1.getMax(), leaf2.getMax() );
		switch( apricodd_type ){
//			case RANGE : return (ADDLeaf) getLeaf(min, max ).getNode();
			case AVERAGE : return (ADDLeaf) getLeaf( 0.5d*(min+max) ).getNode();
			case LOWER : return (ADDLeaf) getLeaf( min ).getNode();
			case UPPER : return (ADDLeaf) getLeaf( max ).getNode();
			default : try{
				throw new Exception("Bad apricodd type ");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return null;
	}

	private double combinedSpan( final ADDLeaf leaf1, final ADDLeaf leaf2 ){
		final double min_min = ( Math.min( leaf1.getMin(), leaf2.getMin() ) );
		final double max_max = ( Math.max( leaf1.getMax(), leaf2.getMax() ) );
		return Math.abs( max_max - min_min );
	}

	private void traverseAndMerge(final ADDRNode input, final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type, 
			final Map<ADDRNode, ADDRNode> mergedNodes) {
		
		Map<ADDRNode,Boolean> seen = new HashMap<ADDRNode, Boolean>();
		
		traverseAndMergeInt( input, apricodd_epsilon, apricodd_type, mergedNodes,
				seen );
		seen.clear();
		seen = null;

		return;
	}

	private void traverseAndMergeInt(final ADDRNode input, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type,
			final Map<ADDRNode, ADDRNode> mergedNodes,
			final Map<ADDRNode, Boolean> seen) {
		
		Boolean visited = seen.get(input);
		if( visited != null){
			return;
		}
		
		final ADDNode theNode = input.getNode();
		//if here, node not been merged before
		//else would be in cache too.
		
		if( theNode instanceof ADDLeaf ){
			mergedNodes.put( input, input );
			return;
		}else if( theNode instanceof ADDINode ){
			ADDINode inode = (ADDINode)theNode;
			final double max = inode.getMax();
			final double min = inode.getMin();
			if( Math.abs(max-min) <= apricodd_epsilon ){
				ADDRNode merged = null;
				switch( apricodd_type ){
//				case RANGE : merged = getLeaf(min, max); break;
				case AVERAGE : merged = getLeaf(0.5d*(min+max) );
				break;
				case LOWER : merged = getLeaf(min); break;
				case UPPER : merged = getLeaf(max); break;
				default : try{
								throw new Exception("bad apricodd type");
						  }catch(Exception e ){
					            e.printStackTrace();
					            System.exit(1);
						  }
				}
				mergedNodes.put( input, merged );
			}else{
				traverseAndMergeInt(input.getTrueChild(), 
						apricodd_epsilon, apricodd_type, 
						mergedNodes, seen );
				traverseAndMergeInt( input.getFalseChild(), apricodd_epsilon, 
						apricodd_type, mergedNodes, seen);
			}
			seen.put( input, true );
		}
	}
	
	public static void testApricodd(){
		final ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		final ADDManager man = new ADDManager(100, 100, ord, 42);
		final ADDRNode inodeA = man.getINode("A",  man.getLeaf(1d), man.getLeaf(1.2d) );
		final ADDRNode inodeB = man.getINode("B",  man.getLeaf(3d), man.getLeaf(3.4d) );
		
		final ADDRNode inodeC = man.getINode("C", inodeA, inodeB);
	
//		man.showGraph(inodeA, man.doApricodd(inodeA, 0.2d, APPROX_TYPE.RANGE ) );
//		
//		man.showGraph(inodeC, man.doApricodd(inodeC, 0.2d, APPROX_TYPE.RANGE ) );
//		
//		man.showGraph(inodeC, man.doApricodd(inodeC, 0.4d, APPROX_TYPE.RANGE ) );
		
		man.showGraph(inodeC);
		
		man.showGraph( man.doApricodd(inodeC, true, 0.2d, APPROX_TYPE.LOWER ) );
		
		
	}

	public static boolean isCommutative(final DDOper op){
//		Objects.requireNonNull(op, "DDOper null" );
		if( op.equals(DDOper.ARITH_DIV) || op.equals(DDOper.ARITH_MINUS) ){
			return false;
		}

		return true;

	}

	public static void main(String[] args) throws Exception {
//	    testGetProductDD( );
//	    testAssignDD();
	    testGetProductFromAssign();
//		testApricodd();
//		testBreakTies();
		//		testAddPair();
		//		testgetINode();
		//		testIndicators();
		//		testApplyLeafOp();
//				testGetRNode();
//		testBreakTies();
		//		testGraph();
		//		testClearDeadNodes((int)1e5);
//		testApply();
//		testConstrain();
		//		testRestrict();
//		testEnumeratePaths();
//		testPathsToLeaf();
		//		testEvaluate();
//				testApplyCache();
		//				testGetLeaves();
//		testGetNodes();
//		testRemapVars();
//		testMarginalize();

		//		testGetLeaf();
//				ADDRNode diag = getBernoulliProb(20, 0.75);
				
		//		testCountNodes();
		//		testCountPaths();
//				testBernoulliConstraints(20, 0.75, true, 5);

		//		double[] probs = {0.5, 0.6, 0.7, 0.8};
		//		double[] constr_probs = {0.5, 0.6, 0.7, 0.8};
		//		ArrayList<String> results = new ArrayList<String>();
		//		int[] nvars = {35};
		//		
		//		for( int nv : nvars ){
		//			for( double prob : probs ){
		//				
		//				for( double constr_prob : constr_probs ){
		//					results.add( testRandomizedConstraints(nv, prob, constr_prob ));		
		//			
		//					System.out.println( prob + " " + constr_prob );
		//					
		//					try {
		//						Thread.sleep(10000);
		//					} catch (InterruptedException e) {
		//					}
		//					
		//				}
		//		}	
		//		}
		//		
		//		System.out.println( results );

	}

	private static void testGetProductFromAssign() {
	    ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);
		
		ADDRNode leaf1 = man.getLeaf(3d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X",  
			man.getINode("Z", leaf1, leaf2), 
			man.getINode("Y", leaf1, leaf2) );
		
		NavigableMap<String, Boolean> assign = Maps.newTreeMap();
		assign.put("Y", true);
		assign.put("X", false);
		
		man.showGraph( man.getProductBDDFromAssignment(assign) );
		
	}

	private static void testAssignDD() {
	    
	    ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);
		
		ADDRNode leaf1 = man.getLeaf(3d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X",  
			man.getINode("Z", leaf1, leaf2), 
			man.getINode("Y", leaf1, leaf2) );
		
		NavigableMap<String, Boolean> assign = Maps.newTreeMap();
		assign.put("X", true);
		assign.put("Y", false);
		
		man.showGraph( inode, man.assign(inode, assign , 6d) );
		
		

	}

	private static void testGetProductDD() {
	    
	    ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		NavigableMap<String, Boolean> assign = Maps.newTreeMap();
		assign.put("X", true);
		assign.put("Y", false);
		
		man.showGraph( man.getProductBDDFromAssignment(assign) );
	}
	
	

	public static void testAddPair(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(3d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		System.out.println( man.applyCache );

		System.out.println( man.applyHit );

		inode2 = man.getINode("Y", inode, man.DD_ZERO);

		System.out.println( man.applyCache );

		System.out.println( man.applyHit );

		man.showGraph(inode, inode2);


	}

	public static void testApply(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode ind1 = man.getIndicatorDiagram("A", true);

		ADDRNode inodeA = man.getINode("A",  man.getLeaf( 1d), man.getLeaf(2d) );

		ADDRNode inodeB = man.getINode("B",  man.getLeaf( 3d), man.getLeaf(4d) );
		//
		//		ADDRNode inodeC = man.getINode("C",  man.getLeaf(9d, 10d), man.getLeaf(11d, 12d) );
		//		
		//		ADDRNode inodeD = man.getINode("D",  man.getLeaf(13d, 14d), man.getLeaf(15d, 16d) );
		//		
		//		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode prod = man.apply(inodeA, inodeB, DDOper.ARITH_PROD);

		ADDRNode indC = man.getIndicatorDiagram("C", true);

		ADDRNode prod2 = man.apply( prod, indC, DDOper.ARITH_PROD );
		
		System.out.println( inodeA.toString() );
		System.out.println( inodeB.toString() );
		System.out.println( prod.toString() );
		

//		man.showGraph(prod, indC, prod2);

//		man.showGraph( man.apply( inodeA, inodeB, DDOper.ARITH_MINUS) );
	}

	public static void testApplyLeafOp(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord , 42);

		ADDRNode leaf1 = man.getLeaf(2d);

		ADDRNode leaf2 = man.getLeaf(4d);

		ADDRNode add = man.applyLeafOp( leaf1, 
				leaf2, DDOper.ARITH_PLUS);


		ADDRNode minus = man.applyLeafOp( leaf1, 
				leaf2, DDOper.ARITH_MINUS);

		ADDRNode prod = man.applyLeafOp( leaf1, 
				leaf2, DDOper.ARITH_PROD);

		ADDRNode div = man.applyLeafOp( leaf1, 
				leaf2, DDOper.ARITH_DIV);

		man.showGraph(leaf1, leaf2, add, minus, prod, div);


		//w00t!

	}

//	public static void testBernoulliConstraints(int nvars, double prob, 
//			boolean disjoint, int num_disjoint ) {
//
//		ArrayList<String> ord = new ArrayList<String>();
//
//		for( int i = 0 ; i < nvars; ++i ){
//			ord.add("X"+i);
//		}
//
//		if( disjoint ){
//			for( int i = 0 ; i < num_disjoint ; ++i ){
//				ord.add("X"+(nvars+i));	
//			}
//		}
//
//
//		ADDManager man = new ADDManager(100, 100, ord);
//
//		ADDRNode diag = man.getBernoulliProb(nvars, 0.75);
//
//		man.showGraph(diag);
//		
//		ADDRNode constr = man.DD_ONE;
//
//		int j = ( disjoint ) ? nvars : 0;
//
//		for( int i = 0 ; i < num_disjoint ; ++i ){
//			ADDRNode thisOne = man.getIndicatorDiagram("X"+(j+i), true );
//			//			man.showGraph(thisOne);
//			constr = man.apply(constr, thisOne, DDOper.ARITH_PROD);
//			//			man.showGraph(constr);
//		}
//
//		System.out.println( man.countNodes(diag, constr) );
//
////		man.showGraph(diag, constr);
//
//		Timer conTime = new Timer();
//
//		ADDRNode coned = man.constrain(diag, constr, man.DD_ZERO);
//
//		double cTime = conTime.GetTimeSoFarAndResetInMinutes();
//
//		man.clearDeadNodes(true);
//
//		conTime.ResetTimer();
//
//		ADDRNode multd = man.apply(diag, constr, DDOper.ARITH_PROD );
//
//		double multTime = conTime.GetTimeSoFarAndResetInMinutes();
//
//		man.clearDeadNodes(true);
//
//		System.out.println( man.countNodes(diag, coned, multd) + " " + cTime + " " + multTime );
//
//		//		man.showGraph( coned, multd );
//
//
//	}

	//	testApplyLeafOp
	//	testWhichBefore
	//	testgetInode

//	public static void testClearDeadNodes(int num_nodes ){
//
//		ArrayList<String> ord = new ArrayList<String>();
//		for( char ch = 'A'; ch <= 'Z'; ++ch ){
//			ord.add(ch+"");
//			ord.add( Character.toLowerCase(ch) + "" );
//		}
//		System.out.println( ord );
//		ADDManager man = new ADDManager(100, 100, ord);
//		ADDRNode old_leaf = man.getLeaf(0.0d , 0.0d);
//		ADDRNode old_inode = null;
//
//		int i = 0;
//		ArrayList<ADDRNode> leaf_list = new ArrayList<ADDRNode>();
//		ArrayList<ADDRNode> inode_list = new ArrayList<ADDRNode>();
//		for( String chr : ord ){
//			ADDRNode leaf = man.getLeaf(i+1.32, i+5.34);
//			ADDRNode inode = null;
//			if( old_inode == null ){
//				inode = man.getINode(chr+"", leaf, old_leaf);	
//			}else{
//				inode = man.getINode(chr+"", leaf, old_inode);
//			}
//			leaf_list.add(leaf);
//			man.showGraph(leaf, inode );
//			if( old_inode != null ){
//				man.showGraph(old_inode);
//			}
//
//			inode_list.add(inode);
//			double memory = man.getMemoryPercent();
//			man.memorySummary();
//			man.makeSureNoNull();
//			if( memory > 0.8 ){
//				break;
//			}
//
//			old_inode = inode;
//			++i;
//			System.out.println(chr);
//		}
//		System.out.println(leaf_list);
//		man.memorySummary();
//		man.clearDeadNodes(true);
//		man.memorySummary();
//		int deleted = 0;
//
//		for( ADDRNode rnode : leaf_list ){
//			if( man.nullify(rnode) ){
//				++deleted;
//			}
//		}
//
//		for( ADDRNode rnode : inode_list ){
//			if( man.nullify(rnode) ){
//				++deleted;
//			}
//		}
//
//		System.err.println( "deleted through nullify " + deleted );
//		try {
//			System.gc();
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		man.clearDeadNodes(true);
//
//		man.memorySummary();
//
//		man.memorySummary();
//
//	}

	public static void testConstrain() {

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5), man.getLeaf(4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d), man.getLeaf(7d) );

		//		man.showGraph(inodeB);

		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode constr = man.getIndicatorDiagram("C", true);

		ADDRNode constrained = man.constrain(inodeAB, constr, man.DD_ZERO);

		man.showGraph(inodeA, inodeB, inodeAB, constr);

		man.showGraph( constrained );

		ADDRNode mult = man.apply( inodeAB, constr, DDOper.ARITH_PROD);

		man.showGraph( mult );

		ADDRNode con2 = man.constrain( constrained, man.getIndicatorDiagram("A", false), man.DD_ZERO);

		man.showGraph(con2);

	}

	public static void testCountNodes(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(10100, 10000, ord, 42);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5), man.getLeaf(4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d), man.getLeaf(7d) );

		//		man.showGraph(inodeB);

		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode inodeABC = man.apply(inodeAB, man.getIndicatorDiagram("C", false), DDOper.ARITH_PROD);

		man.showGraph( inodeA, inodeB, inodeAB, inodeABC );

		System.out.println( man.countNodes(inodeAB, inodeA, inodeB, inodeABC) );

	}

	public static void testGetNodes(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(5d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph(inode2);

		System.out.println( man.getNodes(inode2, true) );

	}

	public static void testCountPaths() {

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5), man.getLeaf(4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d), man.getLeaf(7d) );

		//		man.showGraph(inodeB);

		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode inodeABC = man.apply(inodeAB, man.getIndicatorDiagram("C", false), DDOper.ARITH_PROD);

		man.showGraph( inodeA, inodeB, inodeAB, inodeABC );

//		System.out.println( man.countPaths( inodeABC ) );

	}

	public static void testgetINode(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(2d);

		ADDRNode leaf2 = man.getLeaf(3d);

		ADDRNode inode1 = man.getINode("X", leaf1 , leaf2);

		ADDRNode inode2 = man.getINode("Y", inode1, leaf2);

		man.showGraph(inode1, inode2);

		ADDRNode inode3 = man.getINode("X", leaf2, leaf1 );

		man.showGraph(inode3);

		ADDRNode inode4 = man.getINode("Y", inode1, inode3);

		man.showGraph(inode4);

		ADDRNode inode5 = man.getINode("Z", inode1, inode1);

		man.showGraph(inode5);

		ADDRNode inode6 = man.getINode("Y", leaf2,  man.getINode("X", leaf2, leaf2) );

		man.showGraph(inode6);

	}

	public static void testGetLeaf(){

		//		ArrayList<Double> l1 = new ArrayList<Double>();
		//		
		//		l1.add(2d);
		//		l1.add(2d);
		//		
		//		System.out.println( l1.hashCode() );
		//		
		//		ArrayList<Double> l2 = new ArrayList<Double>();
		//		
		//		l2.add(0d);
		//		l2.add(0d);
		//		
		//		System.out.println( l2.hashCode() );


		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(2d);

		ADDRNode leaf2 = man.getLeaf(7d);

		man.showGraph(leaf1, leaf2);

	}

	public static void testGetRNode() throws Exception{

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf = man.getLeaf(5.34);

		ADDRNode leaf2 = man.getLeaf( 4.3 );

		System.out.println(leaf);

		System.out.println(leaf2);

		ADDRNode inod1 = man.getINode("X", leaf, leaf2);

		System.out.println(inod1);

		ADDRNode inod1_copy = man.getINode("X", leaf, leaf2);

		System.out.println(inod1_copy);
		
		ADDRNode inod1_copy_2 = man.makeINode("X", leaf, leaf2 );

		System.out.println( inod1_copy_2 );
		
		ADDRNode leaf3 = man.getLeaf( 6.5 );

		System.out.println(leaf3);

		ADDRNode inod3 = man.getINode("X", leaf, leaf3);

		System.out.println(inod3);

//		man.memorySummary();

		ADDRNode aleaf = man.getINode("X", leaf, leaf);

		System.out.println(aleaf);

//		man.memorySummary();

		ADDRNode inod4 = man.getINode("X", leaf3, leaf);

		System.out.println(inod4);
		
		ADDRNode leaf_copy = man.getLeaf(1.32);
		
		System.out.println( leaf_copy );
		
		System.out.println("reduction test");
		ADDRNode red = man.getINode("X", leaf3, man.getINode("Y", leaf3, leaf3) );
		ADDRNode red2 = man.getINode("X", man.getINode("Y", leaf3, leaf3), man.getINode("Y", leaf3, leaf3) );
		ADDRNode red3 = man.getINode("X", man.getINode("X", leaf3, leaf2), leaf3 );
		
		System.out.println( red.toString() );
		System.out.println( red2.toString() );
		System.out.println( red3.toString() );

//		man.memorySummary();

//		man.showGraph(leaf, leaf_copy );
//		man.showGraph(inod1_copy, inod1, inod3, inod4);
		
		/////////
//		ADDINode in1 = new ADDINode().getNullDD();
//		in1.plugIn("X", man.DD_ZERO, man.DD_ONE);
//		
//		ADDINode in2 = new ADDINode().getNullDD();
//		in2.plugIn("X", man.DD_ONE, man.DD_ZERO);
//		
//		ADDRNode n1 = new ADDRNode(in1);
//		ADDRNode n2 = new ADDRNode(in2);
////		n2.setNegated(true);
//		
//		System.out.println( n1.equals(n2) );
//		man.showGraph(n1, n2);
	}

	public static void testGraph(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		//have to test this again with ordering
		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf = man.getLeaf(5.34);

		//		man.showGraph(leaf);

		ADDRNode leaf2 = man.getLeaf( 6.5 );

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

	public static void testIndicators(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode a = man.getIndicatorDiagram("X", true);

		ADDRNode b = man.getIndicatorDiagram("X", false);

		man.showGraph(a,b);

	}

//	public static String testRandomizedConstraints( int nvars, double prob, double constr_prob ){
//
//		ArrayList<String> ord = new ArrayList<String>();
//
//		for( int i = 0 ; i < nvars; ++i ){
//			ord.add("X"+i);
//		}
//
//		ADDManager man = new ADDManager(100, 100, ord);
//
//		ADDRNode diag = man.DD_ONE, constr = man.DD_ONE;
//
//		Random rand = new Random( );
//
//
//		for( int i = 0 ; i < nvars; ++i ){
//
//			double r = rand.nextDouble();
//
//			if( r < constr_prob ){
//				ADDRNode thisOne = man.getIndicatorDiagram("X"+i, true);
//				constr = man.apply( thisOne, constr, DDOper.ARITH_PROD);
//			}
//
//			r = rand.nextDouble();
//
//			if( r < prob ){
//				double v = rand.nextDouble();
//
//				ADDRNode inode = man.getINode("X"+i, man.getLeaf(v, v), man.getLeaf(1-v, 1-v) );
//				diag = man.apply( diag, inode, DDOper.ARITH_PROD );
//			}
//
//		}
//
//		man.flushCaches(true);
//
//		Timer conTime = new Timer();
//
//		ADDRNode coned = man.constrain(diag, constr, man.DD_ZERO);
//
//		double cTime = conTime.GetTimeSoFarAndResetInMinutes();
//
//		man.flushCaches(true);
//
//		conTime.ResetTimer();
//
//		ADDRNode multd = man.apply(diag, constr, DDOper.ARITH_PROD );
//
//		double multTime = conTime.GetTimeSoFarAndResetInMinutes();
//
//		man.flushCaches(true);
//
//		String str =  prob + " " + constr_prob + " " + man.countNodes(diag, constr) + " " +  
//				man.countNodes(coned, multd) + " " + cTime + " " + multTime ;
//
//		System.out.println( str );
//
//		return str; 
//
//	}

	public static void testRestrict(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf = man.getLeaf( 5.34);

		ADDRNode leaf2 = man.getLeaf( 4.3 );

		ADDRNode inod1 = man.getINode("X", leaf, leaf2);

		ADDRNode leaf3 = man.getLeaf( 6.5 );

		ADDRNode inod3 = man.getINode("X", leaf, leaf3);

		man.showGraph(inod1, inod3);

		ADDRNode rest1 = man.restrict(inod1, "X", true);

		ADDRNode rest2 = man.restrict(inod1, "X", false);

		man.showGraph(rest1, rest2);

		ADDRNode inod4 = man.getINode("K", inod1, leaf3);

		man.showGraph(inod4, man.restrict(inod4, "K", true), man.restrict(inod4, "K", false) );

	}

	protected InternedArrayList< String > _ordering = null;//new InternedArrayList<String>();
	
	protected Runtime _runtime = Runtime.getRuntime();

	//caches can be in terms of RNodes
	protected EnumMap< DDOper, Table< ADDRNode, ADDRNode , ADDRNode > >
			applyCache = new EnumMap< 
				DDOper, Table< ADDRNode, ADDRNode , ADDRNode > >( DDOper.class );
	private int applyHit = 0;
	public static ADDRNode DD_ZERO;
	public static ADDRNode DD_ONE;
	public static ADDRNode DD_NEG_INF;

	protected Map< String, ReferenceMap< ADDINode, ADDRNode > > 
		madeINodes = new TreeMap< String, 
		ReferenceMap< ADDINode, ADDRNode> >();

	protected ReferenceMap< ADDLeaf, ADDRNode > madeLeaf
		= new ReferenceMap<ADDLeaf, ADDRNode>();
//	= CacheBuilder.from( made_leaf_cache_spec ).recordStats().
//			removalListener( new RemovalListener<ADDLeaf, ADDRNode>( ) {
//				@Override
//				public void onRemoval(
//						RemovalNotification<ADDLeaf, ADDRNode> arg) {
//					if( arg != null ){
//						ADDLeaf leaf = arg.getKey();
//						leaf = null;
//					}
//				}
//			}).build();
	
	//never deleted
//	protected Set< ADDRNode > permenantNodes = Collections.newSetFromMap( 
//			new ConcurrentHashMap< ADDRNode, Boolean >() );

//	protected ConcurrentHashMap< String,
//		Map< ADDINode, ADDRNode > > 
//			permanentMadeINodes = new ConcurrentHashMap< String, 
//				Map< ADDINode, ADDRNode> >();
	
	//store of null nodes
	protected Queue< ADDINode > storeINodes = null;
	protected Queue< ADDLeaf > storeLeaf  = null;
	
	protected Multiset< ADDRNode > permanentNodes 
		= HashMultiset.create();
//	private Random _rand;
	private ADDLeaf _temp_null_leaf;
	private ADDINode _temp_null_inode ;
	
	public ADDManager( final long managerStoreInitSize, final long managerStoreIncrSize, 
			final ArrayList<String> ordering, final long seed ){
//		Objects.requireNonNull(ordering, "ordering is null" );
		createStore(managerStoreInitSize);
//		addToStore( managerStoreInitSize, true, true );
		STORE_INCREMENT = managerStoreIncrSize;
		_temp_null_leaf = (ADDLeaf) getOneNullDD(true);
		_temp_null_inode = (ADDINode) getOneNullDD(false);
		 
		DD_ZERO = getLeaf(0.0d);
		DD_ONE = getLeaf(1.0d);
		DD_NEG_INF = getLeaf( getNegativeInfValue());
		_ordering = new InternedArrayList<String>();//( ordering );
		addPermenant(DD_ZERO, DD_ONE, DD_NEG_INF);
		for( final String var : ordering ){
			_ordering.add( var.intern() );
		}
		
		for( final String var : ordering ){
			addPermenant( getIndicatorDiagram(var, true) );
			addPermenant( getIndicatorDiagram(var, false) );
		}
//		_rand = new Random(seed);
	}

	private synchronized void addPair( final ADDRNode op1, final ADDRNode op2, 
			final DDOper op, final ADDRNode res ){
			
//			Objects.requireNonNull(op1, "addpair null");
//			Objects.requireNonNull(op2, "addpair null");
//			Objects.requireNonNull(res, "addpair null");
//			Objects.requireNonNull(op, "op null");
			
			if( applyCache.get(op) == null ){
				final Table< ADDRNode, ADDRNode, ADDRNode > inner_cache 
					= HashBasedTable.create();
//					CacheBuilder.from( apply_cache_spec ).recordStats().build();
				applyCache.put(op, inner_cache);
			}

			Table<ADDRNode, ADDRNode, ADDRNode> inner_cache =
					applyCache.get(op);
			inner_cache.put( op1, op2, res );
	}

	public static void testApplyCache(){
		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(5d);

		ADDRNode leaf2 = man.getLeaf(2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

//		System.out.println( man.applyCache );

		System.out.println( man.applyHit );

		ADDRNode inode3 = man.getINode("Z", leaf2, leaf1 );
		
		ADDRNode c = man.apply( inode2, inode3, DDOper.ARITH_PLUS );
		
		System.out.println( man.applyHit );

		ADDRNode c2 = man.apply( inode3, inode2, DDOper.ARITH_PLUS );
		
		System.out.println( man.applyHit );

		ADDRNode b = man.apply( c, inode3, DDOper.ARITH_MINUS );
		
		System.out.println( man.applyHit );

		ADDRNode a = man.apply( c, inode2, DDOper.ARITH_MINUS );
		
		System.out.println( man.applyHit );

		man.showGraph( inode2, inode3, c, c2, b, a );
		
	}

	@Override
	public synchronized void addPermenant(final ADDRNode... input) {
//		Objects.requireNonNull( input );
		for( ADDRNode rnode : input ){
			cacheNode( rnode );
		}
	}
	
	private void cacheNode(final ADDRNode input) {
		final Set<ADDRNode> nodes = getNodes( input , true);
		for( final ADDRNode rnode : nodes ){
			permanentNodes.add( rnode );
//				return; 
//			}
//			
//			final String test_var = rnode.getTestVariable();
//			Map<ADDINode, ADDRNode> inner = permanentMadeINodes.get( test_var );
//			if( inner == null ){
//				inner = new HashMap< ADDINode, ADDRNode >();
//				CacheBuilder.newBuilder().recordStats().
//						removalListener( new RemovalListener<ADDINode, ADDRNode>() {
//							@Override
//							public void onRemoval(
//									RemovalNotification<ADDINode, ADDRNode> arg0) {
//								try{
//									throw new Exception("perm. inode removed " + arg0.toString() );
//								}catch( Exception e ){
//									e.printStackTrace();
//									System.exit(1);
//								}
//							}
//						}).build();	
			}
//			inner.put( (ADDINode)node, rnode );
//		}
	}
	
	//assumes input is a BDD
	//for each var in List
	//if both true and false branch have a max of 1
	//pick false branch, set true branch to zero
	public ADDRNode breakTiesInBDD( final ADDRNode input ,
			final Set<String> tiesIn,
			final boolean default_value ){
//		Objects.requireNonNull( input );
//		Objects.requireNonNull( tiesIn );
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();
		final ADDRNode ret = breakTiesInBDDInt( input, tiesIn, default_value, 
				_tempUnaryCache );
		_tempUnaryCache = null;
		return ret;
	}

	private ADDRNode breakTiesInBDDInt(ADDRNode input, Set<String> tiesIn,
			final boolean default_value, Map<ADDRNode, ADDRNode> _tempUnaryCache ) {
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			return input;
		}
		
		final String testVar = input.getTestVariable();
		final double this_max = input.getMax();
		final ADDRNode trueChild = input.getTrueChild();
		final double true_max = trueChild.getMax();
		final ADDRNode falseChild = input.getFalseChild();
		final double false_max = falseChild.getMax();
		if( this_max != 1.0d ){
			try{
				throw new Exception("No 1 leaf here");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if( this_max != true_max && this_max != false_max ){
			try{
				throw new Exception("Bounds do not match");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//2/6/2013 : Leaf child preferred for breaking ties
		
		final boolean is_tie_able = tiesIn.contains( testVar );
		if( is_tie_able ){
		
				final ADDNode trueNode = trueChild.getNode();
				final ADDNode falseNode = falseChild.getNode();
				if( trueNode instanceof ADDLeaf && !( falseNode instanceof ADDLeaf )
						&& trueChild.equals( DD_ONE ) ){
					final ADDRNode ret = getINode( testVar,  trueChild, DD_ZERO );
					return ret;//true was 1 leaf
				}else if( falseNode instanceof ADDLeaf && !(trueNode instanceof ADDLeaf)
						&& falseChild.equals( DD_ONE ) ){
					final ADDRNode ret = getINode( testVar,  DD_ZERO, falseChild );
					return ret;//false was 1 leaf
				}else if( this_max == false_max && this_max == true_max ){
					//1/20/14 : intersect true and false
					// zero out in non default
					final ADDRNode intersect = BDDIntersection(trueChild, falseChild);
					final ADDRNode intersect_not = BDDNegate( intersect );
					
					final ADDRNode true_recurse = breakTiesInBDDInt( 
							default_value ? trueChild : BDDIntersection(trueChild, intersect_not), 
									tiesIn, 
							default_value, _tempUnaryCache );
					final ADDRNode false_recurse = breakTiesInBDDInt( 
							default_value ? BDDIntersection(falseChild, intersect_not) : falseChild, 
							tiesIn, default_value, _tempUnaryCache);
					final ADDRNode ret = getINode( testVar,  true_recurse, 
							false_recurse );
					_tempUnaryCache.put( input, ret );
					return ret;//bounds matched - pick default
				}else{
					//if here - one of them must be a 0 leaf
					//recurse on the other one only
					final boolean true_zero = trueChild.equals( DD_ZERO );
					final boolean false_zero = falseChild.equals( DD_ZERO );
					if( true_zero && !false_zero ){
						final ADDRNode false_ans = breakTiesInBDDInt( falseChild, 
								tiesIn, default_value, _tempUnaryCache );
						final ADDRNode ret = getINode( testVar, DD_ZERO, false_ans );
						return ret;
					}else if( false_zero && !true_zero ){
						final ADDRNode true_ans = breakTiesInBDDInt(trueChild, 
								tiesIn, default_value, _tempUnaryCache );
						final ADDRNode ret = getINode(testVar, true_ans, DD_ZERO );
						return ret;
					}else{
						try{
							throw new Exception("incorrect state - irreduced BDD");
						}catch( Exception e ){
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
				
		}else if( !is_tie_able ){
			final ADDRNode lookup = _tempUnaryCache.get( input );
			if( lookup == null ){
				final ADDRNode true_result = breakTiesInBDDInt( trueChild, 
						tiesIn, default_value, _tempUnaryCache );
				final ADDRNode false_result = breakTiesInBDDInt( falseChild, tiesIn, 
						default_value, _tempUnaryCache );
				final ADDRNode this_result = getINode( testVar, true_result, false_result );
				_tempUnaryCache.put( input, this_result );
				return this_result;
			}
			return lookup;
		}
		return null;
	}
	
	public static void testBreakTies(){
			final ArrayList<String> ord = new ArrayList<String>();
			ord.add("X");
			ord.add("Y");
			ord.add("Z");
			ord.add("K");
			final ADDManager man = new ADDManager(100, 100, ord, 42);
			final ADDRNode z = man.getIndicatorDiagram("Z", true);
			final ADDRNode not_z = man.getIndicatorDiagram("Z", false );
			final ADDRNode y = man.getIndicatorDiagram("Y", true);

			final ADDRNode inod1 = man.getINode( "X", 
					man.getINode("Y", z, man.DD_ONE ),
					man.getINode("Y", z, man.DD_ZERO ) );
			
			man.showGraph( inod1 );
			final Set<String> tiesIn = Collections.singleton("X");
			final ADDRNode tie_true = man.breakTiesInBDD( inod1, tiesIn, true );
			final ADDRNode tie_false = man.breakTiesInBDD( inod1, tiesIn, false );
			man.showGraph( tie_true, tie_false );
			
	}

	public NavigableMap<String, Boolean> findFirstOneLeafAction(final ADDRNode input){
//		Objects.requireNonNull( input );
		ADDRNode ret = input;
		NavigableMap<String, Boolean> action = new TreeMap<String, Boolean>();
		
		while( ret.getNode() instanceof ADDINode ){
			String testVar = ret.getTestVariable();
			int max = (int) ret.getMax();
			if( max != 1 ){
				try{
					throw new Exception("No 1 leaf in input");
				}catch( Exception e ){
					e.printStackTrace();
				}
			}
			int max_true = (int) ret.getTrueChild().getMax();
			int max_false = (int) ret.getFalseChild().getMax();
			if( max_true == max ){
				ret = ret.getTrueChild();
				action.put( testVar, true );
			}else if( max_false == max ){
				ret = ret.getFalseChild();
				action.put( testVar, false );
			}else{
				try{
					throw new Exception("Max != true or false max");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		return action;
	}
	
	@Override
	public void removePermenant( final ADDRNode... input ){
//		Objects.requireNonNull( input );
		for( final ADDRNode in : input ){ 
			final Set<ADDRNode> nodes = getNodes( in,  true );
			for( final ADDRNode rnode : nodes ){
				permanentNodes.remove( rnode );
			}
//			this_node = in.getNode();
//			if( this_node instanceof ADDINode ){
//				permanentMadeINodes.get( in.getTestVariable() ).remove( (ADDINode)this_node );
//				removePermenant( in.getTrueChild() );
//				removePermenant( in.getFalseChild() );
//			}else{
//				permanentMadeLeaf.remove( (ADDLeaf)this_node );
//			}
		}
	}

	public synchronized void addToApplyCache( ADDRNode a, ADDRNode b, DDOper op, ADDRNode res ){
//		Objects.requireNonNull( new Object[]{a,b,res} );
//		Objects.requireNonNull(op);
		addPair(a,b,op,res);

		if( EXHAUSTIVE_CACHING ){
			if( isCommutative(op) ){
				addPair(b, a, op, res);
			}

			final DDOper compOp = getCompliment(op);
			if( op.equals(DDOper.ARITH_MAX) || op.equals(DDOper.ARITH_MIN) ){
				addPair(res, b, compOp, b);
				addPair(res, a, compOp, a);
			}else{

				//a+b=c => c-a=b, c-b=a, 
				//a-b=c => c+b=a, a-c=b, 
				//a*b=c, c/a=b, c/b=a
				//a/b=c => c*b=a, a/c=b

				addPair(res, b, compOp, a);
				if( isCommutative(op) ){
					addPair(res, a, compOp, b);
				}else{
					addPair(a, res, op, b);
				}
			}
			
		}
	}

	//apply cache
	//if a+b not found, look for if exists c such that c-a=b? or c-b=a?
	//if a*b = ?, exists c s.t. c/a=b? or c/b=a?
	//apply will store only +,* 
	//cache : a+b=c can also be read backawards as c-b=a 
	//Map< Map< DDNode, <-> d
	// a,b -> c
	// d,e -> c
	// want to get c,b -> a
	@Override
	public synchronized void addToStore(final long numdds, final boolean leaf, final boolean node) {

		if( leaf ){
			if( !storeLeaf.isEmpty() ){
				try{
					throw new Exception("Adding to store when store not empty" );
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			final ADDLeaf[] news = new ADDLeaf[(int) numdds];
			for( int i = 0 ; i < numdds; ++i ){
				news[i] = new ADDLeaf().getNullDD();
			}
			for( int i = 0 ; i < numdds; ++i ){
				try{
					storeLeaf.offer( news[i] );
				}catch(OutOfMemoryError e){
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		
		if( node ){
			if( !storeINodes.isEmpty() ){
				try{
					throw new Exception("addto store when inodes not empty" );
				}catch(Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			final ADDINode[] news = new ADDINode[ (int) numdds ];
			for( int i = 0 ; i < numdds; ++i ){
				news[i] = new ADDINode().getNullDD();
			}
			for( int i = 0 ; i < numdds; ++i ){
				try{
					storeINodes.offer( news[i] );
				}catch(OutOfMemoryError e){
					e.printStackTrace();
					System.exit(1);
					return;
				}
			}	
		}
	}

//	private synchronized void addToUnaryTempCache(final ADDRNode in, final ADDRNode ret) {
//		Objects.requireNonNull( new Object[]{ in, ret } );
//		_tempUnaryCache.put( in, ret );
//	}
	
	public ADDRNode threshold( final ADDRNode input, final double threshold, final boolean strict ){
		//< thresh & strict ? 1: 0 
		//<= thresh & !strict ? 1 : 0
		//sets neg inf to 0 always
//		Objects.requireNonNull( input );
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();;//CacheBuilder.from( temp_unary_cache_spec ).build();
		ADDRNode ret = thresholdInt( input, threshold, strict, _tempUnaryCache );
		_tempUnaryCache = null;
		return ret;
	}

	private double thresholdDouble( final double val , final double thresh, final boolean strict ){
		if( val == getNegativeInfValue() ){
			return 0;
		}else if( strict ){
			return val < thresh ? 1 : 0;
		}else{
			return val <= thresh ? 1 : 0;
		}
	}
	
	//thresholding is done wrt upper bound of leaf nodes
	private ADDRNode thresholdInt( final ADDRNode input, final double threshold, 
			final boolean strict, Map<ADDRNode, ADDRNode> _tempUnaryCache ){
//		Objects.requireNonNull( input );
		final ADDNode node = input.getNode();

		if( node instanceof ADDLeaf ){
			final ADDLeaf leaf = (ADDLeaf)node;
			final Double value = leaf.getLeafValues(); 
			final double val1 = thresholdDouble( value , threshold, strict );
//			final double val2 = thresholdDouble( value._o2, threshold, strict );
			return getLeaf( val1 );
		}else{
			ADDRNode ret = _tempUnaryCache.get( input );
			if( ret == null ){
				final String var = input.getTestVariable();
				final ADDRNode truth = thresholdInt( input.getTrueChild(), 
						threshold, strict, _tempUnaryCache );
				final ADDRNode falseth = thresholdInt( input.getFalseChild(), 
						threshold, strict, _tempUnaryCache );
				ret = getINode( var, truth, falseth );
				_tempUnaryCache.put(input, ret);
			}
			return ret;
		}
	}
	
//	private void clearTempUnaryCache(){
//		_tempUnaryCache.invalidateAll();
//	}
	
	public ADDRNode sumDD( final ADDRNode... input ){
		ADDRNode ret = DD_ZERO;
		for( final ADDRNode each : input ){
			ret = apply( ret, each, DDOper.ARITH_PLUS );
		}
		return ret;
	}
	
//	public ADDRNode sumDD( final ArrayList<ADDRNode> input ){
//		ADDRNode ret = DD_ZERO;
//		for( final ADDRNode each : input ){
//			ret = apply( ret, each, DDOper.ARITH_PLUS );
//		}
//		return ret;
//	}
	
	public ADDRNode sumDD( final ArrayList<ADDRNode>... input ){
		ADDRNode ret = DD_ZERO;
		for( final ArrayList<ADDRNode> list : input ){
			for( final ADDRNode each : list ){
				ret = apply( ret, each, DDOper.ARITH_PLUS );
			}
		}
		return ret;
	}
	
	public ADDRNode apply( final ADDRNode op1, final ADDRNode op2,
			final DDOper op ){
//		Objects.requireNonNull( op );
//		invalidateApplyCache();
		final ADDRNode ret = applyInt( op1, op2, op );
		if( ret.getMax() == Double.NaN ){
			try{
				throw new Exception("Nan produced");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		throwAwayApplyCache();
//		System.gc();
		return ret;
	}
	
	private void invalidateApplyCache() {
		for( final DDOper op : applyCache.keySet() ){
			Table<ADDRNode, ADDRNode, ADDRNode> cache =
					applyCache.get(op);
			cache.clear();			
		}
	}

	public ADDRNode applyInt( final ADDRNode op1, final ADDRNode op2,
			final DDOper op) {
//		Objects.requireNonNull( op1 );
//		Objects.requireNonNull( op2 );
		//not able to construct Inodes with indicator due to neg inf
		//special rule : 0 prod neginf = 0
		final boolean op1_zero = op1.equals(DD_ZERO);
		final boolean op2_zero = op2.equals(DD_ZERO);
		
		if( op1_zero || op2_zero ){
			switch( op ){
				case ARITH_PROD :
					return DD_ZERO;
				case ARITH_PLUS :
					return ( op1_zero ? op2 : op1 );
				case ARITH_DIV :
					if( op2_zero ){
						try{
							throw new ArithmeticException("divide by zero");
						}catch( ArithmeticException e ){
							e.printStackTrace();
							System.exit(1);
						}
					}else{
						return DD_ZERO;
					}
					break;
				case ARITH_MINUS : 
					if( op2_zero ){
						return op1;
					}
					break;
			}
		}
		
		final boolean op1_one = op1.equals(DD_ONE);
		final boolean op2_one = op2.equals(DD_ONE);
		
		if( op1_one || op2_one ){
			switch( op ){
				case ARITH_PROD :
					return op1_one ? op2 : op1;
				case ARITH_DIV :
					if( op2_one ){
						return op1;
					}
			}
		}
		
		final boolean op1_neg_inf = op1.equals(DD_NEG_INF);
		final boolean op2_neg_inf =  op2.equals(DD_NEG_INF);
		final boolean is_neg_inf = ( op1_neg_inf || op2_neg_inf );
		
		if( is_neg_inf && op.equals(DDOper.ARITH_MAX) ){
			return op1_neg_inf ? op2 : op1;
		}
//		BUG : what if the DD underneath has some zeros -
		//which would violate our `abuse' of notation
		//ok if not multiply, i guess
		//otjerwise this need to be moved to leaf level
		else if( is_neg_inf && !op.equals(DDOper.ARITH_PROD) ){
			return DD_NEG_INF;
		}

		final ADDNode node1 = op1.getNode();
		final ADDNode node2 = op2.getNode();
		
		
		if( op.equals(DDOper.ARITH_MAX) || op.equals( DDOper.ARITH_MIN) ){
			if( op1.equals( op2 ) ){
				return op1;
			}
		}

		ADDRNode ret = null;
		if( node1 instanceof ADDLeaf && node2 instanceof ADDLeaf ){
			//we're done here
			ret = applyLeafOp( op1, op2, op);
			if( ret.getMax() == Double.NaN  ){
				try{
					throw new Exception("Nan produced");
				}catch( Exception e  ){
					e.printStackTrace();
					System.exit(1);
				}
			}
//			System.out.println("Leaf op : " + node1 + " " + op + " " + node2 + " = " + ret);
		}
		else if( op.equals(DDOper.ARITH_MAX ) &&
				( ( node1 instanceof ADDLeaf && node1.getMax() >= node2.getMax() )
				|| ( node2 instanceof ADDLeaf && node2.getMax() >= node1.getMax() ) ) ){
				return node1 instanceof ADDLeaf ? op1 : op2;
		}
		else if( op.equals(DDOper.ARITH_MIN) &&
				( ( node1 instanceof ADDLeaf && node1.getMin() <= node2.getMin() )
				|| ( node2 instanceof ADDLeaf && node2.getMin() <= node1.getMin() ) ) ){
			return node1 instanceof ADDLeaf ? op1 : op2;
					
		}
		else if( op.equals(DDOper.ARITH_MAX) && 
				node1.getMin() >= node2.getMax() ){
			return op1;
		}else if( op.equals(DDOper.ARITH_MAX) &&
				node2.getMin() >= node1.getMax() ){
			return op2;
		}else if( op.equals(DDOper.ARITH_MIN) && 
				node1.getMax() <= node2.getMin() ){
			return op1;
		}else if( op.equals(DDOper.ARITH_MIN) && 
				node2.getMax() <= node1.getMin() ){
			return op2;
		}
		else{
			final ADDRNode lookup = lookupPair(op1, op2, op);
			if( lookup != null ){
				++applyHit ;
				ret = lookup;	
			}else{

				if( node1 instanceof ADDINode && node2 instanceof ADDINode 
						&& isEqualVars(op1, op2) ){
					//check if equal test vars
					final ADDRNode true_child_op1 = op1.getTrueChild();
					final ADDRNode false_child_op1 = op1.getFalseChild();
					final ADDRNode true_child_op2 = op2.getTrueChild();
					final ADDRNode false_child_op2 = op2.getFalseChild();
					
					final ADDRNode trueAns = applyInt( 
							true_child_op1, true_child_op2, op );
					final ADDRNode falseAns = applyInt( 
							false_child_op1, false_child_op2, op);
					ret = makeINode(op1.getTestVariable(), trueAns, falseAns);
				}else{
					//unequal test vars
					//descend on one
					final ADDRNode trueAns, falseAns;
					if( isBefore( op1, op2 ) ){//descend op1
						trueAns = applyInt( op1.getTrueChild(), op2, op );
						falseAns = applyInt( op1.getFalseChild(), op2, op );
						ret = makeINode( op1.getTestVariable(), trueAns, falseAns);
					}else{//descend op2
						trueAns = applyInt( op1, op2.getTrueChild(), op );
						falseAns = applyInt( op1, op2.getFalseChild(), op );
						ret = makeINode( op2.getTestVariable(), trueAns, falseAns);
					}
				}
			}
			addToApplyCache(op1, op2, op, ret);	
		}
						
		return ret;
	}
	
	public ADDRNode applyLeafOp(final ADDLeaf node1, final ADDLeaf node2, 
			final DDOper op) {
		final Double leaf1 = node1.getLeafValues();
		final Double leaf2 = node2.getLeafValues();
		double result = Double.NaN;//, result_2 = Double.NaN;
		ADDRNode ret = null;

		switch( op ){

		case ARITH_PLUS : 
			result = (leaf1 + leaf2);
			break;

		case ARITH_MINUS : 
			result = (leaf1 - leaf2);
			break;

		case ARITH_PROD : 
			result = leaf1*leaf2;
//			
//			result_2 = Math.max( 
//					Math.max( prod1, prod2 ),
//					Math.max(prod3, prod4 ) );
			break;

		case ARITH_DIV:
			try{
//				if( leaf2 < 0 && leaf2 > 0 ){
//					throw new ArithmeticException("interval contains zero in division.");
//				}
//				
//				final double div1 = leaf1/leaf2;
//				final double div2 = leaf1/leaf2;
//				final double div3 = leaf1/leaf2;
//				final double div4 = leaf1/leaf2;
				
				result = leaf1/leaf2;//Math.min( Math.min(div1,  div2), Math.min(div3, div4) );
//				result_2 = Math.max( Math.max(div1, div2), Math.max(div3, div4) );
			}catch( ArithmeticException e ){
				e.printStackTrace();
				System.exit(1);
			}
			break;

		case ARITH_MAX :
//			result_2 = Math.max(leaf1, leaf2);
			result = 
					Math.max(leaf1, leaf2);
//					( result_2 == leaf1._o2) ? leaf1._o1 : leaf2._o1;
			break;

		case ARITH_MIN :
//			result_2 = Math.min(leaf1, leaf2);
			result = 
					Math.min(leaf1, leaf2);
//					( result_2 == leaf1._o2) ? leaf1._o1 : leaf2._o1;
			break;

		default : 	
			System.err.println("unknown operation");
			System.exit(1);
		}

		if( result == Double.NaN ){//|| result_2 == Double.NaN ){
			try{
				throw new Exception("NaN's prduced");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		ret = getLeaf( result );
//		System.out.println( rnode1 + " " + rnode2 + " " + op + " " + ret); 
		return ret;
	}

	public ADDRNode applyLeafOp(final ADDRNode rnode1, final ADDRNode rnode2, 
			final DDOper op) {
		final ADDLeaf node1 = (ADDLeaf) rnode1.getNode();
		final ADDLeaf node2 = (ADDLeaf) rnode2.getNode();
		return applyLeafOp(node1, node2, op);
	}

//	@Override
//	public ADDRNode approximate(ADDRNode input, double epsilon, APPROX_TYPE approx_type) {
//		try{
//			throw new UnsupportedOperationException("APRICODD not yet implemented");
//		}catch( UnsupportedOperationException e ){
//			e.printStackTrace();
//		}
//		return null;
//	} 

//	public synchronized void clearApplyCache(){
//
//		Set<Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, 
//		MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>>> maps = applyCache.entrySet();
//
//		Iterator<Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>,
//		MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>>> it = maps.iterator();
//
//		while( it.hasNext() ){
//
//			Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, 
//			MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>> inner = it.next();
//
//			ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
//			MySoftReference<ADDRNode>> innerMap = inner.getValue();
//
//			Set<Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
//			MySoftReference<ADDRNode>>> innerSet = innerMap.entrySet();
//
//			Iterator<Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
//			MySoftReference<ADDRNode>>> itIn = innerSet.iterator();
//
//			while( itIn.hasNext() ){
//
//				Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>, 
//				MySoftReference<ADDRNode>> thing = itIn.next();
//
//				Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> key = thing.getKey();
//				MySoftReference<ADDRNode> value = thing.getValue();
//
//				if( value == null || value.get() == null 
//						|| key._o1 == null || key._o2 == null
//						|| key._o1.get() == null || key._o2.get() == null ){
//
//					itIn.remove();
//
//				}
//
//			}
//
//		}
//
//	}

//	public synchronized
//	<T extends Comparable<T>, U extends Comparable<U>>
//		void clearDeadEntries( final ConcurrentHashMap<
//				MySoftReference<U>, MySoftReference<T> > map ){
//
//		new Thread( new Runnable() {
//			
//			@Override
//			public void run() {
//				Set<Entry<MySoftReference<U>, MySoftReference<T>>> set = map.entrySet();
//
//				Iterator<Entry<MySoftReference<U>, MySoftReference<T>>> it = set.iterator();
//
//				while( it.hasNext() ){
//					Entry<MySoftReference<U>, MySoftReference<T>> thing = it.next();
//					MySoftReference<U> key = thing.getKey();
//					MySoftReference<T> value = thing.getValue();
//					if( key == null || key.get() == null || 
//							value == null || value.get() == null ){
//						it.remove();
//					}
//				}				
//			}
//		}, "clearDeadEntries").start();
//	}
//
//	public synchronized void clearDeadNodes( final boolean clearDeadMaps) {
//
//		//take references from ref queue
//		//get the object in it
//		//remove it from madeINodes or madeLeafs
//		//by using nullify()
//		removeDeadEntries( deletedLeafNodes );
//		removeDeadEntries( deletedINodes );
//		removeDeadEntries( deletedRNodes );
//		if( clearDeadMaps ){
//			clearMadeNodes();
////			clearApplyCache();	
////			Thread.currentThread()
//		}
//	}

//	private <T> void removeDeadEntries(final ReferenceQueue<T> ref_q) {
//
//		new Thread( new Runnable() {
//			
//			@Override
//			public void run() {
//				Reference<? extends T> item;
//				int total = 0, deleted = 0;
//
//				while( (item = ref_q.poll()) != null ){
//
//					T entry = item.get();
//
//					if(  entry != null  ){
//						if( entry instanceof ADDLeaf ){
//							ADDLeaf leaf = (ADDLeaf)entry;
//							removeMap(new MySoftReference<ADDLeaf>(leaf), madeLeaf );
//							leaf.nullify();
//							leaf = null;
//						}else if( entry instanceof ADDINode ){
//							ADDINode inode = (ADDINode)entry;
//							removeMap( new MySoftReference<ADDINode>(inode), 
//									madeINodes.get(inode.getTestVariable() ) );
//							inode.nullify();
//							inode = null;
//						}else if( entry instanceof ADDRNode ){
//							ADDRNode rnode = (ADDRNode)entry;
//							if( !permenantNodes.contains(rnode) ){
//								ADDNode node = rnode.getNode();
//								if( node instanceof ADDINode ){
//									ADDINode inode = (ADDINode)node;
//									ConcurrentHashMap<MySoftReference<ADDINode>, 
//										MySoftReference<ADDRNode>> innerMap 
//									= madeINodes.get( inode.getTestVariable() );
//									removeMap( new MySoftReference<ADDINode>(inode), 
//											innerMap );
//									inode.nullify();
//									inode = null;
//								}else if( node instanceof ADDLeaf ){
//									ADDLeaf leaf = (ADDLeaf)node;
//									removeMap( new MySoftReference<ADDLeaf>(
//											leaf),
//											madeLeaf );
//									leaf.nullify();
//									leaf = null;
//								}
//								++deleted;
//							}
//	
//						}else{
//							try{
//								throw new Exception("whut whut!");
//							}catch( Exception e ){
//								e.printStackTrace();
//								System.exit(1);
//							}
//						}
//						entry = null;
//					}
//					++total;
//				}
//
//				System.err.println( "Total: " + total + " Deleted : " + deleted );
//		
//			}
//		}, "flushcaches").start();		
//	}
//
//	private synchronized void clearMadeNodes() {
//
//		Set<Entry<String, ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>>>>
//			map = madeINodes.entrySet();
//
//		Iterator<Entry<String, ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>>>> 
//			itOut = map.iterator();
//
//		while( itOut.hasNext() ){
//
//			Entry<String, ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>>> 
//				inner = itOut.next();
//			ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>> 
//				innerMap = inner.getValue();
//			clearDeadEntries(innerMap);
//		}
//
//		clearDeadEntries(madeLeaf);
//
//	}

	@Override
	public boolean compare(final ADDRNode input1, final ADDRNode input2) {
//		Objects.requireNonNull( new Object[]{input1, input2} );
		final ADDRNode diff = apply( input1, input2, DDOper.ARITH_MINUS );
		if( diff.equals(DD_ZERO) ){
			return true;
		}
		return false;
	}

	public ADDRNode constrain( final ADDRNode rnode, final ADDRNode rconstrain, 
			final ADDRNode violate ){

//		if( _constrainCache == null ){
//			_constrainCache = new ConcurrentHashMap< 
//					Pair< ADDRNode, ADDRNode >, ADDRNode >();
//		}
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();// CacheBuilder.from( temp_unary_cache_spec ).build();
		Map<ADDRNode, ADDRNode> _tempExistCache 
		= new HashMap<ADDRNode, ADDRNode>();
		final ADDRNode ret = constrainInt(rnode, rconstrain, violate, _tempUnaryCache ,
				_tempExistCache );
		_tempUnaryCache = null;
		_tempExistCache = null;
		return ret;
	}

	//constraint diagram is a BDD
	//replaces constrained parts of rnode with violate
	public ADDRNode constrainInt( final ADDRNode rnode, 
			final ADDRNode rconstrain, final ADDRNode violate,
			Map<ADDRNode, ADDRNode> _tempUnaryCache,
			final Map<ADDRNode, ADDRNode> _tempExistCache ){
//		Objects.requireNonNull( new Object[]{ rnode, rconstrain, violate } );
		final ADDNode node = rnode.getNode();
		final ADDNode constr = rconstrain.getNode();
		ADDRNode ret = null;

		if( rconstrain.equals(DD_ZERO) ){
//			System.out.println("constraint was violated");
			//return zero
			ret = violate;
		}else if( rconstrain.equals(DD_ONE) ){
			//fine
//			System.out.println("constraint satisfied " + rnode);
			ret = rnode;
		}else if( node instanceof ADDLeaf ){
//			System.out.println("leaf was reached in input ADD " + rnode);
			ret = rnode;
		}else{
			ADDRNode lookup = _tempUnaryCache.get( rnode );

			if( lookup == null ){
				if( node instanceof ADDINode && constr instanceof ADDINode ){
					final ADDRNode trueAns, falseAns;
					if( isEqualVars(rnode, rconstrain) ){
//								System.out.println("descend both " + rnode.getTestVariable() 
//										+ " " + rconstrain.getTestVariable() );
							final ADDRNode rnode_true = rnode.getTrueChild();
							final ADDRNode rnode_false = rnode.getFalseChild();
							final ADDRNode rconstrain_true = rconstrain.getTrueChild();
							final ADDRNode rconstrain_false = rconstrain.getFalseChild();
							trueAns = constrainInt(rnode_true, 
									rconstrain_true, violate, _tempUnaryCache , _tempExistCache);
//								System.out.println( rnode.getTestVariable() + " true " 
//										+ trueAns );
							falseAns = constrainInt( rnode_false, 
									rconstrain_false, violate, _tempUnaryCache, _tempExistCache );
//								System.out.println( rnode.getTestVariable() + " false " 
//										+ falseAns );
							final ADDRNode ans = getINode(rnode.getTestVariable(), trueAns, falseAns);
							return ans;
					}else{
							//descend just one
							//one inode at least
							if( isBefore(rnode, rconstrain) ){//descend on rnode
//									System.out.println("descend rnode " + rnode.getTestVariable() + " "
//											+ rconstrain.getTestVariable() );
								trueAns = constrainInt(rnode.getTrueChild(), 
										rconstrain, violate, _tempUnaryCache , _tempExistCache );
//									System.out.println( rnode.getTestVariable() + " true " 
//											+ trueAns );
								falseAns = constrainInt( rnode.getFalseChild(), 
										rconstrain, violate, _tempUnaryCache, _tempExistCache );
//									System.out.println( rnode.getTestVariable() + " false " 
//											+ falseAns );
								final ADDRNode ans = getINode(rnode.getTestVariable(), trueAns, falseAns);
								return ans;
							}else{
								//descend on rconstrain
								//here is the approximation
												
								//i want to apply only those constraints that are common to 
								//both branches
								//1. take sub-bdds... multiply (1-subbdds)
								//will have 1 in common constraints
								//do 1-that to get common constraints
								//presuming that the constraint is compact
								//multiplying the subdds is ok
//									System.out.println(" getting common constraints " 
//											+ rnode.getTestVariable() 
//											+ " " + rconstrain.getTestVariable() );
								//inefficiency : recurrent computation
								ADDRNode common = null;
								if( !_tempExistCache.containsKey(rconstrain) ){
									 common = getCommonConstraints( rconstrain );
									_tempExistCache.put(rconstrain, common );
								}else{
									common = _tempExistCache.get( rconstrain );
								}
								final ADDRNode ans = constrainInt( rnode, 
									common, violate, _tempUnaryCache , _tempExistCache );
								return ans;
							}
					}
			}else{
				try{
					throw new ExecutionException("not two Inodes", null);
				}catch( ExecutionException e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			}else{
				ret = lookup;
			}
		}
//		showGraph(rnode, rconstrain, ret);
		return ret;
	}

	//WARNING: constrain should be a BDD
	private ADDRNode getCommonConstraints(final ADDRNode rconstrain) {
//		Objects.requireNonNull(rconstrain);
		final ADDRNode truth = rconstrain.getTrueChild();
		final ADDRNode falseth = rconstrain.getFalseChild();
		final ADDRNode ret = BDDUnion(truth, falseth);
//		
//		final ADDRNode one_minus_truth = apply( DD_ONE, truth, DDOper.ARITH_MINUS);
//		final ADDRNode one_minus_falseth = apply( DD_ONE, falseth, DDOper.ARITH_MINUS);
//		final ADDRNode common_one = apply( one_minus_falseth, one_minus_truth, DDOper.ARITH_PROD );
//		final ADDRNode ret = apply( DD_ONE, common_one, DDOper.ARITH_MINUS );
//		Objects.requireNonNull( new Object[]{ ret, truth, falseth, one_minus_falseth,
//				one_minus_truth, common_one } );
		return ret;
	}

	public List<Long> countNodes( final ADDRNode... rnodes ){
//		Objects.requireNonNull( rnodes );
		ArrayList<Long> ret = new ArrayList<Long>();
		for( ADDRNode rnode : rnodes ){
			if( rnode == null ){
				continue;
			}
			ret.add( countNodesInt( rnode , new HashSet<ADDRNode>() ) );
		}
		return ret;//Collections.unmodifiableList( ret ); 
	}

	private long countNodesInt( final ADDRNode rnode, final HashSet<ADDRNode> seen ){
//		Objects.requireNonNull( rnode );
//		Objects.requireNonNull( seen );
		if( seen.contains(rnode) ){
			return 0;
		}
		final ADDNode node = rnode.getNode();
		if( node instanceof ADDLeaf ){
			return 0;
		}
		seen.add( rnode );
		final long trueCount = countNodesInt( rnode.getTrueChild(), seen );
		final long falseCount = countNodesInt( rnode.getFalseChild(), seen );
		return 1L + trueCount + falseCount;
	}

	public int countPathsBDD( final ADDRNode rnode ){
		return enumeratePathsBDD(rnode).size();
	}
	
	public int countPathsADD( final ADDRNode rnode ){
//		Objects.requireNonNull( rnode );
//		final ADDNode node = rnode.getNode();
//		if( node instanceof ADDLeaf ){
//			return 1;
//		}
//		final int trueCount = countPaths( rnode.getTrueChild() );
//		final int falseCount = countPaths( rnode.getFalseChild() );
//		return trueCount + falseCount;
		return enumeratePathsADD(rnode).size();
	}

	
	@Override
	public void createStore( final long NumDDs ) {
		this.STORE_INCREMENT = NumDDs;
		storeINodes = new ArrayBlockingQueue< ADDINode >( (int) NumDDs );
		storeLeaf = new ArrayBlockingQueue< ADDLeaf >( (int) NumDDs );
		addToStore(NumDDs, true, true);
	}
	
	public ADDRNode all_paths_to_leaf( 
			final ADDRNode input, 
			final ADDLeaf leafVal ) {
		return all_paths_to_leaf(input, leafVal.getLeafValues());
	}
	
	public ADDRNode all_paths_to_leaf( 
			final ADDRNode input, 
			final Double leafVal ) {
		ADDRNode ret = DD_ZERO;
		Set<NavigableMap<String, Boolean>> assigns = enumeratePaths(input, false, true, leafVal, false );
		if( assigns.isEmpty() ){
			return DD_ONE;
		}
		for( final NavigableMap<String, Boolean> path : assigns ){
			final ADDRNode this_path_dd = getProductBDDFromAssignment( path );
			ret = BDDUnion( this_path_dd, ret );
		}
		return ret;
	}
	
	public Set<NavigableMap<String,Boolean>> enumeratePathsBDD(
			final ADDRNode input ) {
		return enumeratePaths( input, true, true, 
				DD_ONE,
				false );
		
	}
	
	public Set<NavigableMap<String,Boolean>> enumeratePathsADD(
			final ADDRNode input ) {
		return enumeratePaths( input, true, true, 
				DD_NEG_INF,
				true );
		
	}
	
	public Set<NavigableMap<String,Boolean>> enumeratePaths(
			final ADDRNode input ) {
		return enumeratePaths( input, true, false, 
				DD_ZERO,
				false );
	}
	public Set<NavigableMap<String,Boolean>> enumeratePaths(
			final ADDRNode input, 
			final boolean include_leaves, final boolean specified_leaves_only, 
			final ADDRNode leaf, final boolean invert ) {
		return enumeratePaths( input, include_leaves, specified_leaves_only, 
				((ADDLeaf)(leaf.getNode())).getLeafValues(),
				invert );
	}
	
	public Set<NavigableMap<String,Boolean>> enumeratePaths(
			final ADDRNode input, 
			final boolean include_leaves, final boolean specified_leaves_only, 
			final ADDLeaf leafVal, final boolean invert ) {
		return enumeratePaths( input, include_leaves, specified_leaves_only, leafVal.getLeafValues(),
				invert );
	}
	
	public Set<NavigableMap<String,Boolean>> enumeratePaths(
			final ADDRNode input, 
			final boolean include_leaves, final boolean specified_leaves_only, 
			final Double leafVal, final boolean invert ) {
		
		//NOTE : when input is a leaf node
		//output will be empty set
		//do not iterate over result
		
//		Objects.requireNonNull( input );
//		final Cache< ADDRNode, Set<NavigableMap<String,Boolean>> > _tempUnaryCache 
//		= CacheBuilder.from( temp_unary_cache_spec ).build();
	
		Map< ADDRNode, Set<NavigableMap<String, Boolean>> > _tempUnaryCache 
		= new HashMap<ADDRNode, Set<NavigableMap<String, Boolean>>>();;//= CacheBuilder.from( temp_unary_cache_spec ).build();
		final Set<NavigableMap<String, Boolean>> paths = new HashSet<NavigableMap<String,Boolean>>();
		
		enumeratePathsInt( input, include_leaves, specified_leaves_only, 
					leafVal , invert , _tempUnaryCache , paths );
//		_tempUnaryCache.invalidateAll();
		_tempUnaryCache = null;
		return paths;// Collections.unmodifiableSet( paths );
	}

	public static void testEnumeratePaths( ){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(0d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

//		man.showGraph(inode2);

		System.out.println( man.enumeratePaths(inode2, false, false, ((ADDLeaf)leaf1.getNode()) , false) );

		System.out.println( man.enumeratePaths(inode2, false, true, ((ADDLeaf)leaf1.getNode()) ,false) );
		
		System.out.println( man.enumeratePaths(inode2, true, false, ((ADDLeaf)leaf1.getNode()) ,false) );
		
		System.out.println( man.enumeratePaths(inode2, true, true, ((ADDLeaf)leaf1.getNode()) ,false) );
		
		System.out.println( man.enumeratePaths(inode2, true, true, ((ADDLeaf)leaf1.getNode()) ,true) );
		
	}
	
	public static void testPathsToLeaf( ){

		ArrayList<String> ord = new ArrayList<String>();

		
		ord.add("Y");
		ord.add("X");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(0d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, 
				 man.getINode("X", leaf1, man.DD_ONE ) );

		man.showGraph(inode2);

		man.showGraph( man.all_paths_to_leaf(inode2, ((ADDLeaf)leaf1.getNode()) ) );
		
	}

	private int enumeratePathsInt(final ADDRNode input,
			final boolean include_leaves, 
			final boolean specified_leaves_only, 
			final Double leafVal , final boolean invert ,
			final Map<ADDRNode, Set<NavigableMap<String, Boolean>> > cache,
			final Set<NavigableMap<String, Boolean>> paths ){
//			final Cache<ADDRNode , Set<NavigableMap<String,Boolean> > > cache ){
//			final Cache<ADDRNode, Set<NavigableMap<String, Boolean>>> cache ) {

//		Set<NavigableMap<String, Boolean>> lookup = cache.getIfPresent( input );
//		if( lookup != null ){
//			return lookup;
//		}
		
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			
			boolean add = true;
			if( specified_leaves_only ){
				if( ( invert && !(((ADDLeaf)node).getLeafValues().equals(leafVal)) )  || 
					( ( !invert &&((ADDLeaf)node).getLeafValues().equals(leafVal) ) ) ){
					add = true;
				}else{
					add = false;
				}
			}
			
			if( add ){
				if( include_leaves ){
					NavigableMap<String, Boolean> tm = new TreeMap<String, Boolean>();
					tm.put( node.toString(), false) ;
					paths.add( tm );//Maps.unmodifiableNavigableMap( tm ) );
				}
			}else{
				return -1;//nly case when path must not be added if it fails the condition
				//caller must ensure it is not added
				//ie X=t/f is not added when paths is empty
			}
			//no need to add anyhting 
		
		}else if( cache.containsKey(input) ){
			final Set<NavigableMap<String, Boolean>> cached = cache.get( input );
			paths.addAll(cached);
		}else{
//			final TreeMap<String, Boolean> truth = new TreeMap<String, Boolean>(current);
			final String testVar = input.getTestVariable();
			final Set<NavigableMap<String, Boolean>> this_node_paths = 
					new HashSet<NavigableMap<String,Boolean>>();
			
			final Set<NavigableMap<String, Boolean>> true_paths
			= new HashSet<NavigableMap<String, Boolean> >();
			
			final int ret_true = enumeratePathsInt(input.getTrueChild(), include_leaves, 
					specified_leaves_only, leafVal , invert, cache , true_paths );//, cache );
			//for each path in true_paths do union with true and put in this node paths
			//do union with current in addition and put in paths
			
			if( true_paths.isEmpty() && ret_true != -1 ){
				NavigableMap<String, Boolean> this_true_path = new TreeMap<String, Boolean>(  );
				this_true_path.put( testVar, true );
				this_node_paths.add(this_true_path);
			}else{
				for( final NavigableMap<String, Boolean> true_path : true_paths ){
					NavigableMap<String, Boolean> this_true_path = new TreeMap<String, Boolean>( true_path );
					this_true_path.put( testVar, true );
					this_node_paths.add( this_true_path );//Maps.unmodifiableNavigableMap( this_true_path ) );
				}	
			}
			
			
			final Set<NavigableMap<String, Boolean>> false_paths
			= new HashSet<NavigableMap<String, Boolean> >();

			final int ret_false = enumeratePathsInt( input.getFalseChild(), include_leaves, 
					specified_leaves_only, leafVal, invert, cache, false_paths );
			if( false_paths.isEmpty() && ret_false != -1  ){
				NavigableMap<String, Boolean> this_false_path = new TreeMap<String, Boolean>( );
				this_false_path.put( testVar, false );
				this_node_paths.add(  this_false_path );//Maps.unmodifiableNavigableMap( this_false_path ) );
			}else{
				for( final NavigableMap<String, Boolean> false_path : false_paths ){
					NavigableMap<String, Boolean> this_false_path = new TreeMap<String, Boolean>( false_path );
					this_false_path.put( testVar, false );
					this_node_paths.add( this_false_path );//  Maps.unmodifiableNavigableMap( this_false_path ) );
				}
			}
			
			final Set<NavigableMap<String, Boolean>> ret 
				= this_node_paths;//Collections.unmodifiableSet( this_node_paths );
			cache.put( input, ret );
			paths.addAll(ret);
			
			//dont have to add to paths - as it will be added at each leaf
			//at INOde keep track of paths from each child
//			ret.addAll( ret_false );
//			ret.addAll( ret_truth );
		}
		return 0;
	}

	@Override
	public ADDRNode evaluate(final ADDRNode input, final Map<?, Boolean> assign) {
		
		ADDRNode ret = input;
		while( !( ret.getNode() instanceof ADDLeaf ) ){
			final String testVar = ret.getTestVariable();
			final boolean value = assign.get( testVar );
			ret = ( value ) ?  ret.getTrueChild() : ret.getFalseChild();
		}
		return ret;
	}

	public static void testEvaluate(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(5d);

		ADDRNode leaf2 = man.getLeaf(2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph(inode2);

		TreeMap<String, Boolean> assign = new TreeMap<String, Boolean>();

		assign.put( "X", true );

		man.showGraph( man.evaluate(inode2, assign) );

	}

	@Override
	public void flushCaches( ){// final ADDRNode save ) {
//		System.out.println( "Flushing : " + applyHit );
//		applyHit = 0;
//		this._tempCache.clear();
//		final double mem_percent = getMemoryPercent();
//		throwAwayApplyCache();
//		if( mem_percent > 0.99d ){
////			invalidateApplyCache();
////			throwAwayApplyCache();
//			madeLeaf.clear();
//			for( final ReferenceMap<ADDINode, ADDRNode> inner : madeINodes.values() ){
//				inner.clear();
//			}
////			System.gc();
//		}
//			cacheSummary();
////			_tempUnaryCache.invalidateAll();
//			madeLeaf.cleanUp();
//			for( final Cache< ADDINode, ADDRNode > inner : madeINodes.values() ){
//				inner.cleanUp();
//			}
//		}
			//			inner.getValue().invalidateAll();
//			inner.getValue().putAll( permanentMadeINodes.get( inner.getKey() ).asMap() );
//		}
//		madeLeaf.invalidateAll();
//		madeLeaf.putAll( permanentMadeLeaf.asMap() );
//		for( Map.Entry< String,
//				Cache< ADDINode, ADDRNode > > inner : madeINodes.entrySet() ){
//			inner.getValue().invalidateAll();
//			inner.getValue().putAll( permanentMadeINodes.get( inner.getKey() ).asMap() );
//		}
//	}
//	cacheSummary();
//	System.out.println( mem_percent );
	
//		System.out.println("Low on memory, flushing caches and nodes");
//		reduceCache.clear();
//		clearDeadNodes(clearDeadMaps);
////		madeLeaf.clear();
////		madeINodes.clear();
//		restorePermenants( );			
//	}
	}

	private void throwAwayApplyCache() {
	    	applyCache = new EnumMap< 
		DDOper, Table< ADDRNode, ADDRNode , ADDRNode > >( DDOper.class );
	    	
//		for( final Table<ADDRNode, ADDRNode, ADDRNode> t  : applyCache.values()) {
//			t.clear();
//		}
	}

	//testaddPair
	//testlookuppair

//	private void clearAll(final ADDRNode... save) {
//		madeLeaf.invalidateAll();
//		for( Map.Entry<String, Cache<ADDINode, ADDRNode>> entry : madeINodes.entrySet() ){
//			entry.getValue().invalidateAll();
//		}
//		for( ADDRNode rnode : save ){
//			cacheNode( rnode );
//		}
//	}

//	public void cacheSummary() {
////		System.out.println( "Apply hits : " + applyHit );
////		System.out.println("Apply cache size : " + getApplyCacheSize() );
//		System.out.println("---------apply cache-------");
//		for( DDOper op : applyCache.keySet() ){
//			Map<Pair<ADDRNode, ADDRNode>, ADDRNode>  cache = applyCache.get( op );
//			System.out.println( cache.stats().toString() );
//		}
//		System.out.println("------Made INodes stats" );
//		for( Map.Entry< String, Cache<ADDINode, ADDRNode > > entry 
//				: madeINodes.entrySet() ){
//			System.out.println( " MadeInodes for " + entry.getKey() );
//			System.out.println( entry.getValue().stats().toString() );
//		}
//		System.out.println("-------MadeLeaf nodes----");
////		System.out.println("Permenant Inodes : " + perm.size() );
//		System.out.println( madeLeaf.stats().toString() );
//	}

//	private int getApplyCacheSize() {
//		int size = 0;
//		for( Entry<DDOper, 
//				 Cache< 
//					Pair< ADDRNode, ADDRNode >, ADDRNode > >  
//						entry : applyCache.entrySet() ){
//			size += entry.getValue().size();
//		}
//		return size;
//	}
//
//	private void restorePermenants() {
//		for( final ADDRNode rnode : permenantNodes ){
//			final ADDNode node = rnode.getNode();
//			Objects.requireNonNull( rnode );
//			Objects.requireNonNull( node );
//			
//			if( node instanceof ADDLeaf ){
//				madeLeaf.put( (ADDLeaf) node, rnode );
//			}else{
//				final String testVar = rnode.getTestVariable();
//				Cache<ADDINode, ADDRNode> innerMap = madeINodes.get( testVar );
//				final ADDINode inode = (ADDINode)rnode.getNode();
//				Objects.requireNonNull( innerMap );
//				innerMap.put( inode, rnode );
//			}
//		}
//	}

	public ADDRNode getBernoulliProb( final int num_vars, final double prob ){

		ArrayList<String> ord = new ArrayList<String>();
		for( int i = 0 ; i < num_vars; ++i ){
			ord.add("X"+i);
		}

		List<ADDRNode> Pis = new ArrayList<ADDRNode>();

		for( int i = 0 ; i < num_vars; ++i ){
			ADDRNode thisOne = getINode("X"+i, getLeaf(prob), getLeaf(1-prob) );
			Pis.add( thisOne );
		}

		ADDRNode res = DD_ONE;

		for( ADDRNode thing : Pis ){
			res = apply( res, thing, DDOper.ARITH_PROD );
			//			man.showGraph(res);
		}

//		if( LOGGING_ON ){
//			memorySummary();	
//		}
		

		//		man.showGraph(res);

		return res;


	}

	private synchronized <T extends Comparable<T> > T getFirstRealOne(
			final Queue<T> store) {
		return store.poll();//store poll() might block - using ArrayBlockingQueue
	}

	public ADDRNode getIndicatorDiagram(final String testVar, final boolean b) {
//		Objects.requireNonNull( testVar );
		final ADDRNode ret = 
				getINode(testVar, b ? DD_ONE : DD_ZERO,  b ? DD_ZERO : DD_ONE );
		return ret;
//		ADDINode inode = (ADDINode) getOneNullDD(false);
//
//		if( inode == null ){
//			System.err.println("can't make more inodes");
//		}
//
//		ADDINode ret = null;
//		//plug in
//		try {
//
//			if( b ){
//				ret = inode.plugIn( testVar, new UniPair<ADDRNode>( DD_ONE, DD_ZERO ) );
//			}else{
//				ret = inode.plugIn( testVar, new UniPair<ADDRNode>( DD_ZERO, DD_ONE ) );
//			}
//
//		} catch (Exception e) {
//			ret = null;
//			//			e.printStackTrace();
//		}
//
//		return getRNode(ret, true);
	}
	
	//creates new null inode
	//plugs into that inode
	//get Rnode - gets unique version of that node
	//returns falsebranch if truebranch.equals(falsebranch)
	//WARNING: assumes truebranch and falsebranch are reduced
	public ADDRNode getINode( final String testVar, 
			final ADDRNode trueBranch, final ADDRNode falseBranch ){

		//if you have RNode it should be that
		//under that node
		//no true = false
		//for inodes under

		//obvious reduction
//		Objects.requireNonNull( trueBranch );
//		Objects.requireNonNull( falseBranch );
//		Objects.requireNonNull( testVar );
		
		if( trueBranch == null || falseBranch == null ){
			return null;
		}

		if( trueBranch.equals(falseBranch) ){
			return trueBranch;
		}
		
//		try {
//			_temp_null_inode.plugIn(testVar, trueBranch, falseBranch);
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//		
		final ADDNode trueNode = trueBranch.getNode() ;
		final ADDNode falseNode = falseBranch.getNode();
		ADDRNode ret = null;

		if( trueNode instanceof ADDLeaf && falseNode instanceof ADDLeaf ){
			ret = makeINode( testVar, trueBranch, falseBranch );
//			System.out.println( "made inode from leaves " );
//			System.out.println( trueBranch + "," + falseBranch + "=>" + ((ADDINode)ret.getNode()).getChildren() );
		}else{

			//here i will handle other cases of reduction
			//1. if testVar is already in true or false - 
			//2. if testvar appears lower in the ordering 

			//for both these, i will create indicatr diagrams
			//and depend on apply to take care of ordering 

			ADDRNode true_br = null;
			if( trueBranch.equals(DD_NEG_INF) ){
				true_br = makeINode(testVar, DD_NEG_INF, DD_ZERO);
			}else{
				final ADDRNode trueComp = getIndicatorDiagram( testVar, true );
				true_br = apply( trueComp, trueBranch, DDOper.ARITH_PROD );
			}

			ADDRNode false_br = null;
			if( falseBranch.equals(DD_NEG_INF) ){
				false_br = makeINode(testVar, DD_ZERO, DD_NEG_INF);
			}else{
				final ADDRNode falseComp = getIndicatorDiagram( testVar, false );
				false_br = apply( falseComp, falseBranch, DDOper.ARITH_PROD );
			}

			if( true_br.equals(DD_NEG_INF) || false_br.equals(DD_NEG_INF) ){
				System.err.println("should not be here... apply add with NEG_INF");
				System.exit(1);
			}
			ret = apply( true_br, false_br, DDOper.ARITH_PLUS );
//			if( trueBranch.equals(DD_NEG_INF) || falseBranch.equals(DD_NEG_INF) ){
//				System.out.println( " getInode was called with neg inf");
//				System.out.println(" check results " );
//				showGraph( trueBranch, falseBranch, true_br, false_br, ret );
//			}
		}

//		if( ret.getTestVariable() != testVar.intern() ){
//			try {
//				throw new Exception("Fatal error: RNode did not have same testvar as inode");
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}

		return ret;

		/*old method

		ADDNode n1 = trueBranch.getNode();
		ADDNode n2 = falseBranch.getNode();

		//get from store
		ADDINode inode = (ADDINode) getOneNullDD(false);

		if( inode == null ){
			System.err.println("can't make more inodes");
		}

		ADDINode ret = null;
		//plug in
		try {
			 ret = inode.plugIn( testVar, new UniPair<ADDRNode>( trueBranch, falseBranch ) );
		} catch (Exception e) {
			ret = null;
//			e.printStackTrace();
		}

		return getRNode(ret, true);
		 */

	}

	//creates new null leaf
	//plugs in
	//getRNode
//	public ADDRNode getLeaf( final Double low, final Double high ){
//		_temp_null_leaf.plugIn( low, high );
//		return getRNode( ADDLeaf.class, true );
////		ADDLeaf ret = null;
////		try{
////			ret = leaf.plugIn( new Pair<Double, Double>(
////					low, high) );
////		}catch(NullPointerException e){
////			ret = null;
////		}
//		return getRNode( ADDLeaf.class, true );
//	}

	@Override
	public SortedSet<ADDLeaf> getLeaves(final ADDRNode input) {
//		Objects.requireNonNull( input );
		SortedSet<ADDLeaf> ret = new TreeSet<ADDLeaf>();
		getLeavesInt( input, ret, new TreeSet<ADDRNode>() );
		return ret;//Collections.unmodifiableSortedSet( ret );
	}

	public static void testGetLeaves(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(5d);

		ADDRNode leaf2 = man.getLeaf(2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		System.out.println( man.getLeaves( inode2 ) );


	}

	private void getLeavesInt( final ADDRNode input, final SortedSet<ADDLeaf> leaves,
			final Set<ADDRNode> visited ) {

		if( visited.contains(input) ){
			return;
		}
		final ADDNode node = input.getNode();

		if( node instanceof ADDLeaf ){
			leaves.add((ADDLeaf) node);
		}else{
			visited.add(input);
			getLeavesInt(input.getTrueChild(), leaves, visited);
			getLeavesInt(input.getFalseChild(), leaves, visited);
		}
		return;
	}

	private String getMemoryMBs() {
		double total = _runtime.totalMemory()/BytesToMB;
		double current = total -  _runtime.freeMemory()/BytesToMB;
		return current + " \\ " + total;
	}

	public double getMemoryPercent() {
		long total = _runtime.totalMemory();

		return (total-_runtime.freeMemory())/((double)total);
	}

	@Override
	public Set<ADDRNode> getNodes(final ADDRNode input, final boolean leaves) {
		final TreeSet<ADDRNode> ret = new TreeSet<ADDRNode>();
		getNodesInt( input, ret, leaves);
		return ret;// Collections.unmodifiableSet( ret );
	}

	private void getNodesInt( final ADDRNode input, 
			final TreeSet<ADDRNode> nodes, final boolean leaves ) {

		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			if( leaves ){
				nodes.add( input );
			}
			return;
		}
		if( nodes.contains(input) ){
			return;
		}

		nodes.add( input );
		getNodesInt( input.getTrueChild(), nodes, leaves);
		getNodesInt( input.getFalseChild(), nodes, leaves);
	}

//	private int getNumINodes() {
//		int tot  = 0;
//		for( Cache< ADDINode, ADDRNode> maps : madeINodes.values() ){
//			tot += maps.size();
//		}
//		return tot;
//	}

	@Override
	public synchronized ADDNode getOneNullDD( final boolean leaf ) {
		ADDNode ret = null;

		if( leaf ){

			//try
			ADDLeaf aLeaf = getFirstRealOne(storeLeaf);
			if( aLeaf == null ){
				//try adding 
				try{
//					LOGGER.fine("store has no more leaves. Adding " + STORE_INCREMENT + " leaves ");
//					if( LOGGING_ON ){
//						memorySummary();
//					}
					addToStore(STORE_INCREMENT, true, false);
					aLeaf = getFirstRealOne(storeLeaf);
//					Objects.requireNonNull( aLeaf );
					ret = aLeaf;
				}catch(OutOfMemoryError e){
					ret = null;
					System.err.println( "can't make more leaves" );
					e.printStackTrace();
					System.exit(1);
				}
			}else{
				ret = aLeaf;
			}

		}else{

			ADDINode inode = getFirstRealOne( storeINodes );
			if( inode == null ){
				//try adding
				try{
//					LOGGER.fine("store has no more nodes. Adding "
//							+ STORE_INCREMENT + " nodes ");
					addToStore(STORE_INCREMENT, false, true);
					inode = getFirstRealOne(storeINodes);
//					Objects.requireNonNull( inode );
					ret = inode;
//					if( LOGGING_ON ){
//						memorySummary();
//					}
				}catch( OutOfMemoryError e ){
					ret = null;
					System.err.println("cant create more inodes");
					e.printStackTrace();
					System.exit(1);
				}
			}else{
				ret = inode;
			}
		}

		return ret;
	}

	//main methods of getting RNode from INode and Leaf
	//gets the canonical copy - including negated edges
	protected <T extends ADDNode> ADDRNode getRNode( final Class<T> type,
			final boolean create ){
//		Objects.requireNonNull( obj );

		ADDRNode ret = null;
		if( type.isInstance(_temp_null_inode) ){
			final String testvar = _temp_null_inode.getTestVariable();
			ReferenceMap<ADDINode, ADDRNode> look_in_cache = madeINodes.get(testvar);

			if( look_in_cache == null ){
				look_in_cache = new ReferenceMap<ADDINode, ADDRNode>(); 
//				    CacheBuilder.from( made_inode_cache_spec ).recordStats().
//							removalListener( 
//									new RemovalListener<ADDINode, ADDRNode>() {
//								@Override
//								public void onRemoval( final RemovalNotification<ADDINode, ADDRNode> arg ) {
////									System.err.println( "removed Inode " + arg.toString() );
//									ADDINode node = arg.getKey();
//									node = null;
//								}
//							}).build();
				madeINodes.put(testvar, look_in_cache);
			}
			
			final boolean looked = look_in_cache.containsKey( _temp_null_inode );
			if( !looked ){
			    	//new nulldd is needed strictly if not found
			    //maintain global nullDD and use plugin
			    //if needed to insert only mthen make a copy and add to made
			    //
//				final ADDINode negNode = inode.getNegatedNode( _nullDD ((ADDINode)getOneNullDD(false)) );
//				_temp_null_inode.swapChilden("nuller");
				
//				final boolean looked_neg = look_in_cache.containsKey( _temp_null_inode );
				
				if( create ){//!looked_neg && create ){
					//not found at all
					//switch back to original children? 
//					_temp_null_inode.swapChildren();
					
					final ADDINode save = _temp_null_inode;
					_temp_null_inode = null;
					ret = new ADDRNode(save);
					look_in_cache.put( save, ret );
					
					_temp_null_inode = (ADDINode) getOneNullDD(false);
//					System.out.println( "created new rnode " + looked.hashCode() + " for inode : " + inode );
					
				}else{
					return null;
				}
//				else if( looked_neg  ){
//					// negated node exists in memory
//					// return with a not sign
//					//if create=false and looked=null
//					//not creating memory for inode
//					//but only for reference 
//					//this inode has swapped children
//					//no need to add it to cache
//					final ADDINode save = _temp_null_inode;
//					_temp_null_inode = null;
//					ret = new ADDRNode( look_in_cache.get( save ) , true );
//					look_in_cache.put( save, ret );
//					_temp_null_inode = (ADDINode) getOneNullDD(false);
//					
//				}
			}else{
				ret = look_in_cache.get( _temp_null_inode );
			}
//			else{
//				//the inode was present in the cache
//				//but, we created a new one in checking
//				//must nullify that?
//				
//			}
			
			//sanity - difference should be zero
//			if( ( !looked.isNegated() && !looked.getNode().equals( inode ) ) 
//					|| ( looked.isNegated() && 
//							!((ADDINode)looked.getNode()).getNegatedNode().equals( inode ) ) ){
//				try {
//					throw new Exception("bad inode. wanted " + inode + " got "
//							+ looked + " \n " + inode.hashCode() + " " + looked.getNode().hashCode() );
//				} catch (Exception e) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}

		}else if( type.isInstance( _temp_null_leaf ) ){

//			final ADDLeaf leaf = (ADDLeaf)obj;
//			ret = madeLeaf.get( leaf );
			final boolean looked = madeLeaf.containsKey( _temp_null_leaf );
			if( !looked ){
//				System.out.println("Creating new " + leaf.toString() + " " + leaf.hashCode() );
//				leaf.printHash();
				final ADDLeaf save = _temp_null_leaf;
				_temp_null_leaf = null;
				ret = new ADDRNode( save );
				madeLeaf.put( save, ret );
				_temp_null_leaf = (ADDLeaf) getOneNullDD(true);
			}else{
				ret = madeLeaf.get( _temp_null_leaf );
			}
			
//			if(! looked.getNode().equals(leaf) ){
//				try {
//					throw new Exception("bad leaf. wanted " + leaf + " got "
//							+ looked + " \n " + leaf.hashCode() + " " + looked.getNode().hashCode() );
//				} catch (Exception e) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
		}
		return ret;
	}

//	public <T extends Comparable<T> > MySoftReference<T> getSoftRef(T obj,
//			ReferenceQueue<T> que){
//		//obj should have been created here
//		MySoftReference<T> ret = new MySoftReference<T>( obj , que);
//		obj = null;
//		return ret;
//	}

	@Override
	public List<Set<String>> getVars(final ADDRNode... inputs) {
		List<Set<String>> ret = new ArrayList<Set<String>>();
		
		for( final ADDRNode input : inputs ){
			final TreeSet<ADDRNode> seen = new TreeSet<ADDRNode>();
			final TreeSet<String> vars = new TreeSet<String>();
			getVarsInt( input, seen, vars);
			ret.add( vars );//Collections.unmodifiableSet( vars ) );
		}
		return ret;
	}

	private void getVarsInt(final ADDRNode input, final TreeSet<ADDRNode> visited, 
			final Set<String> vars ) {
//		Objects.requireNonNull( input );
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			return;
		}
		if( visited.contains(input) ){
			return;
		}
		vars.add( input.getTestVariable() );
		visited.add( input );
		getVarsInt(input.getTrueChild(), visited, vars);
		getVarsInt(input.getFalseChild(), visited, vars);
	}

	private boolean isEqualVars(final ADDRNode node1, final ADDRNode node2) {
		return node1.getTestVariable().equals(   
				node2.getTestVariable() );//interned , can use ==
	}

	public ADDRNode lookupApplyCache( final ADDRNode a, final ADDRNode b, final DDOper op ){
//		Objects.requireNonNull( a );
//		Objects.requireNonNull( b );
//		Objects.requireNonNull( op );
		
		ADDRNode ret = null;
		ret = lookupPair(a, b, op);
		if( ret == null ){
			if( isCommutative(op) ){
				ret = lookupPair(b, a, op);
			}
		}
		return ret;
	}

	//this method looks up a hash in a map<Integer,Mysoftreference> and gets the underlying object
	//if it exists
//	public <N extends Comparable<N>> ADDRNode lookupMap( 
//			final MySoftReference<N> soft_ref_node, 
//			Map< MySoftReference<N> , MySoftReference<ADDRNode>> aMap ){
//
//		MySoftReference<ADDRNode> thing = aMap.get( soft_ref_node );
//
//		if( thing == null ){
//			return null;
//		}
//		
//		ADDRNode gotten = null;
//
//		try{
//			gotten = thing.get();
//		}catch( NullPointerException e ){
//			gotten = null;
//		}
//
//		if( thing != null && gotten != null ){
//
//			return gotten;
//
//		}else{
//
//			//bookkeeping 
//			if( thing != null && gotten == null ){
//				aMap.remove( soft_ref_node );
//			}
//
//			return null;
//
//		}
//
//	}

	private ADDRNode lookupPair( final ADDRNode a, final ADDRNode b, final DDOper op ){
//		Objects.requireNonNull( a );
//		Objects.requireNonNull( b );
		
		ADDRNode ret = null;
		if( applyCache.get(op) == null ){
			ret = null;
		}else{
			final Table<ADDRNode, ADDRNode, ADDRNode> theMap =
				applyCache.get(op);
//			Pair< ADDRNode, ADDRNode> key = 
//					new Pair< ADDRNode, ADDRNode>( a, b );//DONT DO THIS
			ret = theMap.get( a,b );
//			key = null;
		}
		return ret;
	}

//	private ADDRNode lookupTempUnaryCache( final ADDRNode in ) {
//		Objects.requireNonNull( in );
//		final ADDRNode looked = _tempUnaryCache.getIfPresent( in );
//		return looked;
//	}

	//this methoid just constructs the inode
	//does nothing about reduction
	//use getINode 
	private ADDRNode makeINode(String testVariable, ADDRNode trueBranch,
			ADDRNode falseBranch) {

		if( trueBranch.equals(falseBranch) ){
			return trueBranch;
		}

//		if( trueBranch.getNode() instanceof ADDINode ){
//			final String true_var = trueBranch.getTestVariable();	
//			if( _ordering.indexOf(testVariable) > _ordering.indexOf(true_var) ){
//				try{
//					throw new Exception("makeInode called out of order");
//				}catch( Exception e){
//					e.printStackTrace();
//					System.exit(1);
//				}	
//			}
//		}
//		
//		if( falseBranch.getNode() instanceof ADDINode ){
//			final String false_var = falseBranch.getTestVariable();
//			if( _ordering.indexOf( testVariable ) > _ordering.indexOf(false_var) ){
//				try{
//					throw new Exception("makeInode called out of order");
//				}catch( Exception e){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
//		}

		try {
			_temp_null_inode.plugIn( _ordering.get( _ordering.indexOf( testVariable ))
					, trueBranch, falseBranch );
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
//		_temp_null_inode.updateMinMax();
		
		ADDRNode ret = getRNode( ADDINode.class, true);
//		if( !ret.isNegated() && ret.getNode() != inode ){
//			inode = null;//found in cache
//		}
//		ret.getNode().updateMinMax();
		return ret;

	}
	
//	public ADDRNode restrict_approx( final ADDRNode input, final ADDRNode care_set,
//			final ADDRNode violate ){
//		Map< ADDRNode, ADDRNode > _tempUnaryCache 
//		= new HashMap<ADDRNode, ADDRNode>();
//		final ADDRNode ret = restrict_approx_int( input, care_set, _tempUnaryCache, violate );
//		_tempUnaryCache = null;
//		return ret;
//	}
//
//	private ADDRNode restrict_approx_int(final ADDRNode rnode, final ADDRNode care_set,
//			final Map<ADDRNode, ADDRNode> _tempUnaryCache, ADDRNode violate) {
//		final ADDNode node = rnode.getNode();
//		final ADDNode constr = rnode.getNode();
//		if( constr instanceof ADDLeaf ){
//			if( care_set.equals(DD_ZERO) ){
//				return violate;
//			}else if( care_set.equals(DD_ONE) ){
//				return rnode;
//			}
//			return null;
//		}else if( node instanceof ADDLeaf ){
//			return rnode;
//		}
//
//		ADDRNode lookup = _tempUnaryCache.get( rnode );
//
//		if( lookup == null ){
//			if( node instanceof ADDINode && constr instanceof ADDINode ){
//				final ADDRNode trueAns, falseAns;
//				if( isEqualVars(rnode, rconstrain) ){
////							System.out.println("descend both " + rnode.getTestVariable() 
////									+ " " + rconstrain.getTestVariable() );
//						final ADDRNode rnode_true = rnode.getTrueChild();
//						final ADDRNode rnode_false = rnode.getFalseChild();
//						final ADDRNode rconstrain_true = rconstrain.getTrueChild();
//						final ADDRNode rconstrain_false = rconstrain.getFalseChild();
//						trueAns = constrainInt(rnode_true, 
//								rconstrain_true, violate, _tempUnaryCache , _tempExistCache);
////							System.out.println( rnode.getTestVariable() + " true " 
////									+ trueAns );
//						falseAns = constrainInt( rnode_false, 
//								rconstrain_false, violate, _tempUnaryCache, _tempExistCache );
////							System.out.println( rnode.getTestVariable() + " false " 
////									+ falseAns );
//						final ADDRNode ans = getINode(rnode.getTestVariable(), trueAns, falseAns);
//						return ans;
//				}else{
//						//descend just one
//						//one inode at least
//						if( isBefore(rnode, rconstrain) ){//descend on rnode
////								System.out.println("descend rnode " + rnode.getTestVariable() + " "
////										+ rconstrain.getTestVariable() );
//							trueAns = constrainInt(rnode.getTrueChild(), 
//									rconstrain, violate, _tempUnaryCache , _tempExistCache );
////								System.out.println( rnode.getTestVariable() + " true " 
////										+ trueAns );
//							falseAns = constrainInt( rnode.getFalseChild(), 
//									rconstrain, violate, _tempUnaryCache, _tempExistCache );
////								System.out.println( rnode.getTestVariable() + " false " 
////										+ falseAns );
//							final ADDRNode ans = getINode(rnode.getTestVariable(), trueAns, falseAns);
//							return ans;
//						}else{
//							//descend on rconstrain
//							//here is the approximation
//											
//							//i want to apply only those constraints that are common to 
//							//both branches
//							//1. take sub-bdds... multiply (1-subbdds)
//							//will have 1 in common constraints
//							//do 1-that to get common constraints
//							//presuming that the constraint is compact
//							//multiplying the subdds is ok
////								System.out.println(" getting common constraints " 
////										+ rnode.getTestVariable() 
////										+ " " + rconstrain.getTestVariable() );
//							//inefficiency : recurrent computation
//							ADDRNode common = null;
//							if( !_tempExistCache.containsKey(rconstrain) ){
//								 common = getCommonConstraints( rconstrain );
//								_tempExistCache.put(rconstrain, common );
//							}else{
//								common = _tempExistCache.get( rconstrain );
//							}
//							final ADDRNode ans = constrainInt( rnode, 
//								common, violate, _tempUnaryCache , _tempExistCache );
//							return ans;
//						}
//				}
//		}else{
//			try{
//				throw new ExecutionException("not two Inodes", null);
//			}catch( ExecutionException e ){
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//		}else{
//			ret = lookup;
//		}
//		
//	}

//	private void makeSureNoNull() {
//
//		for( ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>> setInodes  
//				: madeINodes.values() ){ 
//			for( MySoftReference<ADDRNode> ref : setInodes.values() ){
//				if( ref == null || ref.get() == null ){
//					System.err.println("oops... null madeinode");
//					System.exit(1);
//				}
//			}
//		}
//
//		for( MySoftReference<ADDRNode> ref : madeLeaf.values() ){
//			if( ref == null || ref.get() == null ){
//				System.err.println("oops... null madeinode");
//				System.exit(1);
//			}
//		}
//
//		for( ConcurrentHashMap< 
//				Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> > 
//		, MySoftReference<ADDRNode> > maps : applyCache.values() ){
//
//			for( Map.Entry<Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> > 
//			, MySoftReference<ADDRNode> > entry : maps.entrySet() ){
//
//				Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> key = entry.getKey();
//				MySoftReference<ADDRNode> value = entry.getValue();
//
//				if( value == null || value.get() == null 
//						|| key._o1 == null || key._o2 == null
//						|| key._o1.get() == null || key._o2.get() == null ){
//
//					try {
//						throw new Exception("null entry in apply?");
//					} catch (Exception e) {
//						e.printStackTrace();
//						System.exit(1);
//					}
//
//				}	
//
//			}
//
//
//		}
//
//
//
//	}
	

	public static void testMarginalize( ){
		
		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(3d);

		ADDRNode leaf2 = man.getLeaf(2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ONE);

		man.showGraph(inode2);
		
		ADDRNode one = man.marginalize(inode2, "X", DDMarginalize.MARGINALIZE_SUM);
		
		ADDRNode two = man.marginalize(inode2, "X", DDMarginalize.MARGINALIZE_MAX);
		
		ADDRNode three = man.marginalize(inode2, "Y", DDMarginalize.MARGINALIZE_SUM);
		
		ADDRNode four = man.marginalize(inode2, "Y", DDMarginalize.MARGINALIZE_MAX);
		
		man.showGraph( one, two, three, four );
		
	}

	@Override
	public ADDRNode marginalize( final ADDRNode input, final String var, 
			final DDMarginalize oper) {
//		Objects.requireNonNull( input );
//		Objects.requireNonNull( var );
//		Objects.requireNonNull( oper );
//		System.out.println("marginalizing " + var + " " + oper);
		final int index = _ordering.indexOf(var);
		if( index == -1 ){
			try{
				throw new Exception("cannot marginalize " + var + " not found in ordering" );
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();;//= CacheBuilder.from( temp_unary_cache_spec ).build();
		final ADDRNode ret = marginalizeInt( input, index, oper, _tempUnaryCache );	
//		showGraph( input, ret );
		_tempUnaryCache = null;
		return ret;
	}

	private ADDRNode marginalizeInt(final ADDRNode in, final int index,
			final DDMarginalize oper, Map<ADDRNode, ADDRNode> _tempUnaryCache) {
//		Objects.requireNonNull( in );
//		Objects.requireNonNull( oper );
		
		if( in.getNode() instanceof ADDLeaf ){
//			if( !in.equals(DD_NEG_INF) ){
//				System.out.println("not neg inf leaf");
//			}
			return in;
		}

		final ADDRNode looked = _tempUnaryCache.get( in );
		ADDRNode ret = null;
		if( looked != null ){
//			System.out.println("cache hit " + index );
//			System.out.println( looked );
			ret = looked;
		}else{
			final String testVar = in.getTestVariable();
			final int curIndex = _ordering.indexOf(testVar);
			if( curIndex == index ){
//				System.out.println( " marginalize " + in.getTestVariable() );
				ret = apply( in.getTrueChild(), in.getFalseChild(), getArithOper( oper ) );
//				System.out.println( in.getTestVariable() + " marginalize returned " +  ret );
//				if( ret.equals( DD_NEG_INF ) ){
//					showGraph(in, ret);
//					try {
//						System.in.read();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//				System.out.println( "result of marginalize " + oper );
//				showGraph( in, ret );
//				try {
//					System.in.read();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}else if( curIndex < index ){
				//recurse
//				System.out.println( in.getTestVariable() + " true." );
				final ADDRNode true_marg = marginalizeInt( in.getTrueChild(), index, 
						oper, _tempUnaryCache );
//				System.out.println( in.getTestVariable() + " true. result was "
//						+ true_marg );
//				if( true_marg.equals(DD_NEG_INF) ){
//					showGraph( in, true_marg );
//					try {
//						System.in.read();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//				System.out.println( in.getTestVariable() + " false." );
				final ADDRNode false_marg = marginalizeInt(in.getFalseChild(), 
						index, oper, _tempUnaryCache );
//				System.out.println( in.getTestVariable() + " false. result was "
//						+ false_marg );
//				if( false_marg.equals(DD_NEG_INF) ){
//					showGraph( in, false_marg );
//					try {
//						System.in.read();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//				Objects.requireNonNull( true_marg );
//				Objects.requireNonNull( false_marg );
				ret = getINode(in.getTestVariable(), true_marg, false_marg);
				//				System.out.println( "gotten inode = " + ret );
			}else{
				//this testvar is beyond the marginalizing variable in the 
				//ordering. Return as is.
				ret = in;
			}
			_tempUnaryCache.put( in, ret );
		}
		
//		if( in.getTestVariable().equals("reboot__c2") || in.getTestVariable().equals("running__c2") ){
//			showGraph( in, ret );	
//		}
		
		if( ret == null ){
			try{
				throw new NullPointerException("marginalize gave null");
			}catch( NullPointerException e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;
	}

//	private void addToTempUnaryCache(final ADDRNode in, final ADDRNode ret) {
//		Objects.requireNonNull( in );
//		Objects.requireNonNull( ret );
//		_tempUnaryCache.put( in, ret );
//	}

	private DDOper getArithOper( DDMarginalize oper) {
		if( oper.equals(DDMarginalize.MARGINALIZE_MAX) ){
			return DDOper.ARITH_MAX;
		}else if( oper.equals(DDMarginalize.MARGINALIZE_SUM) ){
			return DDOper.ARITH_PLUS;
		}else if( oper.equals( DDMarginalize.MARGINALIZE_MIN ) ){
			return DDOper.ARITH_MIN;
		}
		System.err.println("improper usage of get arith oper " + oper );
		System.exit(1);
		return null;
	}

//	public void memorySummary(){
//
//		System.out.println( getNumINodes() + " Inodes " );
//		System.out.println( madeLeaf.size() + " leaves" );
//
//		System.out.println("memory : " + getMemoryPercent() );
//		System.out.println( getMemoryMBs() );
////		cacheSummary();
//	}

//	private boolean nullify(ADDRNode rnode) {
//
//		//recurse?
//
//		ADDNode node = rnode.getNode();
//		boolean deleted = false;
//
//		if( node instanceof ADDLeaf ){
//			MySoftReference<ADDRNode> ret = removeMap( 
//					getSoftRef((ADDLeaf)node, deletedLeafNodes), madeLeaf );
//			if( ret != null ){
//				deleted = true;
//				ret.get().nullify();
//			}else{
//				deleted = false;
//			}
//		}else{
//			String testvar = ((ADDINode)node).getTestVariable();
//			ConcurrentHashMap<MySoftReference<ADDINode>, MySoftReference<ADDRNode>> 
//				innerMap = madeINodes.get(testvar);
//			MySoftReference<ADDRNode> ret = removeMap( 
//					getSoftRef((ADDINode)node, deletedINodes), innerMap );
//
//			if( ret == null ){
//				deleted = false;
//			}else{
//				deleted = true;
//				ret.get().nullify();
//			}
//
//		}
//
//		rnode.nullify();
//
//		return deleted;
//
//	}

//	@Override
//	public void nullifyDD(ADDNode input) {
//		input.nullify();
//		input = null;
//	}

	public static void testRemapVars(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Y'");
		ord.add("X'");
		
		ADDManager man = new ADDManager(100, 100, ord, 42);

		ADDRNode leaf1 = man.getLeaf(3d);

		ADDRNode leaf2 = man.getLeaf(6d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph( inode2 );
		
		Map< String, String > remap = new TreeMap<String, String>();
		remap.put("X", "X'");
		remap.put("Y", "Y'");
		
		ADDRNode remapped = man.remapVars(inode2, remap);
		
		man.showGraph(remapped);
		
	}
	
	@Override
	public ADDRNode scalarMultiply(final ADDRNode input, double scalar) {
		return apply( input, getLeaf(scalar), DDOper.ARITH_PROD );
	}

	@Override
	public ADDRNode remapVars(final ADDRNode input, final Map<String, String> remap) {
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();;//= CacheBuilder.from( temp_unary_cache_spec ).build();
		final ADDRNode ret = remapVarsInt(input, remap, _tempUnaryCache );
		_tempUnaryCache = null;
		return ret;
	}

	protected ADDRNode remapVarsInt(final ADDRNode input, final Map<String, String> remap,
			Map<ADDRNode, ADDRNode> _tempUnaryCache) {
//		Objects.requireNonNull( input );
//		Objects.requireNonNull( remap );
		
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			return input;
		}
		
		final ADDRNode lookup = _tempUnaryCache.get( input );
		if( lookup == null ){
			final String var = input.getTestVariable();
			final String newVar = remap.get( var );
			if( newVar == null ){
				try{
					throw new Exception("No remap for " + var );
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			final ADDRNode truth = input.getTrueChild();
			final ADDRNode falseth = input.getFalseChild();//negation taken care of
			final ADDRNode remap_true = remapVarsInt( truth , remap, _tempUnaryCache );
			final ADDRNode remap_false = remapVarsInt( falseth , remap, _tempUnaryCache );	
			final ADDRNode ret = getINode(newVar, remap_true, remap_false);
			if( ret == DD_NEG_INF ){
				System.out.println("Oops");
			}
			_tempUnaryCache.put( input, ret );
			return ret;
		}
		return lookup;
	}

//	public synchronized < N extends Comparable<N> > void  
//	removeMap( final MySoftReference<N> soft_node, Map< MySoftReference<N> ,MySoftReference<ADDRNode>> aMap ){
//
//		MySoftReference<ADDRNode> thing = aMap.get( soft_node );
//
//		ADDRNode gotten = null;
//
//		try{
//			gotten = thing.get();
//		}catch( NullPointerException e ){
//			gotten = null;
//		}
//
//		if( thing != null && gotten != null ){
////			System.err.println( "removed " );
//			aMap.remove( soft_node );
//			gotten = null;
//		}else if( thing == null ){
//			try {
//				throw new Exception("removeMap did not find item");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}else{
//
//			//also, bookkeeping 
//			if( thing != null && gotten == null ){
//				aMap.remove( soft_node );
//			}
//		}
//		return;
//	}

	@Override
	public ADDRNode restrict( final ADDRNode input, 
			final String var, boolean assign) {
//		Objects.requireNonNull( input );
//		Objects.requireNonNull( var );
		
		if( input.getNode() instanceof ADDLeaf ){
			return input;
		}
		
		final int index = _ordering.indexOf(var);
		if( index == -1 ){
			System.err.println("var " + var + " could not be found in ordering " + _ordering );
			System.exit(1);
		}
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();;//CacheBuilder.from( temp_unary_cache_spec ).build();
		final ADDRNode ret = restrictInt( input, var, assign, index, _tempUnaryCache );
		_tempUnaryCache = null;
		return ret;
	}

	private ADDRNode restrictInt( final ADDRNode in, final String var, 
			final boolean assign, 
			final int index, Map<ADDRNode, ADDRNode> _tempUnaryCache ){
//		Objects.requireNonNull( in );
//		Objects.requireNonNull( var );
		
		if( in.getNode() instanceof ADDLeaf ){
//			if( !in.equals(DD_NEG_INF) ){
//				System.out.println("not neg inf leaf");
//			}
			return in;
		}

		ADDRNode looked = _tempUnaryCache.get( in );
		ADDRNode ret = null;
		if( looked != null ){
			ret = looked;
		}else{
			final String testVar = in.getTestVariable();
			final int curIndex = _ordering.indexOf(testVar);
			if( curIndex == index ){
				ret = ( assign ) ? in.getTrueChild() : in.getFalseChild();
			}else if( curIndex < index ){
				//recurse
				final ADDRNode true_restrict = restrictInt( in.getTrueChild(), 
						var, assign, index, _tempUnaryCache );
				final ADDRNode false_restrict = restrictInt(in.getFalseChild(), 
						var, assign, index, _tempUnaryCache );
//				Objects.requireNonNull( true_restrict );
//				Objects.requireNonNull( false_restrict );
				ret = getINode(in.getTestVariable(), true_restrict, false_restrict);
				_tempUnaryCache.put( in, ret );
			}else{
				ret = in;
			}
		}
		return ret;
	}

	//graph vieweiing works
	//getRNode works w00t!
	//
	public void showGraph(final ADDRNode... nodes) {
//		Objects.requireNonNull( nodes );
		for( ADDRNode node : nodes ){
			Graph g = new Graph(true, false, false, false);
			node.toGraph(g);
			g.launchViewer();
		}
	}

	private boolean isBefore(final ADDRNode rnode1, final ADDRNode rnode2 ) {

		final ADDNode node1 = rnode1.getNode();
		final ADDNode node2 = rnode2.getNode();
		if( node1 instanceof ADDLeaf && node2 instanceof ADDLeaf ){
			try{
				throw new Exception("two leaves in isBefore");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
			return false;
		}else if( node1 instanceof ADDLeaf && node2 instanceof ADDINode ){
			return false;
		}else if( node1 instanceof ADDINode && node2 instanceof ADDLeaf ){
			return true;
		}else{
			//look at testvars
			final String var1 = rnode1.getTestVariable();
			final String var2 = rnode2.getTestVariable();
			final int ind1 = _ordering.indexOf( var1 );
			final int ind2 = _ordering.indexOf( var2 );

			if( ind1 == -1 || ind2 == -1 ){
				try {
					throw new Exception("Variable not found in ordering " + var1 + 
							" " + var2 + " " + _ordering );
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			if( ind1 < ind2 ){
				return true;
			}else if( ind1 > ind2 ){
				return false;
			}else{
				try {
					throw new Exception("WhichBefore called on equal test vars");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		return false;
	}

	public static double getNegativeInfValue() {
		return Double.NEGATIVE_INFINITY;
	}

	public ADDRNode restrict(final ADDRNode input,
			final Map<String, Boolean> assign) {
		if( input.getNode() instanceof ADDLeaf ){
			return input;
		}
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();//CacheBuilder.from( temp_unary_cache_spec ).build();
		final ADDRNode ret = restrictInt( input, assign, _tempUnaryCache );
		_tempUnaryCache = null;
		return ret;
	}

	private ADDRNode restrictInt(final ADDRNode input,
			final Map<String, Boolean> assign,
			final Map<ADDRNode, ADDRNode> _tempUnaryCache ) {
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			return input;
		}
		final String testVar = input.getTestVariable();
		final Boolean value = assign.get( testVar );
		if( value == null ){
			final ADDRNode true_ret = restrictInt( input.getTrueChild(), assign, _tempUnaryCache );
			final ADDRNode false_ret = restrictInt( input.getFalseChild(), assign, _tempUnaryCache );
			final ADDRNode ret = makeINode(testVar, true_ret, false_ret);
			_tempUnaryCache.put(input, ret);
			return ret;
		}else if( value.equals( Boolean.TRUE ) ){
			return restrictInt(input.getTrueChild(), assign, _tempUnaryCache);
		}else if( value.equals( Boolean.FALSE ) ){
			return restrictInt(input.getFalseChild(), assign, _tempUnaryCache);
		}
		return null;
	}
	
	public ADDRNode getProductBDDFromAssignment( final NavigableMap<String, Boolean> assignments ) {
	    ADDRNode ret = DD_ONE;
	    for( final String key : assignments.keySet() ){
	    	final boolean assign = assignments.get( key );
			ret = BDDIntersection(ret, getIndicatorDiagram(key, assign));
	    }
	    return ret;
	}

//	public ADDRNode getProductBDDFromAssignment(final NavigableMap<String, Boolean> assignments ) {
//		return getProductBDDFromAssignmentInt( 0, assignments );
//	}
//
//	private ADDRNode getProductBDDFromAssignmentInt( final int pos, 
//		final NavigableMap<String, Boolean> assignments ){
//	    if( pos >= _ordering.size() ){
//		return DD_ONE;
//	    }
//	    final String var = _ordering.get(pos);
//	    final Boolean val = assignments.get( var );
//	    if( val != null ){
//		final ADDRNode root_next = getProductBDDFromAssignmentInt(pos+1, assignments);
//		if( isBefore( root_next, getIndicatorDiagram(var, val) ) ) {
//		    try{
//			throw new Exception("root next is before " + var + " after " + root_next.getTestVariable() );
//		    }catch( Exception e ){
//			e.printStackTrace();
//			System.exit(1);
//		    }
//		}
//		
//		return getINode( var, val ? root_next : DD_ZERO, val ? DD_ZERO : root_next );
//	    }
//	    return getProductBDDFromAssignmentInt(pos+1, assignments);
//	}
//	public ADDRNode convertNegInfZeroDDToBDD(final ADDRNode input) {
//		//convert a DD with zero and neg inf DDs
//		//maps zero to one
//		//neg inf to zero
//		//this will make zeros to ones
//		final ADDRNode plus_one = apply( input, DD_ONE, DDOper.ARITH_PLUS );
//		//this will turn  neg infs to zero
//		final ADDRNode max_zero = apply( plus_one, DD_ZERO, DDOper.ARITH_MAX );
//		return max_zero;
//	}

	private boolean isBeforeOrdering(final String var1, final String var2) {
		final int ind1 = _ordering.indexOf( var1 );
		final int ind2 = _ordering.indexOf( var2 );
		return ind1 < ind2;
	}

	public boolean hasSuffixVars(final ADDRNode ret,
			final String suffix ) {
		Set<ADDRNode> setONodes = getNodes(ret, false);
		for( ADDRNode rn : setONodes ){
			String testVar = rn.getTestVariable();
			if( testVar.endsWith(suffix) ){
				return true;
			}
		}
		return false;
	}

	public boolean hasVars(final ADDRNode input, final Set<String> vars) {
		Set<ADDRNode> nodes = getNodes( input, false );
		for( ADDRNode rn : nodes ){
			String testvar = rn.getTestVariable();
			if( vars.contains( testvar ) ){
				return true;
			}
		}	
		return false;
	}

	public ADDRNode remapLeaf( final ADDRNode input, 
			final ADDRNode source, final ADDRNode dest ) {
//		Objects.requireNonNull( input );
//		Objects.requireNonNull( source );
//		Objects.requireNonNull( dest );
			
		Map< ADDRNode, ADDRNode > _tempUnaryCache 
		= new HashMap<ADDRNode, ADDRNode>();;//= CacheBuilder.from( temp_unary_cache_spec ).build();
		final ADDRNode replaced = remapLeafInt( input, source, dest, _tempUnaryCache );
		_tempUnaryCache = null;
		return replaced;
	}

	private ADDRNode remapLeafInt(final ADDRNode input, final ADDRNode source, 
			final ADDRNode dest, final Map<ADDRNode, ADDRNode> _tempUnaryCache) {
		
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			if( input.equals( source ) ){
				return dest;
			}
			return input;
		}

		final ADDRNode lookup = _tempUnaryCache.get( input );
		if( lookup != null ){
			return lookup;
		}
		
		final ADDRNode trueChild = input.getTrueChild();
		final ADDRNode true_replaced = remapLeafInt(  trueChild, source, dest, _tempUnaryCache );
		
		final ADDRNode falseChild = input.getFalseChild();
		final ADDRNode false_replaced = remapLeafInt( falseChild, source, dest, _tempUnaryCache );
		final ADDRNode replaced = getINode( input.getTestVariable(), true_replaced, false_replaced );
		_tempUnaryCache.put( input, replaced );
		return replaced;
	}

	public int getSize(final ADDRNode input, final boolean leaves) {
		return getNodes(input, leaves).size();
	}

	public ADDRNode productDD( final Set<ADDRNode> DDs) {
		if( DDs == null ){
			return null;
		}
		ADDRNode ret = DD_ONE;
		for( final ADDRNode rn : DDs ){
			if( rn != null ){
				ret = apply( ret, rn, DDOper.ARITH_PROD );				
			}
		}
		return ret;
	}
	
	public ADDRNode productDD( final ADDRNode... DDs ){
		ADDRNode ret = DD_ONE;
		for( final ADDRNode rn : DDs ){
			if( rn != null ){
				ret = apply( ret, rn, DDOper.ARITH_PROD );				
			}
		}
		return ret;
	}
	
	public ADDRNode productDD( final Set<ADDRNode> DDs, final ADDRNode... DDs_2 ){
		return productDD( productDD(DDs), productDD(DDs_2) );
	}
	
	public ADDRNode productDD( final Set<ADDRNode>... DDs ){
		ADDRNode ret = DD_ONE;
		for( final Set<ADDRNode> d : DDs ){
			ADDRNode this_prod = productDD(d);
			ret = productDD( ret, this_prod );
		}
		return ret;
	}

	public int[] countLeaves(final ADDRNode... inputs) {
		final int[] sizes = new int[ inputs.length ];
		int i = 0;
		for( final ADDRNode input : inputs ){
			final Set<ADDLeaf> set_o_leaves = getLeaves(input);
			sizes[ i++ ] = set_o_leaves.size();
		}
		return sizes;
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

	//sifting
	//pick one variable randomly
	//test the size in posn 1...n 
	//pick smallest ADD
	//repeat for all variables
	//input add
	//output add in new ordering
	//but side effect : changes ordering of addmanager
	//clear all caches 
	//reorder permenant ?
//	public UnorderedPair<ADDManager,ADDRNode> rudellSift(final ADDRNode input){
//		if( isLeaf(input) ){
//			return null;
//		}
//		String[] vars = {};
//		vars = getVars(input).toArray(vars);
//		int[] positions = new int[vars.length];
//		for( int i = 0 ; i < positions.length ; ++i ){
//			positions[i] = i;
//		}
//		return rudellSiftInt( new UnorderedPair<ADDManager, ADDRNode>(this,input), 
//				vars, positions, vars.length );
//	}

	//positions[i] == position of x_i in ordering
	//vars is not the ordering
	//finished vars wiill be put at the end of vars and 
	//positions will be appropriately changed
//	private UnorderedPair<ADDManager, ADDRNode> rudellSiftInt(final UnorderedPair<ADDManager, ADDRNode> input,
//			final String[] vars, final Integer[] positions, final int varsToGo ) {
//		if( varsToGo == 0 ){
//			return input;
//		}
//		
//		//pick variable
//		final int picked_index = _rand.nextInt(varsToGo);
//		final String picked_var = vars[picked_index]; 
//		final int current_position = positions[picked_index];
//		
//		//compute restrictions once
//		final ADDRNode true_branch = input._o1.restrict(input._o2, picked_var, true);
//		final ADDRNode false_branch = input._o1.restrict(input._o2, picked_var, false);
//
//		//iterate over possible positions for this var
//		long best_size = Long.MAX_VALUE;
//		int best_pos = -1;
//		UnorderedPair<ADDManager, ADDRNode> smallest_ADD = null;
//		for( int i = 0; i < positions.length ; ++i ){
//			final int old_posn = positions[picked_index];
//			positions[picked_index] = i;
//			final UnorderedPair<Long, UnorderedPair<ADDManager,ADDRNode> > 
//				size_i = reorder( input, vars, positions, true_branch, false_branch);
//			if( size_i._o1 < best_size ){
//				best_size = size_i._o1;
//				best_pos = i;
//				smallest_ADD = size_i._o2;
//			}
//		}
//		swapWith( vars, picked_index , varsToGo - 1 );
//		positions[picked_index] = best_pos;
//		swapWith( positions, picked_index, varsToGo - 1 );
//		return rudellSiftInt(smallest_ADD, vars, positions, varsToGo-1);
//	}

//	private UnorderedPair<Long, UnorderedPair<ADDManager, ADDRNode>> reorder(
//			final UnorderedPair<ADDManager, ADDRNode> input, final String[] vars,
//			final Integer[] positions,
//			final ADDRNode true_branch, final ADDRNode false_branch ) {
//		//reconstruct ordering
//		
//	}

	private <T> void swapWith(final T[] vars, final int src_index,
			final int dest_index ) {
		final T v_i = vars[src_index];
		final T v_j = vars[dest_index];
		vars[dest_index] = v_i;
		vars[src_index] = v_j;
	}

	private boolean isLeaf(ADDRNode input) {
		final ADDNode node = input.getNode();
		if( node instanceof ADDLeaf ){
			return true;
		}
		return false;
	}
	
	//assumes : input ADD is a BDD
	//exist quant is max margin.
	//univ quant is min margin.
	public ADDRNode quantify( final ADDRNode input, final String var, final DDQuantify quantification) {
//		System.out.print( quantification );
//		System.out.print(" of variable " );
//		System.out.print( var );
//		System.out.println();
		ADDRNode ret = null;
		switch( quantification ){
			case EXISTENTIAL : ret = marginalize( input , var, DDMarginalize.MARGINALIZE_MAX ); break;
			case UNIVERSAL : ret = marginalize(input, var, DDMarginalize.MARGINALIZE_MIN); break;
		}
		return ret;
	}

	//ASSUMES : input is BDD
	public ADDRNode BDDNegate(final ADDRNode input) {
		return apply( DD_ONE, input, DDOper.ARITH_MINUS );
	}

	public ADDRNode BDDUnion(final ADDRNode... input) {
		ADDRNode ret = DD_ZERO;
		for( final ADDRNode in : input ){
			ret = apply( ret, in , DDOper.ARITH_MAX );
		}
		return ret;
	}
	
	public ADDRNode BDDIntersection( final ADDRNode... inputs ){
		ADDRNode ret = DD_ONE;
		for ( final ADDRNode input : inputs ){
			ret = apply( ret, input, DDOper.ARITH_PROD ); 
		}
		return ret;
	}
	
	//not symmetric difference
	public ADDRNode BDDSubtract( final ADDRNode input1, final ADDRNode input2 ){
		//whenever input1 is zero, output is zero
		//wever input1 is one , op is one only if ip2 is zero
		final ADDRNode intersection = BDDIntersection( input1, input2 );
		//intersection has 1 in common 
		//set those to zero
		final ADDRNode intersection_compl = BDDNegate(intersection);
		return apply( input1, intersection_compl, DDOper.ARITH_PROD );
	}

	public ADDRNode productDD(final ArrayList<ADDRNode>... array_lists ){
		ADDRNode ret = DD_ONE;
		for( final ArrayList<ADDRNode> list : array_lists ){
			for( final ADDRNode node : list ){
				ret = apply( ret, node, DDOper.ARITH_PROD );
			}
		}
		return ret;
	}
	
	public ADDRNode sampleBDD( final ADDRNode input,
			final Random rand , final int num_paths ){
	    
	    if ( num_paths <= 0 ){
		try{
		    throw new Exception("Invalid arguement num_paths" + num_paths );
		}catch( Exception e ){
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	    
		ADDRNode ret = DD_ZERO;
		
		int i = 0;
		while( i <= num_paths ){
			final NavigableMap<String, Boolean> new_path = sampleOneLeaf(input, rand);
			final ADDRNode this_path = this.getProductBDDFromAssignment(new_path);
			ret = this.BDDUnion( ret, this_path );
			++i;
		}
		
		return ret;
	}
	
	//ASSUMES : input is  a BDD
	public static NavigableMap<String, Boolean> sampleOneLeaf(
			final ADDRNode input,
			final Random rand ) {

		NavigableMap<String, Boolean> ret = new TreeMap<String, Boolean>();
		ADDRNode curNode = input; 
		while( curNode.getNode() instanceof ADDINode ){
			if( curNode.getMax() == 0.0d ){
				try {
					throw new Exception("No non zero leaf but Inode in BDD");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			final ADDRNode cur_true_child = curNode.getTrueChild();
			final ADDRNode cur_false_child = curNode.getFalseChild();
			final double cur_true_max = cur_true_child.getMax();
			final double cur_false_max = cur_false_child.getMax();
			if( cur_true_max == 0.0d && cur_false_max > 0.0d ){
				ret.put( curNode.getTestVariable(),  false );
				curNode = cur_false_child;
			}else if( cur_true_max > 0.0d && cur_false_max == 0.0d ){
				ret.put( curNode.getTestVariable(), true );
				curNode = cur_true_child;
			}else{
				final boolean val = rand.nextBoolean();
				ret.put(curNode.getTestVariable(), val );
				curNode = val ? cur_true_child : cur_false_child;
			}
		}
		if( curNode.equals(DD_ZERO) ){
			try{
				throw new Exception("BDD sample is not one");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;//Maps.unmodifiableNavigableMap( ret );
	}

	public ADDRNode getSumNegInfDDFromAssignment(
			final NavigableMap<String, Boolean> assign) {
		ADDRNode ret = DD_ZERO;
		for( final String var : assign.keySet() ){
			final boolean value = assign.get( var );
			final ADDRNode this_dd = getINode(var, value ? DD_ZERO : DD_NEG_INF, value ? DD_NEG_INF : DD_ZERO );
			ret = apply( ret, this_dd, DDOper.ARITH_PLUS );
		}
		return ret;
	}

	public ADDRNode normalizePDF(final ADDRNode input, final ADDRNode constraint) {
		final ADDRNode constr_neg = BDDNegate(constraint);
		final ADDRNode missing_mass_dd = apply( input, constr_neg, DDOper.ARITH_PROD );
		final ADDRNode missing_mass = sumOutAllVariables( missing_mass_dd );
		ADDRNode ret = apply( input, constraint, DDOper.ARITH_PROD );
		ret = apply( ret ,  missing_mass, DDOper.ARITH_DIV );
		return ret;
	}
	
	public ADDRNode sumOutAllVariables( final ADDRNode input ){
		ADDRNode ret = input;
		final Set<String> vars = getVars( input ).get(0);
		for( final String var : vars ){
			ret = marginalize(ret, var, DDMarginalize.MARGINALIZE_SUM );
		}
		
		if( ret.getNode() instanceof ADDINode ){
			try {
				throw new Exception("marginalize not leaf");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return ret;
	}

	public ADDRNode normalizeConditionalPDF (
			final ADDRNode input, 
			final String var ) throws ArithmeticException {
		final ADDRNode var_true = getIndicatorDiagram(var, true);
		final ADDRNode var_false = getIndicatorDiagram(var, false );
		
		final ADDRNode prob_true = apply( var_true, input, DDOper.ARITH_PROD );
		final ADDRNode prob_false = apply( var_false, input, DDOper.ARITH_PROD );
		//this step necessary if var is not in DD
		//i.e. p_true = p_false
		//direct marginalizze will put leaf to 1
		//needs to be 0.5
		//sol : sum true prob  and false prob separately
		final ADDRNode total_prob_true = marginalize( prob_true, var, DDMarginalize.MARGINALIZE_SUM );
		final ADDRNode total_prob_false = marginalize( prob_false, var, DDMarginalize.MARGINALIZE_SUM );
		
		final ADDRNode total_prob = apply( total_prob_true, total_prob_false, DDOper.ARITH_PLUS );
		ADDRNode ret = null;
		try{
			ret = apply( input, total_prob, DDOper.ARITH_DIV );
		}catch( ArithmeticException e ){
			throw e;
		}
		return ret;
	}

	public ADDRNode getLeaf(double d) {
		_temp_null_leaf.plugIn( d );
		return getRNode( ADDLeaf.class, true );
	}

	public ADDRNode assign(final ADDRNode input,
			NavigableMap<String, Boolean> assign, 
			final double new_val ) {
		
		final ADDRNode bdd = getProductBDDFromAssignment(assign);
		final ADDRNode bdd_not = BDDNegate( bdd );
		final ADDRNode ret = apply( scalarMultiply(bdd, new_val),
				apply( bdd_not, input, DDOper.ARITH_PROD ),
				DDOper.ARITH_PLUS );
		return ret;
	}
	
//	public ADDRNode assign(final ADDRNode input,
//			NavigableMap<String, Boolean> assign, 
//			final double new_val ) {
//	
//	    //dont use apply assuming input is ordered and reduced
//	    Map< ADDRNode, ADDRNode > _tempUnaryCache  = new HashMap< ADDRNode, ADDRNode>();
//	    final Set<String> assigns = assign.keySet();
//	    final List<Integer> orders = new ArrayList<Integer>();
//	    
//	    for( final String ass : assigns ){
//		orders.add( _ordering.indexOf(ass) );
//	    }
//
//	    Collections.sort(orders, new Comparator<Integer>(){
//
//		@Override
//		public int compare(Integer o1, Integer o2) {
//		    return (o1-o2);
//		}
//		
//	    });
//	    
//	    final Integer[] orders_int = orders.toArray ( new Integer[orders.size()] );
//	    
//	    
//	    final ADDRNode ret = assignInt( input, Maps.unmodifiableNavigableMap( assign ), new_val, assign.size(), 
//		    _tempUnaryCache, orders_int , 0 );
//	    _tempUnaryCache = null;
//	    return ret;
//	}
//	
//	private ADDRNode assignInt( final ADDRNode input, final NavigableMap<String, Boolean> assign, 
//		final double new_val, final int to_go , Map<ADDRNode, ADDRNode> _tempUnaryCache, 
//		final Integer[] orders_int, final int cur_idx  ){
//	    final ADDNode theNode = input.getNode();
//	    if( to_go == 0 ){
//		return getLeaf(new_val);
//	    }else if( theNode instanceof ADDLeaf && to_go != 0 ){
//		return input;
//	    }else if( _tempUnaryCache.containsKey(input) ){
//		return _tempUnaryCache.get( input );
//	    }
//	    
//	    final String testVar = input.getTestVariable();
//	    final Boolean val = assign.get(testVar);
//	    final int this_ord = _ordering.indexOf( testVar );
//	    final int cur_ord_assign = orders_int[cur_idx];
//	    
//	    if( this_ord > cur_ord_assign ){
//		//current node skips levels
//		//some of these levels need to be assigned
//		ADDRNode recurse = DD_ONE;
//		while( cur_idx == this_ord ){
//		    final String that_var = _ordering.get( cur_idx );
//		    final Boolean that_assign = assign.get( that_var );
//		    recurse = BDDIntersection(recurse, getIndicatorDiagram(that_var, that_assign) );
//		    ++cur_idx;
//		}
//		final ADDRNode ret = assignInt(
//			, assign, new_val, to_go, _tempUnaryCache, orders_int, cur_idx)
//	    }
//	    
//	    if( val != null ){
//		Map< String, Boolean > not_assigned_val = new HashMap< String, Boolean >( orders_int );
//		orders_int.remove( testVar );
//		
//		final ADDRNode sub_assign = val ? 
//			assignInt( input.getTrueChild(), assign, new_val, to_go-1 , _tempUnaryCache, not_assigned_val ) :
//			assignInt( input.getFalseChild(), assign, new_val, to_go-1 , _tempUnaryCache, not_assigned_val );
//		final ADDRNode ret = val ? makeINode(testVar, sub_assign, input.getFalseChild() ) :
//		    makeINode( testVar, input.getFalseChild(), sub_assign );
//		_tempUnaryCache.put(input, ret);
//		return ret;
//	    }
//	    //recurse necessary
//	    final ADDRNode true_assign = assignInt(input.getTrueChild(), assign, new_val, to_go, _tempUnaryCache, orders_int );
//	    final ADDRNode false_assign = assignInt( input.getFalseChild(), assign, new_val, to_go, _tempUnaryCache, orders_int );
//	    final ADDRNode ret = makeINode( testVar, true_assign, false_assign );
//	    _tempUnaryCache.put(input, ret );
//	    return ret;
//	}
////	}

	public ADDRNode get_path(final ADDRNode input,
			final NavigableMap<String, Boolean>... paths) {
		ADDRNode ret = DD_ZERO;
		for( final NavigableMap<String, Boolean> path : paths ){
			if( path != null ){
				ret = BDDUnion( ret, get_path_int( input, path ) );
			}
		}
		return ret;
	}

	private ADDRNode get_path_int(
			final ADDRNode input, final NavigableMap<String, Boolean> path) {
		ADDRNode ret = DD_ONE, cur = input;
		while( cur.getNode() instanceof ADDINode ){
			final String var = cur.getTestVariable();
			Boolean val = path.get(var);
			final ADDRNode cur_node = getIndicatorDiagram(var, val);
			ret = BDDIntersection(ret, cur_node);
			if( val ){
				cur = cur.getTrueChild();
			}else{
				cur = cur.getFalseChild();
			}
		}
		return ret;
	}

	public ADDRNode quantify(final ADDRNode input, 
			final Set<String> vars,
			final DDQuantify quantifier) {
		ADDRNode ret = input;
		//do bottom up to avoid recomputation
		for( int i = _ordering.size()-1; i >= 0 ; --i ){
			final String thisVar = _ordering.get(i);
			if( vars.contains( thisVar ) ) {
				ret = quantify(ret, thisVar, quantifier);
			}
		}
		return ret;
	}

//	public void clearNodes() {
//		madeLeaf = new ReferenceMap<ADDLeaf, ADDRNode>();
//		madeINodes = new TreeMap<String, 
//		ReferenceMap< ADDINode, ADDRNode>>();
//	}

}
