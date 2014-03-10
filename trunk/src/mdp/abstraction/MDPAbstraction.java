package mdp.abstraction;

import java.util.List;
import java.util.Map;

import add.ADDRNode;

import mdp.abstraction.parameters.ADDAbstractionParameters;
import mdp.abstraction.types.ADDAbstractionType;

public interface MDPAbstraction< T extends ADDAbstractionType ,  U extends ADDAbstractionParameters<T> >
 	extends ADDAbstraction<T, U>{

	public Map<String, ADDRNode> abstract_dynamics( final Map<String, ADDRNode> dynamics );
	public List<ADDRNode> abstract_rewards( final List<ADDRNode> reward );
	
}
