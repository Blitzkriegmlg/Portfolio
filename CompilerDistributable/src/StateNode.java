/**
 * The StateNode class is an extension of the ParseStackNode.
 * The purpose of this class is to hold state numbers on the 
 * parse stack while having the stack hold the ParseStackNode,
 * that way the stack can hold different types and differentiate
 * between them.
 * 
 * @author Matthew Benson
 *
 */
public class StateNode extends ParseStackNode {
	
	/**
	 * Constructs a new StateNode with value that holds the
	 * state number.
	 * 
	 * @param stateNumber	State number to be held by the StateNode.
	 */
	public StateNode(Integer stateNumber) {
		super(stateNumber);
	}

	/**
	 * This method returns the value of the state node, which
	 * is the state number of the node, as set up by our constructor.
	 * Call this method for a StateNode rather than calling .value
	 * because this method casts the value as an int.
	 * 
	 * @return int	The state number held by the node as an int.
	 */
	public int getState()
	{
		return (int) value;
	}
	
}
