package add;

import graph.Graph;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import util.MySoftReference;
import util.Pair;
import util.UniPair;
import util.UnorderedPair;

import dd.DDManager;


public class ADDManager implements DDManager<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

	private static FileHandler allHandler;

	private static final String blanky = "".intern();

	private static final double BytesToMB = 1024*1024;

	private final static ConsoleHandler consoleHandler = new ConsoleHandler();

	private static ReferenceQueue<ADDRNode> deletedNodes = new ReferenceQueue<ADDRNode>();

	//all nodes - soft references - cache has to be cleaned up from reference queue
	//keep nodes around as long as possible
	//don't do this since ADDRNode might have a bad hash performance
	//	protected Map< Integer, MySoftReference< ADDRNode > > madeNodes 
	//		= new ConcurrentHashMap< Integer, MySoftReference< ADDRNode > >();

	private final static Logger LOGGER = Logger.getLogger(ADDManager.class.getName());

	protected static int STORE_INCREMENT;

	static{
		try {
			allHandler = new FileHandler("./log/" + ADDManager.class.getName());
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static DDOper getCompliment( DDOper op ){

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

	public static boolean isCommutative(DDOper op){

		if( op.equals(DDOper.ARITH_DIV) || op.equals(DDOper.ARITH_MINUS) ){
			return false;
		}

		return true;

	}

	public static void main(String[] args) {
		//		testAddPair();
		//		testgetINode();
		//		testIndicators();
		//		testApplyLeafOp();
		//		testGetRNode();
		//		testGraph();
		//		testClearDeadNodes((int)1e5);
		testApply();
//		testConstrain();
		//		testRestrict();
		//		testEnumeratePaths();
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

	public static void testAddPair(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

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

		ADDManager man = new ADDManager(500, ord);

		ADDRNode ind1 = man.getIndicatorDiagram("A", true);

		ADDRNode inodeA = man.getINode("A",  man.getLeaf(1d, 1d), man.getLeaf(2d, 2d) );

		ADDRNode inodeB = man.getINode("B",  man.getLeaf(3d, 3d), man.getLeaf(4d, 4d) );
		//
		//		ADDRNode inodeC = man.getINode("C",  man.getLeaf(9d, 10d), man.getLeaf(11d, 12d) );
		//		
		//		ADDRNode inodeD = man.getINode("D",  man.getLeaf(13d, 14d), man.getLeaf(15d, 16d) );
		//		
		//		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode prod = man.apply(inodeA, inodeB, DDOper.ARITH_PROD);

		ADDRNode indC = man.getIndicatorDiagram("C", true);

		ADDRNode prod2 = man.apply( prod, indC, DDOper.ARITH_PROD );

		man.showGraph(prod, indC, prod2);

		man.showGraph( man.apply( inodeA, inodeB, DDOper.ARITH_MINUS) );
	}

	public static void testApplyLeafOp(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(2d, 5d);

		ADDRNode leaf2 = man.getLeaf(4d, 3d);

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


	public static void testBernoulliConstraints(int nvars, double prob, 
			boolean disjoint, int num_disjoint ) {

		ArrayList<String> ord = new ArrayList<String>();

		for( int i = 0 ; i < nvars; ++i ){
			ord.add("X"+i);
		}

		if( disjoint ){
			for( int i = 0 ; i < num_disjoint ; ++i ){
				ord.add("X"+(nvars+i));	
			}
		}


		ADDManager man = new ADDManager(100000, ord);

		ADDRNode diag = man.getBernoulliProb(nvars, 0.75);

		man.showGraph(diag);
		
		ADDRNode constr = man.DD_ONE;

		int j = ( disjoint ) ? nvars : 0;

		for( int i = 0 ; i < num_disjoint ; ++i ){
			ADDRNode thisOne = man.getIndicatorDiagram("X"+(j+i), true );
			//			man.showGraph(thisOne);
			constr = man.apply(constr, thisOne, DDOper.ARITH_PROD);
			//			man.showGraph(constr);
		}

		System.out.println( man.countNodes(diag, constr) );

//		man.showGraph(diag, constr);

		Timer conTime = new Timer();

		ADDRNode coned = man.constrain(diag, constr, man.DD_ZERO);

		double cTime = conTime.GetTimeSoFarAndResetInMinutes();

		man.clearDeadNodes(true);

		conTime.ResetTimer();

		ADDRNode multd = man.apply(diag, constr, DDOper.ARITH_PROD );

		double multTime = conTime.GetTimeSoFarAndResetInMinutes();

		man.clearDeadNodes(true);

		System.out.println( man.countNodes(diag, coned, multd) + " " + cTime + " " + multTime );

		//		man.showGraph( coned, multd );


	}

	//	testApplyLeafOp
	//	testWhichBefore
	//	testgetInode
	//	

	public static void testClearDeadNodes(int num_nodes ){

		ArrayList<String> ord = new ArrayList<String>();

		for( char ch = 'A'; ch <= 'Z'; ++ch ){
			ord.add(ch+"");
			ord.add( Character.toLowerCase(ch) + "" );
		}

		System.out.println( ord );

		ADDManager man = new ADDManager(num_nodes, ord);

		ADDRNode old_leaf = man.getLeaf(0.0d , 0.0d);
		ADDRNode old_inode = null;

		int i = 0;

		ArrayList<ADDRNode> leaf_list = new ArrayList<ADDRNode>();

		ArrayList<ADDRNode> inode_list = new ArrayList<ADDRNode>();

		for( String chr : ord ){

			ADDRNode leaf = man.getLeaf(i+1.32, i+5.34);

			ADDRNode inode = null;

			if( old_inode == null ){
				inode = man.getINode(chr+"", leaf, old_leaf);	
			}else{
				inode = man.getINode(chr+"", leaf, old_inode);
			}


			leaf_list.add(leaf);

			//			man.showGraph(leaf, inode );
			//				
			//			if( old_inode != null ){
			//				man.showGraph(old_inode);
			//			}

			inode_list.add(inode);

			double memory = man.getMemoryPercent();

			man.memorySummary();

			man.makeSureNoNull();

			if( memory > 0.8 ){
				break;
			}

			old_inode = inode;

			++i;

			System.out.println(chr);

		}

		//		System.out.println(leaf_list);

		man.memorySummary();

		man.clearDeadNodes(true);

		man.memorySummary();

		int deleted = 0;

		for( ADDRNode rnode : leaf_list ){
			if( man.nullify(rnode) ){
				++deleted;
			}

		}

		for( ADDRNode rnode : inode_list ){
			if( man.nullify(rnode) ){
				++deleted;
			}
		}

		//		System.err.println( "deleted through nullify " + deleted );

		try {
			System.gc();
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		man.clearDeadNodes(true);

		man.memorySummary();

		man.memorySummary();

	}

	public static void testConstrain() {

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(1000, ord);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5, 5.5), man.getLeaf(4.3, 4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d,2d), man.getLeaf(7d, 7d) );

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

		ADDManager man = new ADDManager(1000, ord);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5, 5.5), man.getLeaf(4.3, 4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d,2d), man.getLeaf(7d, 7d) );

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

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph(inode2);

		System.out.println( man.getNodes(inode2) );

	}

	public static void testCountPaths() {

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("A");
		ord.add("B");
		ord.add("C");
		ord.add("D");

		ADDManager man = new ADDManager(1000, ord);

		ADDRNode inodeA = man.getINode("A", man.getLeaf(5.5, 5.5), man.getLeaf(4.3, 4.3) );

		ADDRNode inodeB = man.getINode("B", man.getLeaf(2d,2d), man.getLeaf(7d, 7d) );

		//		man.showGraph(inodeB);

		ADDRNode inodeAB = man.apply(inodeA, inodeB, DDOper.ARITH_PLUS);

		ADDRNode inodeABC = man.apply(inodeAB, man.getIndicatorDiagram("C", false), DDOper.ARITH_PROD);

		man.showGraph( inodeA, inodeB, inodeAB, inodeABC );

		System.out.println( man.countPaths( inodeABC ) );

	}

	public static void testgetINode(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(2d, 5d);

		ADDRNode leaf2 = man.getLeaf(4d, 3d);

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

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(2d, 2d);

		ADDRNode leaf2 = man.getLeaf(7d, 7d);

		man.showGraph(leaf1, leaf2);

	}

	public static void testGetRNode(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf = man.getLeaf(1.32, 5.34);

		ADDRNode leaf2 = man.getLeaf( 4.3, 4.3 );

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

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		//have to test this again with ordering
		ADDManager man = new ADDManager(100, ord);

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

	public static void testIndicators(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode a = man.getIndicatorDiagram("X", true);

		ADDRNode b = man.getIndicatorDiagram("X", false);

		man.showGraph(a,b);

	}

	public static String testRandomizedConstraints( int nvars, double prob, double constr_prob ){

		ArrayList<String> ord = new ArrayList<String>();

		for( int i = 0 ; i < nvars; ++i ){
			ord.add("X"+i);
		}

		ADDManager man = new ADDManager(100000, ord);

		ADDRNode diag = man.DD_ONE, constr = man.DD_ONE;

		Random rand = new Random( );


		for( int i = 0 ; i < nvars; ++i ){

			double r = rand.nextDouble();

			if( r < constr_prob ){
				ADDRNode thisOne = man.getIndicatorDiagram("X"+i, true);
				constr = man.apply( thisOne, constr, DDOper.ARITH_PROD);
			}

			r = rand.nextDouble();

			if( r < prob ){
				double v = rand.nextDouble();

				ADDRNode inode = man.getINode("X"+i, man.getLeaf(v, v), man.getLeaf(1-v, 1-v) );
				diag = man.apply( diag, inode, DDOper.ARITH_PROD );
			}

		}

		man.flushCaches(true);

		Timer conTime = new Timer();

		ADDRNode coned = man.constrain(diag, constr, man.DD_ZERO);

		double cTime = conTime.GetTimeSoFarAndResetInMinutes();

		man.flushCaches(true);

		conTime.ResetTimer();

		ADDRNode multd = man.apply(diag, constr, DDOper.ARITH_PROD );

		double multTime = conTime.GetTimeSoFarAndResetInMinutes();

		man.flushCaches(true);

		String str =  prob + " " + constr_prob + " " + man.countNodes(diag, constr) + " " +  
				man.countNodes(coned, multd) + " " + cTime + " " + multTime ;

		System.out.println( str );

		return str; 

	}

	public static void testRestrict(){

		ArrayList<String> ord = new ArrayList<String>();
		ord.add("X");
		ord.add("Y");
		ord.add("Z");
		ord.add("K");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf = man.getLeaf(1.32, 5.34);

		ADDRNode leaf2 = man.getLeaf( 4.3, 4.3 );

		ADDRNode inod1 = man.getINode("X", leaf, leaf2);

		ADDRNode leaf3 = man.getLeaf( 4.3, 6.5 );

		ADDRNode inod3 = man.getINode("X", leaf, leaf3);

		man.showGraph(inod1, inod3);

		ADDRNode rest1 = man.restrict(inod1, "X", true);

		ADDRNode rest2 = man.restrict(inod1, "X", false);

		man.showGraph(rest1, rest2);

		ADDRNode inod4 = man.getINode("K", inod1, leaf3);

		man.showGraph(inod4, man.restrict(inod4, "K", true), man.restrict(inod4, "K", false) );

	}

	private ConcurrentHashMap<ADDRNode, ADDRNode> _constrainCache;

	protected ArrayList< String > _ordering = null;

	protected Runtime _runtime = Runtime.getRuntime();

	//Done: 
	//1. draw diagrams from am iid distribution
	//2. measure old and new constraints

	//addnode -> addnode cache for general purpose - NOTE: cear after use always
	private ConcurrentHashMap<
	MySoftReference<ADDRNode>, MySoftReference<ADDRNode> > _tempCache 
	= new ConcurrentHashMap< MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>();

	//caches can be in terms of RNodes
	protected ConcurrentHashMap< DDOper, ConcurrentHashMap< 
	Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> >  , MySoftReference<ADDRNode> > > 
	applyCache = new ConcurrentHashMap< 
	DDOper, ConcurrentHashMap< Pair< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > >, 
	MySoftReference<ADDRNode> > > ();

	//next, getRandomADDs
	//constrain - with a cache -  as a method
	//use whichBefore()

	//compare taht with apply



	private int applyHit = 0;

	public ADDRNode DD_ZERO, DD_ONE, DD_NEG_INF;

	//done:
	//1. Write restrict operator
	//2. test restrict
	//3. write marginalize - max and sum

	protected ConcurrentHashMap< String,
	ConcurrentHashMap< Integer, MySoftReference<ADDRNode> > > madeINodes 
	= new ConcurrentHashMap< String, ConcurrentHashMap<Integer, MySoftReference<ADDRNode> > >();

	protected ConcurrentHashMap< Integer, MySoftReference<ADDRNode> > madeLeaf = new ConcurrentHashMap< Integer, MySoftReference<ADDRNode> >();

	//never deleted
	protected Set< ADDRNode > permenantNodes = Collections.newSetFromMap( 
			new ConcurrentHashMap< ADDRNode, Boolean >() );

	//Done : 
	//1. write constrain operator
	//2. compare with apply
	//3. generate from functions

	//we will be reducing only INodes 
	//using RNodes here just to deal with negated edges
	protected ConcurrentHashMap< MySoftReference< ADDRNode >, MySoftReference< ADDRNode > > reduceCache = 
			new ConcurrentHashMap<
			MySoftReference< ADDRNode >, MySoftReference< ADDRNode > >();

	//store of null nodes
	protected ConcurrentLinkedQueue< MySoftReference<ADDINode> > storeINodes 
	= new ConcurrentLinkedQueue< MySoftReference<ADDINode> >();

	protected ConcurrentLinkedQueue< MySoftReference<ADDLeaf> > storeLeaf 
	= new ConcurrentLinkedQueue< MySoftReference<ADDLeaf> >();
	public ADDManager( final int numDDs, ArrayList<String> ordering ){

		addToStore( numDDs, true, true );

		STORE_INCREMENT = numDDs;

		LOGGER.addHandler(allHandler);

		DD_ZERO = getLeaf(0.0d, 0.0d);

		DD_ONE = getLeaf(1.0d, 1.0d);

		DD_NEG_INF = getLeaf(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

		_ordering= ordering;
		
		addPermenant(DD_ZERO, DD_ONE, DD_NEG_INF);

	}

	//	protected ReferenceQueue< ADDINode > deletedINodes = new ReferenceQueue< ADDINode >();

	//	protected ReferenceQueue< ADDLeaf > deletedLeafNodes = new ReferenceQueue< ADDLeaf>();

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

	public static void testApplyCache(){
		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

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

		LOGGER.entering( this.getClass().getName(), "Add permenant");

		for( ADDRNode rnode : input ){
			boolean ret = permenantNodes.add( rnode );

			LOGGER.info(" adding permenant " + input + ". Was not already in set " + ret );
		}
		
		LOGGER.exiting( this.getClass().getName(),  "Add permenant");

	}
	
	@Override
	public NavigableMap<String, Boolean> findFirstOneLeaf(final ADDRNode input){
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
				}
			}
		}
		
		return action;
		
	}
	
	@Override
	public boolean removePermenant( final ADDRNode... input ){
		boolean ret = true;
		for( ADDRNode in : input ){
			ret = ret && permenantNodes.remove(in);
		}
		return ret;
	}

	public synchronized void addToApplyCache( ADDRNode a, ADDRNode b, DDOper op, ADDRNode res ){

		addPair(a,b,op,res);

		if( isCommutative(op) ){
			addPair(b, a, op, res);
		}

		DDOper compOp = getCompliment(op);

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
	public synchronized void addToStore(final int NumDDs, final boolean leaf, final boolean node) {

		LOGGER.entering( this.getClass().getName(), "addtostore" );

		if( leaf ){

			int i = 0;

			for( i = 0 ; i < NumDDs; ++i ){

				try{

					ADDLeaf aLeaf = new ADDLeaf();

					storeLeaf.add( aLeaf.getNullDD() );

				}catch(OutOfMemoryError e){

					LOGGER.severe("out of memory error. added " + i + " leaves." );
					e.printStackTrace();
					System.exit(1);
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
					e.printStackTrace();
					System.exit(1);
					return;

				}

			}	

		}

		LOGGER.info("added " + NumDDs + " leaves: " + leaf + " and nodes: " + node);


		LOGGER.exiting( this.getClass().getName(), "addtostore" );

	}

	private synchronized void addToTempCache(ADDRNode in, ADDRNode ret) {

		_tempCache.put( getSoftRef(in), getSoftRef(ret) );

	}

	
	//< thresh & strict ? 1: 0 
	//<= thresh & !strict ? 1 : 0
	public ADDRNode threshold( ADDRNode input, final double threshold, final boolean strict ){
		clearTempCache();
		ADDRNode ret = thresholdInt( input, threshold, strict );
		clearTempCache();
		return ret;
	}

	private double thresholdDouble( final double val , final double thresh, final boolean strict ){
		
		if( strict ){
			return val < thresh ? 1 : 0;
		}else{
			return val <= thresh ? 1 : 0;
		}
		
	}
	
	private ADDRNode thresholdInt( ADDRNode input, final double threshold, final boolean strict ){
	
		ADDNode node = input.getNode();
		
		if( node instanceof ADDLeaf ){
			
			ADDLeaf leaf = (ADDLeaf)node;
			Pair<Double, Double> value = leaf.getLeafValues(); 
			double val1 = thresholdDouble( value._o1, threshold, strict );
			double val2 = thresholdDouble( value._o2, threshold, strict );
			
			return getLeaf( val1, val2 );
			
		}else{
			
			ADDRNode ret = lookupTempCache(input);
			
			if( ret == null ){
				String var = input.getTestVariable();

				ADDRNode truth = thresholdInt( input.getTrueChild(), threshold, strict );
				ADDRNode falseth = thresholdInt( input.getFalseChild(), threshold, strict );
				
				ret = getINode( var, truth, falseth );
				addToTempCache(input, ret);
				
			}
			
			return ret;
			
		}
		
	}
	
	private void clearTempCache(){
		_tempCache.clear();
	}
	
	
	public ADDRNode apply(ADDRNode op1, ADDRNode op2,
			DDOper op) {

		boolean op1_neg_inf = op1.equals(DD_NEG_INF);
		boolean op2_neg_inf =  op2.equals(DD_NEG_INF);
		
		if( ( op1_neg_inf || op2_neg_inf ) && !op.equals(DDOper.ARITH_MAX) ){
			return DD_NEG_INF;
		}else if( op1_neg_inf || op2_neg_inf ){
			return op1_neg_inf ? op2 : op1;
		}
		//get the nodes
		//descend simultaneously until cache hit or leaf pair

		ADDNode node1 = op1.getNode();
		ADDNode node2 = op2.getNode();

		ADDRNode ret = null;

		if( node1 instanceof ADDLeaf && node2 instanceof ADDLeaf ){
			//we're done here
			ret = applyLeafOp( op1, op2, op);
			System.out.println("Leaf op : " + node1 + " " + op + " " + node2 + " = " + ret);
		}else{

			ADDRNode lookup = lookupPair(op1, op2, op);

			if( lookup != null ){
				++applyHit ;
				ret = lookup;	
			}else{

				if( node1 instanceof ADDINode && node2 instanceof ADDINode 
						&& isEqualVars(op1, op2) ){
					//check if equal test vars
					ADDRNode trueAns = apply( op1.getTrueChild(), op2.getTrueChild(), op );
					ADDRNode falseAns = apply( op1.getFalseChild(), op2.getFalseChild(), op);
					ret = makeINode(op1.getTestVariable(), trueAns, falseAns);
					addToApplyCache(op1, op2, op, ret);					

				}else{
					//unequal test vars
					//descend on one
					ADDRNode trueAns, falseAns;
					if( isBefore( op1, op2 ) ){//descend op1
						trueAns = apply( op1.getTrueChild(), op2, op );
						falseAns = apply( op1.getFalseChild(), op2, op );
						ret = makeINode( op1.getTestVariable(), trueAns, falseAns);
					}else{//descend op2
						trueAns = apply( op1, op2.getTrueChild(), op );
						falseAns = apply( op1, op2.getFalseChild(), op );
						ret = makeINode( op2.getTestVariable(), trueAns, falseAns);
					}
					addToApplyCache(op1, op2, op, ret);					
				}
			}
		}

		return ret;

	}

	public ADDRNode applyLeafOp(ADDRNode rnode1, ADDRNode rnode2, DDOper op) {

		ADDLeaf node1 = (ADDLeaf) rnode1.getNode();

		ADDLeaf node2 = (ADDLeaf) rnode2.getNode();

		Pair<Double, Double> leaf1 = node1.getLeafValues();

		Pair<Double, Double> leaf2 = node2.getLeafValues();

		ADDRNode ret = null;

		switch( op ){

		case ARITH_PLUS : 

			if( rnode1.equals(DD_ZERO) ){
				ret = rnode2;
			}else if( rnode2.equals(DD_ZERO) ){
				ret = rnode1;
			}else{
				ret = getLeaf(leaf1._o1 + leaf2._o1,  leaf1._o2 + leaf2._o2);
			}

			break;

		case ARITH_MINUS : 

			if( rnode2.equals(DD_ZERO) ){
				ret = rnode1;
			}else{
				ret = getLeaf(leaf1._o1 - leaf2._o1, leaf1._o2 - leaf2._o2);
			}

			break;

		case ARITH_PROD : 

			if( rnode1.equals(DD_ZERO) ){
				ret = DD_ZERO;
			}else if( rnode2.equals(DD_ZERO)){
				ret = DD_ZERO;
			}else if( rnode1.equals(DD_ONE) ){
				ret = rnode2;
			}else if( rnode2.equals(DD_ONE) ){
				ret = rnode1;
			}else{
				ret = getLeaf( leaf1._o1 * leaf2._o1, leaf1._o2 * leaf2._o2);	
			}


			break;

		case ARITH_DIV:

			if( rnode1.equals(DD_ZERO) ){
				ret = DD_ZERO;
			}else if( rnode2.equals(DD_ONE) ){
				ret = rnode1;
			}

			try{
				ret = getLeaf(leaf1._o1 / leaf2._o1, leaf1._o2 / leaf2._o2);
			}catch( ArithmeticException e ){
				e.printStackTrace();
				System.exit(1);
			}

			break;

		case ARITH_MAX :

			ret = getLeaf( Math.max(leaf1._o1, leaf2._o1), Math.max(leaf1._o2, leaf2._o2) );

			break;

		case ARITH_MIN :

			ret = getLeaf( Math.min(leaf1._o1, leaf2._o1), Math.min(leaf1._o2, leaf2._o2) );

			break;

		default : 	
			System.err.println("unknown operation");
			System.exit(1);
		}

		return ret;

	}

	@Override
	public ADDRNode approximate(ADDRNode input, double epsilon, APPROX_TYPE approx_type) {
		try{
			throw new UnsupportedOperationException("APRICODD not yet implemented");
		}catch( UnsupportedOperationException e ){
			e.printStackTrace();
		}
		return null;
	} 

	public synchronized void clearApplyCache(){

		Set<Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, 
		MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>>> maps = applyCache.entrySet();

		Iterator<Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>,
		MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>>> it = maps.iterator();

		while( it.hasNext() ){

			Entry<DDOper, ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, 
			MySoftReference<ADDRNode>>, MySoftReference<ADDRNode>>> inner = it.next();

			ConcurrentHashMap<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
			MySoftReference<ADDRNode>> innerMap = inner.getValue();

			Set<Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
			MySoftReference<ADDRNode>>> innerSet = innerMap.entrySet();

			Iterator<Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>,
			MySoftReference<ADDRNode>>> itIn = innerSet.iterator();

			while( itIn.hasNext() ){

				Entry<Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>>, 
				MySoftReference<ADDRNode>> thing = itIn.next();

				Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> key = thing.getKey();
				MySoftReference<ADDRNode> value = thing.getValue();

				if( value == null || value.get() == null 
						|| key._o1 == null || key._o2 == null
						|| key._o1.get() == null || key._o2.get() == null ){

					itIn.remove();

				}

			}

		}

	}

	public synchronized
	<T extends Comparable<T>, U>  void clearDeadEntries( ConcurrentHashMap<U, MySoftReference<T> > map ){

		Set<Entry<U,MySoftReference<T>>> set = map.entrySet();

		Iterator<Entry<U, MySoftReference<T>>> it = set.iterator();

		while( it.hasNext() ){

			Entry<U, MySoftReference<T>> thing = it.next();

			U key = thing.getKey();

			MySoftReference<T> value = thing.getValue();

			if( value == null || value.get() == null ){
				it.remove();
			}

		}

	}

	public synchronized void clearDeadNodes(boolean clearDeadMaps) {

		//take references from ref queue
		//get the object in it
		//remove it from madeINodes or madeLeafs
		//by using nullify()

		Reference<? extends ADDRNode> item;

		int total = 0, deleted = 0;

		while( (item = deletedNodes.poll()) != null ){

			ADDRNode rnode = item.get();

			if(  rnode != null  ){

				if( !permenantNodes.contains(rnode) ){

					ADDNode node = rnode.getNode();

					int hash = node.hashCode();

					if( node instanceof ADDINode ){

						//note : made the change to madeINodes to a bucket map ordered by test variable
						//so do that here as well
						//also look if permenant first, perhaps?

						ADDINode inode = (ADDINode)node;

						ConcurrentHashMap<Integer, MySoftReference<ADDRNode>> innerMap 
						= madeINodes.get( inode.getTestVariable() );

						Object paulie = this.lookupMap(hash, innerMap );

						removeMap( hash, innerMap );

					}else if( node instanceof ADDLeaf ){

						removeMap( hash, madeLeaf );

					}

					++deleted;

				}

			}

			++total;

		}

		//		System.err.println( "Total: " + total + " Deleted : " + deleted );

		if( clearDeadMaps ){
			clearMadeNodes();
			clearApplyCache();	
		}


	}

	private synchronized void clearMadeNodes() {

		Set<Entry<String, ConcurrentHashMap<Integer, MySoftReference<ADDRNode>>>> map = madeINodes.entrySet();

		Iterator<Entry<String, ConcurrentHashMap<Integer, MySoftReference<ADDRNode>>>> itOut = map.iterator();

		while( itOut.hasNext() ){

			Entry<String, ConcurrentHashMap<Integer, MySoftReference<ADDRNode>>> inner = itOut.next();

			ConcurrentHashMap<Integer, MySoftReference<ADDRNode>> innerMap = inner.getValue();

			clearDeadEntries(innerMap);

		}

		clearDeadEntries(madeLeaf);

	}


	//done:
	//1. maps of Inode and Dnode separately
	//2. method for Inode, Dnode to RNode(must check for negation)
	//3. soft references to Rnodes
	//4. update clear null entries etc

	@Override
	public boolean compare(ADDRNode input1, ADDRNode input2) {

		ADDRNode diff = apply( input1, input2, DDOper.ARITH_MINUS );

		if( diff.equals(DD_ZERO) ){
			return true;
		}

		return false;

	}

	public ADDRNode constrain( ADDRNode rnode, ADDRNode rconstrain, ADDRNode violate ){

		if( _constrainCache == null ){
			_constrainCache = new ConcurrentHashMap<ADDRNode, ADDRNode>();
		}

		_constrainCache.clear();
		ADDRNode ret = constrainInt(rnode, rconstrain, violate);
		_constrainCache.clear();
		return ret;
	}

	//constraint diagram is a BDD
	//replaces constrained parts of rnode with violate
	public ADDRNode constrainInt( ADDRNode rnode, ADDRNode rconstrain, ADDRNode violate ){

		ADDNode node = rnode.getNode();
		ADDNode constr = rconstrain.getNode();
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
			ADDRNode lookup = _constrainCache.get( rnode );
			if( lookup != null ){
				ret = lookup;
			}else{

				//not both leaf
				if( node instanceof ADDINode && constr instanceof ADDINode ){
					ADDRNode trueAns, falseAns;
					
					if( isEqualVars(rnode, rconstrain) ){
//						System.out.println("descend both " + rnode.getTestVariable());
//						System.out.println( rnode.getTestVariable() + " true " );
						trueAns = constrainInt(rnode.getTrueChild(), 
								rconstrain.getTrueChild(), violate);
//						System.out.println( rnode.getTestVariable() + " false " );
						falseAns = constrainInt( rnode.getFalseChild(), 
								rconstrain.getFalseChild(), violate );
						ADDRNode ans = makeINode(rnode.getTestVariable(), trueAns, falseAns);
						ret = ans;
					}else{
						//descend just one
						//one inode at least
						if( isBefore(rnode, rconstrain) ){//descend on rnode
//							System.out.println("descend rnode " + rnode.getTestVariable());
//							System.out.println( rnode.getTestVariable() + " true " );
							trueAns = constrainInt(rnode.getTrueChild(), 
									rconstrain, violate);
//							System.out.println( rnode.getTestVariable() + " false " );
							falseAns = constrainInt( rnode.getFalseChild(), 
									rconstrain, violate );
							ADDRNode ans = makeINode(rnode.getTestVariable(), trueAns, falseAns);
							ret = ans;
						}else{
							//descend on rconstrain
							//here is the approximation
							//i will always descend on false brach of constrain
							//this aboid adding the constrain variable
							
							//i want to apply only those constraints that are common to 
							//both branches
							//1. take sub-bdds... multiple (1-subbdds)
							//will have 1 in common constraints
							//do 1-that to get common constraints
							//presuming that the constraint is compact
							//multiplying the subdds is ok
							ADDRNode common = getCommonConstraints( rconstrain );
							ADDRNode ans = constrainInt( rnode, common, violate);
							ret = ans;
						}
					}
				}else{
					try{
						throw new Exception("should not be here");
					}catch( Exception e ){
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
//		showGraph(rnode, rconstrain, ret);
		_constrainCache.put( rnode, ret );
		return ret;
	}

	//WARNING: constrain should be a BDD
	private ADDRNode getCommonConstraints(ADDRNode rconstrain) {
		ADDRNode truth = rconstrain.getTrueChild();
		ADDRNode falseth = rconstrain.getFalseChild();
		ADDRNode one_minus_truth = apply( DD_ONE, truth, DDOper.ARITH_MINUS);
		ADDRNode one_minus_falseth = apply( DD_ONE, falseth, DDOper.ARITH_MINUS);
		ADDRNode common_one = apply( one_minus_falseth, one_minus_truth, DDOper.ARITH_PROD );
		return apply( DD_ONE, common_one, DDOper.ARITH_MINUS );
	}

	public ArrayList<Integer> countNodes( ADDRNode... rnodes ){

		int i = 0;

		ArrayList<Integer> ret = new ArrayList<Integer>();

		for( ADDRNode rnode : rnodes ){
			ret.add( countNodesInt( rnode , new HashSet<ADDRNode>() ) );
		}

		return ret; 
	}

	private int countNodesInt( ADDRNode rnode, HashSet<ADDRNode> seen ){

		if( seen.contains(rnode) ){
			return 0;
		}

		ADDNode node = rnode.getNode();

		if( node instanceof ADDLeaf ){
			return 0;
		}

		seen.add( rnode );

		int trueCount = countNodesInt( rnode.getTrueChild(), seen );
		int falseCount = countNodesInt( rnode.getFalseChild(), seen );

		return 1 + trueCount + falseCount;

	}

	public int countPaths( ADDRNode rnode ){

		ADDNode node = rnode.getNode();

		if( node instanceof ADDLeaf ){
			return 1;
		}

		int trueCount = countPaths( rnode.getTrueChild() );
		int falseCount = countPaths( rnode.getFalseChild() );

		return trueCount + falseCount;

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
	public List<NavigableMap<String,Boolean>> enumeratePaths(ADDRNode input, boolean leaf) {

		List<NavigableMap<String, Boolean>> ret = new ArrayList< NavigableMap<String, Boolean> > () ;

		enumeratePathsInt( input, ret, null, leaf);

		return ret;

	}

	public static void testEnumeratePaths( ){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph(inode2);

		System.out.println( man.enumeratePaths(inode2, true) );

	}


	private void enumeratePathsInt(ADDRNode input,
			List<NavigableMap<String, Boolean>> ret, NavigableMap<String,Boolean> current, boolean leaf ) {

		if( current == null ){
			//root node
			current = new TreeMap<String, Boolean>();
		}

		ADDNode node = input.getNode();

		if( node instanceof ADDLeaf && current.size() > 0 ){
			
			if( leaf ){
				current.put( node.toString(), false) ;
			}
			
			ret.add( current );
		}else{

			TreeMap<String, Boolean> truth = new TreeMap<String, Boolean>(current);
			truth.put( input.getTestVariable(), true );

			enumeratePathsInt(input.getTrueChild(), ret, truth, leaf);

			TreeMap<String, Boolean> falseth = new TreeMap<String, Boolean>(current);
			falseth.put( input.getTestVariable(), false );

			enumeratePathsInt( input.getFalseChild(), ret, falseth, leaf );

		}

	}

	@Override
	public ADDRNode evaluate(ADDRNode input, Map<?, Boolean> assign) {

		ADDRNode ret = input;

		while( !( ret.getNode() instanceof ADDLeaf ) ){
			String testVar = ret.getTestVariable();
			boolean value = assign.get( testVar );
			ret = ( value ) ?  ret.getTrueChild() : ret.getFalseChild();
		}

		return ret;

	}

	public static void testEvaluate(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		man.showGraph(inode2);

		TreeMap<String, Boolean> assign = new TreeMap<String, Boolean>();

		assign.put( "X", true );

		man.showGraph( man.evaluate(inode2, assign) );

	}

	@Override
	public void flushCaches(boolean clearDeadMaps) {

		this._tempCache.clear();

		applyCache.clear();

		reduceCache.clear();

		clearDeadNodes(clearDeadMaps);

	}

	//testaddPair
	//testlookuppair

	public ADDRNode getBernoulliProb( int num_vars, double prob ){

		ArrayList<String> ord = new ArrayList<String>();
		for( int i = 0 ; i < num_vars; ++i ){
			ord.add("X"+i);
		}

		List<ADDRNode> Pis = new ArrayList<ADDRNode>();

		for( int i = 0 ; i < num_vars; ++i ){
			ADDRNode thisOne = getINode("X"+i, getLeaf(prob, prob), getLeaf(1-prob, 1-prob) );
			Pis.add( thisOne );
		}

		ADDRNode res = DD_ONE;

		for( ADDRNode thing : Pis ){
			res = apply( res, thing, DDOper.ARITH_PROD );
			//			man.showGraph(res);
		}

		memorySummary();

		//		man.showGraph(res);

		return res;


	}

	private synchronized <T extends Comparable<T> > T getFirstRealOne(
			ConcurrentLinkedQueue<MySoftReference<T>> store) {

		MySoftReference<T> loc = null;

		while( (loc = store.poll()) != null ){

			T got = loc.get();
			if( got != null ){
				return got;
			}
		}

		return null;

	}

	public ADDRNode getIndicatorDiagram(String testVar, boolean b) {

		ADDINode inode = (ADDINode) getOneNullDD(false);

		if( inode == null ){
			System.err.println("can't make more inodes");
		}

		ADDINode ret = null;
		//plug in
		try {

			if( b ){
				ret = inode.plugIn( testVar, new UniPair<ADDRNode>( DD_ONE, DD_ZERO ) );
			}else{
				ret = inode.plugIn( testVar, new UniPair<ADDRNode>( DD_ZERO, DD_ONE ) );
			}

		} catch (Exception e) {
			ret = null;
			//			e.printStackTrace();
		}

		return getRNode(ret, true);

	}
	//creates new null inode
	//plugs into that inode
	//get Rnode - gets unique version of that node
	//returns falsebranch if truebranch.equals(falsebranch)
	//WARNING: assumes truebranch and falsebranch are reduced
	public ADDRNode getINode( String testVar, ADDRNode trueBranch, ADDRNode falseBranch ){

		//if you have RNode it should be that
		//under that node
		//no true = false
		//for inodes under

		//obvious reduction

		if( trueBranch == null || falseBranch == null ){
			return null;
		}

		//similarly, create=true here0

		if( trueBranch.equals(falseBranch) ){
			return trueBranch;
		}

		ADDNode trueNode = trueBranch.getNode() ;
		ADDNode falseNode = falseBranch.getNode();

		ADDRNode ret = null;

		if( trueNode instanceof ADDLeaf && falseNode instanceof ADDLeaf ){
			ret = makeINode( testVar, trueBranch, falseBranch );
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
				ADDRNode trueComp = getIndicatorDiagram( testVar, true );
				true_br = apply( trueComp, trueBranch, DDOper.ARITH_PROD );
			}

			ADDRNode false_br = null;
			if( falseBranch.equals(DD_NEG_INF) ){
				false_br = makeINode(testVar, DD_ZERO, DD_NEG_INF);
			}else{
				ADDRNode falseComp = getIndicatorDiagram( testVar, false );
				false_br = apply( falseComp, falseBranch, DDOper.ARITH_PROD );
			}

			if( true_br.equals(DD_NEG_INF) || false_br.equals(DD_NEG_INF) ){
				System.err.println("should not be here... apply add with NEG_INF");
				System.exit(1);
			}
			ret = apply( true_br, false_br, DDOper.ARITH_PLUS );

		}


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
	public ADDRNode getLeaf( Double low, Double high ){

		//getLeaf assumes create=true
		//for getRNode
		//if you want just lookup, us getRNode and get the getNode()

		ADDLeaf leaf = (ADDLeaf) getOneNullDD(true);

		if( leaf == null ){
			System.err.println("can't make more leaves");
			memorySummary();
			//			System.exit(1);
		}

		ADDLeaf ret = null;

		try{
			ret = leaf.plugIn( new Pair<Double, Double>(low, high) );
		}catch(NullPointerException e){
			ret = null;
		}



		return getRNode( ret, true );

	}

	@Override
	public Set<ADDLeaf> getLeaves(ADDRNode input) {
		TreeSet<ADDLeaf> ret = new TreeSet<ADDLeaf>();

		getLeavesInt( input, ret, new TreeSet<ADDRNode>() );

		return ret;

	}

	public static void testGetLeaves(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

		ADDRNode inode = man.getINode("X", leaf1, leaf2);

		ADDRNode inode2 = man.getINode("Y", inode, man.DD_ZERO);

		System.out.println( man.getLeaves( inode2 ) );


	}

	private void getLeavesInt(ADDRNode input, TreeSet<ADDLeaf> leaves,
			TreeSet<ADDRNode> visited ) {

		if( visited.contains(input) ){
			return;
		}

		ADDNode node = input.getNode();

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
	public Set<ADDRNode> getNodes(ADDRNode input) {

		TreeSet<ADDRNode> ret = new TreeSet<ADDRNode>();

		getNodesInt( input, ret );

		return ret;

	}

	private void getNodesInt(ADDRNode input, TreeSet<ADDRNode> nodes ) {

		ADDNode node = input.getNode();

		if( node instanceof ADDLeaf ){
			return;
		}

		if( nodes.contains(input) ){
			return;
		}

		nodes.add( input );

		getNodesInt( input.getTrueChild(), nodes);

		getNodesInt( input.getFalseChild(), nodes);


	}

	private int getNumINodes() {
		int tot  = 0;
		for( ConcurrentHashMap<Integer,MySoftReference<ADDRNode>> maps : madeINodes.values() ){
			tot += maps.size();
		}
		return tot;
	}

	@Override
	public ADDNode getOneNullDD( boolean leaf ) {


		LOGGER.entering( this.getClass().getName(), "getOneNullDD");

		ADDNode ret = null;

		if( leaf ){

			//try
			ADDLeaf aLeaf = getFirstRealOne(storeLeaf);

			if( aLeaf == null ){
				//try adding 
				try{
					LOGGER.info("store has no more leaves. Adding " + STORE_INCREMENT + " leaves ");
					memorySummary();
					addToStore(STORE_INCREMENT, true, false);
					aLeaf = getFirstRealOne(storeLeaf);
					ret = aLeaf;
				}catch(OutOfMemoryError e){
					ret = null;
					System.err.println( "can't make more leaves" );
				}
			}else{
				ret = aLeaf;
			}

		}else{

			ADDINode inode = getFirstRealOne( storeINodes );

			if( inode == null ){
				//try adding
				try{
					addToStore(STORE_INCREMENT, false, true);
					LOGGER.severe("store has no more nodes. Adding " + STORE_INCREMENT + " nodes ");
					inode = getFirstRealOne(storeINodes);
					ret = inode;
					memorySummary();
				}catch( OutOfMemoryError e ){
					ret = null;
					System.err.println("cant create more inodes");
				}
			}else{
				ret = inode;
			}

		}

		LOGGER.exiting(this.getClass().getName(), "getOneNullDD");

		if( ret == null ){
			System.err.println("getonenulld retruning null...");
			//			memorySummary();
			//			System.exit(1);
		}

		return ret;

	}

	//main methods of getting RNode from INode and Leaf
	//gets the canonical copy - including negated edges
	public <T extends ADDNode> ADDRNode getRNode( T obj, boolean create ){

		if( obj == null ){
			return null;
		}

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

				if( looked == null && create ){

					looked = new ADDRNode(inode);

					//using the reference queue is important here
					//because cleardeadnodes uses this function
					//with create = false

					lookin.put( inode.hashCode(), new MySoftReference<ADDRNode>(looked, deletedNodes) );

				}else{
					// negated node exists in memory
					// return with a not sign
					//if create=false and looked=null

					if( looked != null ){
						looked = looked.getNegatedNode();					
					}

				}

			}

		}else if( obj instanceof ADDLeaf ){

			ADDLeaf leaf = (ADDLeaf)obj;

			looked = lookupMap(hash, madeLeaf);

			if( looked == null ){

				looked = new ADDRNode(leaf);

				madeLeaf.put(hash, new MySoftReference<ADDRNode>(looked, deletedNodes) );

			}

		}

		return looked;

	}

	public <T extends Comparable<T> > MySoftReference<T> getSoftRef(T obj){

		//obj should have been created here
		return new MySoftReference<T>( obj );

	}

	@Override
	public Set<String> getVars(ADDRNode input) {

		TreeSet<ADDRNode> seen = new TreeSet<ADDRNode>();

		TreeSet<String> vars = new TreeSet<String>();

		getVarsInt( input, seen, vars);

		return vars;

	}

	private void getVarsInt(ADDRNode input, TreeSet<ADDRNode> visited, Set<String> vars ) {

		ADDNode node = input.getNode();

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

	private boolean isEqualVars(ADDRNode node1, ADDRNode node2) {
		return node1.getTestVariable() == node2.getTestVariable();//interned , can use ==
	}

	public ADDRNode lookupApplyCache( ADDRNode a, ADDRNode b, DDOper op ){

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
	public <T extends Comparable<T>> T lookupMap( final int hash, Map<Integer,MySoftReference<T>> aMap ){

		MySoftReference<T> thing = aMap.get(hash);

		T gotten = null;

		try{
			gotten = thing.get();
		}catch( NullPointerException e ){
			gotten = null;
		}

		if( thing != null && gotten != null ){

			return gotten;

		}else{

			//bookkeeping 
			if( thing != null && gotten == null ){
				aMap.remove(hash);
			}

			return null;

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

	private ADDRNode lookupTempCache(ADDRNode in) {

		MySoftReference<ADDRNode> looked = _tempCache.get( getSoftRef(in) );
		if( looked != null && looked.get() != null ){
			return looked.get();
		}
		return null;

	}

	//this methoid just constructs the inode
	//does nothing about reduction
	//use getINode 
	private ADDRNode makeINode(String testVariable, ADDRNode trueBranch,
			ADDRNode falseBranch) {

		if( trueBranch.equals(falseBranch) ){
			return trueBranch;
		}

		ADDINode inode = (ADDINode) getOneNullDD(false);
		try {
			inode = inode.plugIn(testVariable, new UniPair<ADDRNode>(trueBranch, falseBranch));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		ADDRNode ret = getRNode(inode, true);

		return ret;

	}

	private void makeSureNoNull() {

		for( ConcurrentHashMap<Integer,MySoftReference<ADDRNode>> setInodes  : madeINodes.values() ){ 
			for( MySoftReference<ADDRNode> ref : setInodes.values() ){
				if( ref == null || ref.get() == null ){
					System.err.println("oops... null madeinode");
					System.exit(1);
				}
			}
		}

		for( MySoftReference<ADDRNode> ref : madeLeaf.values() ){
			if( ref == null || ref.get() == null ){
				System.err.println("oops... null madeinode");
				System.exit(1);
			}
		}

		for( ConcurrentHashMap< 
				Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> > 
		, MySoftReference<ADDRNode> > maps : applyCache.values() ){

			for( Map.Entry<Pair< MySoftReference<ADDRNode>,  MySoftReference<ADDRNode> > 
			, MySoftReference<ADDRNode> > entry : maps.entrySet() ){

				Pair<MySoftReference<ADDRNode>, MySoftReference<ADDRNode>> key = entry.getKey();
				MySoftReference<ADDRNode> value = entry.getValue();

				if( value == null || value.get() == null 
						|| key._o1 == null || key._o2 == null
						|| key._o1.get() == null || key._o2.get() == null ){

					try {
						throw new Exception("null entry in apply?");
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}

				}	

			}


		}



	}
	
	public static void testMarginalize( ){
		
		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Z");

		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

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
	public ADDRNode marginalize(ADDRNode input, final String var, DDMarginalize oper) {

		ADDRNode truth = restrict(input, var, true);
		ADDRNode falseth = restrict( input, var, false );

		ADDRNode ret = null;

		if( oper.equals(DDMarginalize.MARGINALIZE_SUM) ){
			ret = apply( truth, falseth, DDOper.ARITH_PLUS);
		}else if( oper.equals(DDMarginalize.MARGINALIZE_MAX) ){
			ret =  apply( truth, falseth, DDOper.ARITH_MAX );
		}else{
			System.err.println("unknown marginalize oper " + oper );
			System.exit(1);
		}

		return ret;

	}

	public void memorySummary(){

		System.out.println( getNumINodes() + " Inodes " );
		System.out.println( madeLeaf.size() + " leaves" );

		System.out.println("memory : " + getMemoryPercent() );
		System.out.println( getMemoryMBs() );

	}

	private boolean nullify(ADDRNode rnode) {

		//recurse?

		ADDNode node = rnode.getNode();

		int hash = node.hashCode();

		boolean deleted = false;

		if( node instanceof ADDLeaf ){

			MySoftReference<ADDRNode> ret = madeLeaf.remove( hash );

			if( ret == null ){
				deleted = false;
			}else{
				deleted = true;
			}

		}else{
			String testvar = ((ADDINode)node).getTestVariable();
			ConcurrentHashMap<Integer, MySoftReference<ADDRNode>> thing = madeINodes.get(testvar);

			MySoftReference<ADDRNode> ret = thing.remove(hash);

			if( ret == null ){
				deleted = false;
			}else{
				deleted = true;
			}

		}

		rnode.nullify();

		return deleted;

	}

	@Override
	public void nullifyDD(ADDNode input) {

		LOGGER.entering( this.getClass().getName(), "nullifyDD");

		input.nullify();

		input = null;

		LOGGER.exiting( this.getClass().getName(), "nullifyDD");

	}

	public static void testRemapVars(){

		ArrayList<String> ord = new ArrayList<String>();

		ord.add("X");
		ord.add("Y");
		ord.add("Y'");
		ord.add("X'");
		
		ADDManager man = new ADDManager(100, ord);

		ADDRNode leaf1 = man.getLeaf(3d, 5d);

		ADDRNode leaf2 = man.getLeaf(6d, 2d);

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
	public ADDRNode scalarMultiply(ADDRNode input, double scalar) {

		return apply( input, getLeaf(scalar, scalar), DDOper.ARITH_PROD );
		
	}

	@Override
	public ADDRNode remapVars(ADDRNode input, Map<String, String> remap) {

		ADDNode node = input.getNode();

		if( node instanceof ADDLeaf ){
			return input;
		}

		String var = input.getTestVariable();

		String newVar = remap.get( var );

		ADDRNode truth = input.getTrueChild();

		ADDRNode falseth = input.getFalseChild();//negation taken care of

		ADDRNode remap_true = remapVars( truth , remap );

		ADDRNode remap_false = remapVars( falseth , remap );

		return getINode(newVar, remap_true, remap_false);

	}

	public synchronized <T extends Comparable<T>> void 
	removeMap( final int hash, Map<Integer,MySoftReference<T>> aMap ){

		MySoftReference<T> thing = aMap.get(hash);

		T gotten = null;

		try{
			gotten = thing.get();
		}catch( NullPointerException e ){
			gotten = null;
		}

		if( thing != null && gotten != null ){

			System.err.println( "removed " );

			aMap.remove(hash);

		}else if( thing == null ){

			try {
				throw new Exception("removeMap did not find item");
			} catch (Exception e) {
				e.printStackTrace();
			}


		}else{

			//also, bookkeeping 
			if( thing != null && gotten == null ){
				aMap.remove(hash);
			}

		}

	}

	@Override
	public ADDRNode restrict(ADDRNode input, String var, boolean assign) {

		if( input.getNode() instanceof ADDLeaf ){
			return input;
		}
		
		int index = _ordering.indexOf(var);
		if( index == -1 ){
			System.err.println("var " + var + " could not be found in ordering " + _ordering );
			System.exit(1);
		}


		_tempCache.clear();

		return restrictInt( input, var, assign, index );

	}

	private ADDRNode restrictInt( final ADDRNode in, final String var, final boolean assign, 
			final int index ){
		
		if( in.getNode() instanceof ADDLeaf ){
			if( !in.equals(DD_NEG_INF) ){
				System.out.println("not neg inf leaf");
			}
			return in;
		}

		ADDINode node = (ADDINode)(in.getNode());
		String testVar = node.getTestVariable();
		int curIndex = _ordering.indexOf(testVar);
		ADDRNode ret = null;

		if( curIndex == index ){

			ret = ( assign ) ? in.getTrueChild() : in.getFalseChild();
			addToTempCache( in, ret );

		}else if( curIndex < index ){

			ADDRNode looked = lookupTempCache( in );
			if( looked != null ){
				ret = looked;
			}else{
				//recurse
				ADDRNode true_restrict = restrictInt( in.getTrueChild(), var, assign, index);
				ADDRNode false_restrict = restrictInt(in.getFalseChild(), var, assign, index);
				ret = getINode(in.getTestVariable(), true_restrict, false_restrict);
				addToTempCache( in, ret );
			}

		}else{

			ret = in;
			addToTempCache( in, ret );

		}

		return ret;

	}

	//graph vieweiing works
	//getRNode works w00t!
	//
	public void showGraph(ADDRNode... nodes) {

		for( ADDRNode node : nodes ){
			Graph g = new Graph(true, false, false, false);
			node.toGraph(g);
			g.launchViewer();
		}

	}

	private boolean isBefore(final ADDRNode rnode1, final ADDRNode rnode2 ) {

		ADDNode node1 = rnode1.getNode();
		ADDNode node2 = rnode2.getNode();
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
			String var1 = rnode1.getTestVariable();
			String var2 = rnode2.getTestVariable();
			int ind1 = _ordering.indexOf( var1 );
			int ind2 = _ordering.indexOf( var2 );

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

	public double getNegativeInfValue() {
		return Double.NEGATIVE_INFINITY;
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