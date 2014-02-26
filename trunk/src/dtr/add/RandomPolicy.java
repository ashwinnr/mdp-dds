package dtr.add;

import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import dd.DDManager.DDOper;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;

public class RandomPolicy extends ADDPolicy {


	private ADDRNode _randPolicy;
	private final Set<String> _actionVars;

	public RandomPolicy(ADDManager man, RDDLFactoredStateSpace stateSpace,
			RDDLFactoredTransition transition, RDDLFactoredReward reward,
			long seed, RDDL2ADD mdp, ADDDecisionTheoreticRegression dtr) {
		super(man, stateSpace, transition, reward, seed);
		_rand = new Random(seed);
		_actionVars = mdp.getFactoredActionSpace().getActionVariables();
		final ADDRNode uniform = man.DD_ONE ;
		final ADDRNode unnormalized_constrained 
			= dtr.applyMDPConstraintsNaively(uniform, null, man.DD_ZERO, null);
		final Set<ADDLeaf> unnormalized_leaves 
			= man.getLeaves(unnormalized_constrained);
		double sum = 0.0d;
		for( final ADDLeaf leaf : unnormalized_leaves ){
			sum += leaf.getMax();
		}
		final ADDRNode normalized_constrained = man.scalarMultiply(unnormalized_constrained, 1.0d/sum );
		_randPolicy = normalized_constrained;
//		_manager.showGraph(_randPolicy);
	}
	
	public <T extends factored.mdp.define.FactoredState<RDDLFactoredStateSpace>, 
	U extends factored.mdp.define.FactoredAction<RDDLFactoredStateSpace,rddl.mdp.RDDLFactoredActionSpace>>
		U getFactoredAction(T state) {
		NavigableMap<String, Boolean> ret = new TreeMap<String, Boolean>();
		
		ADDRNode node = _randPolicy;
		while( node.getNode() instanceof ADDINode ){
			ADDRNode true_br = node.getTrueChild();
			ADDRNode false_br = node.getFalseChild();
			if( true_br.getMax() == 0.0d && false_br.getMax() == 0.0d ){
				System.err.println("No action to take");
				System.exit(1);
			}
			if( true_br.getMax() == 0.0d ){
				ret.put( node.getTestVariable(), false );
				node = false_br;
			}else if( false_br.getMax() == 0.0d ){
				ret.put( node.getTestVariable(), true );
				node = true_br;
			}else{
				final boolean val = _rand.nextBoolean();
				ret.put( node.getTestVariable(), val );
				node = ( val ) ? true_br : false_br;
			}
		}
		//randomize other vars
		for( final String str : _actionVars ){
			if( ret.get(str) == null ){
				ret.put( str, _rand.nextBoolean() );
			}
		}
		
		return (U) new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(ret);
	}

}
