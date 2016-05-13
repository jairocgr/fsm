package fsm;

/**
 * Created by jairorodrigues on 11/05/2016.
 */
public enum State {

  HALT(false),
  WORKING(true),
  BLOCKED(true),
  WAITING(true),
  ERROR(false);

  private boolean isAListeningState;

  State(boolean isAListeningState) {
    this.isAListeningState = isAListeningState;
  }

  public boolean isAListeningState() {
    return isAListeningState;
  }
}
