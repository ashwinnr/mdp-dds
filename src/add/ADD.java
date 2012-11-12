package add;

import java.util.Map;
import java.util.Set;

import util.MySoftReference;
import util.Pair;
import dd.DD;
import dd.DD.DDOper;
import dd.NodeOperation;

public class ADD implements DD<ADDNode, ADDRNode, ADDINode, ADDLeaf, ADDManager> {

	protected final int STORE_INIT_SIZE = 10000;
	
	protected ADDManager memoryMan;
	
	public ADD(){
		
		memoryMan = new ADDManager( STORE_INIT_SIZE );
		
	}
	
	
	@Override
	public ADDRNode reduce(ADDRNode input) {

		//check if leaf node
		ADDNode theNode = input.getNode();
		
		if( theNode instanceof ADDLeaf ){
			//negated leaf does not make sense
			if( input.isNegated() ){
				
				System.err.println("here is a leaf that is negated");
				System.exit(1);
				
			}
			
			return input;
			
		}
		
		
		//check cache
		memoryMan.looku
		
		//recursively reduce using reduce operator
		//ie
		//if low  == high, return reduce(low)
		//reduce low and high
		//getINode(v, low, high)
		//add to c
		
	}

	@Override
	public ADDRNode apply(ADDRNode inputA, ADDRNode inputB, DDOper oper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode opOut(ADDRNode input, dd.DD.DDOper oper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode approximate(ADDRNode input, double epsilon,
			dd.DD.APPROX_TYPE type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode restrict(ADDRNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode remapVars(ADDRNode input, Map<?, ?> remap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode evaluate(ADDRNode input, Map<?, Boolean> remap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode scalarOp(ADDRNode input, dd.DD.DDOper op, double scalar) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<Integer, Integer> countLeaves(ADDRNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getVars(ADDRNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showTable(ADDRNode input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ADDRNode enumeratePaths(ADDRNode input, NodeOperation<ADDLeaf> op) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean compare(ADDRNode input1, ADDRNode input2,
			NodeOperation<ADDLeaf> op) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ADDRNode doUnaryOp(ADDRNode input,
			NodeOperation<ADDINode> interiorNodeOp,
			NodeOperation<ADDLeaf> leafOp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDRNode doBinaryOp(ADDRNode inputA, ADDRNode inputB,
			NodeOperation<ADDINode> interiorNodeOp,
			NodeOperation<ADDLeaf> leafOp) {
		// TODO Auto-generated method stub
		return null;
	}

}
