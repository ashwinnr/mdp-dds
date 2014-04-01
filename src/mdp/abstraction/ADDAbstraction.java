package mdp.abstraction;

import mdp.abstraction.parameters.ADDAbstractionParameters;
import mdp.abstraction.types.ADDAbstractionType;
import add.ADDManager;
import add.ADDRNode;

public interface ADDAbstraction< T extends ADDAbstractionType ,  U extends ADDAbstractionParameters<T> > {
	
	public ADDRNode doAbstract( final ADDRNode some_dd ,
			final U params ,
			final ADDManager manager );
	
}
