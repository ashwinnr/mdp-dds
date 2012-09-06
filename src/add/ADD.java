package add;

import java.util.Map;
import java.util.Set;

import util.Pair;
import dd.DD;
import dd.DD.DDOper;
import dd.NodeOperation;

public class ADD implements DD<ADDNode, ADDRNode, ADDManager> {

	
	protected static int LEAF_PRECISION_NUM_DIGITS;
	
	public static DDOper getComplement( DDOper op ){
		
		DDOper ret = null;
		
		switch( op ){
			case ARITH_DIV: ret = DDOper.ARITH_PROD; break;
			case ARITH_PROD: ret = DDOper.ARITH_DIV; break;
			case ARITH_MINUS: ret = DDOper.ARITH_PLUS; break;
			case ARITH_PLUS: ret = DDOper.ARITH_MINUS; break;
		}
		
		return ret;
		
	}
	
	public static boolean isCommutative(DDOper op){
		
		if( op.equals(DDOper.ARITH_DIV) || op.equals(DDOper.ARITH_MINUS) ){
			return false;
		}
		
		return true;
		
	}

	@Override
	public ADDRNode reduce(ADDRNode input) {
		
	}

	@Override
	public ADDNode apply(ADDNode inputA, ADDNode inputB, dd.DD.DDOper oper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode opOut(ADDNode input, dd.DD.DDOper oper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode approximate(ADDNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode restrict(ADDNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode remapVars(ADDNode input, Map<?, ?> remap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode evaluate(ADDNode input, Map<?, Boolean> remap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ADDNode scalarOp(ADDNode input, dd.DD.DDOper op, double scalar) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<Integer, Integer> countLeaves(ADDNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ADDNode> getNodes(ADDNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getVars(ADDNode input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showTable(ADDNode input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ADDNode enumeratePaths(ADDNode input, NodeOperation<ADDNode> op) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean compare(ADDNode input1, ADDNode input2,
			NodeOperation<ADDNode> op) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ADDNode doOp(ADDNode input, NodeOperation<ADDNode> interiorNodeOp,
			NodeOperation<ADDNode> leafOp) {
		// TODO Auto-generated method stub
		return null;
	}

}
