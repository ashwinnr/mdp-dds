package mdp.abstraction;

import mdp.abstraction.parameters.ADDAbstractionParameters;
import mdp.abstraction.types.ADDAbstractionType;
import add.ADDRNode;

public interface ADDAbstraction< T extends ADDAbstractionType ,  U extends ADDAbstractionParameters<T> > {
	
	public ADDRNode doAbstract( final ADDRNode some_dd ,
			final T abstraction_type,
			final U params );
	
}
