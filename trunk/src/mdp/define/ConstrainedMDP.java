package mdp.define;

public interface ConstrainedMDP< S extends StateSpace, A extends ActionSpace<S> > {
	public void checkConstraints(  ) throws Exception;
}
