package fsm.event;

/**
 * Created by jairorodrigues on 11/05/2016.
 */
public class NewReading {

  private final String reading;

  public NewReading(String reading) {
    this.reading = reading;
  }

  public String getReading() {
    return reading;
  }
}
