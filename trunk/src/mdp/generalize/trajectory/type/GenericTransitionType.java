package mdp.generalize.trajectory.type;

public interface GenericTransitionType< T extends GeneralizationType>  extends GeneralizationType {
//this class : takes any generalization
    // and makes it 
    //1. consistent trajectories wrt dynamics
    //2. options for fixing some of state/actions
    //3. fixing the number of generalized state/actions
}
