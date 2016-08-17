package deeplearning;

public interface DQLStatePreprocessor {
	/**
	 * Returns a new buffer containing the current preprocessed state.
	 * 
	 * @return
	 */
	public double[] getPreprocessedState();


	/**
	 * Writes the preprocessed state into the passed buffer, and returns a reference
	 * to the buffer. If the passed buffer is null or is not the correct size to hold
	 * the state, the behavior of this method is undefined. The correct size is given
	 * by the {@link DQLStatePreprocessor#getStateSize()} function.
	 * 
	 * @param stateBuffer
	 *                the buffer to store the state in
	 * @return a reference to the buffer passed to this function
	 * 
	 * @see DQLStatePreprocessor#getStateSize()
	 */
	public double[] getPreprocessedState(double[] stateBuffer);


	/**
	 * Returns the length of the state vector returned by the
	 * <code>getPreprocessedState()</code> function., which is also the size of the
	 * buffer that must be passed to the <code>getpreprocessedState(double[])
	 * function.
	 * 
	 * @return the length of the state vector
	 * 
	 * @see DQLStatePreprocessor#getPreprocessedState()
	 * @see DQLStatePreprocessor#getPreprocessedState(double[])
	 */
	public int getStateSize();
}
