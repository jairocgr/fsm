package fsm;

import org.junit.Before;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import fsm.event.BlockForNewReadings;
import fsm.event.GoBackToListening;
import fsm.event.NewReading;
import fsm.event.StartRequest;
import fsm.event.StopRequest;

import fsm.FiniteStateMachine.Transition;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created by jairorodrigues on 11/05/2016.
 */
public class FiniteStateMachineTest {

  private static final State INIT_STATE = State.HALT;

  private Queue<String> eventQueue;

  private FiniteStateMachine<State> fsm;

  @Before
  public void setUp() {

    eventQueue = new ConcurrentLinkedQueue<String>() {
      @Override
      public boolean add(String s) {

        System.out.println("transition: " + s);

        return super.add(s);
      }
    };

    Transition<FiniteStateMachine<State>, State, Object> emptyTransition = new Transition<FiniteStateMachine<State>, State, Object>() {
      @Override
      public State transit(FiniteStateMachine<State> machine, Object event) throws Exception {
        eventQueue.add("faz nada..");
        return machine.getCurrentState();
      }
    };

    Transition<FiniteStateMachine<State>, State, StartRequest> start = new Transition<FiniteStateMachine<State>, State, StartRequest>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, StartRequest event) throws Exception {

        eventQueue.add("starting...");

        return State.WORKING;
      }
    };

    Transition<FiniteStateMachine<State>, State, BlockForNewReadings> block = new Transition<FiniteStateMachine<State>, State, BlockForNewReadings>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, BlockForNewReadings event) throws Exception {

        eventQueue.add("blocking the machine...");

        return State.BLOCKED;
      }
    };

    Transition<FiniteStateMachine<State>, State, GoBackToListening> unblock = new Transition<FiniteStateMachine<State>, State, GoBackToListening>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, GoBackToListening event) throws Exception {

        eventQueue.add("blocking the machine...");

        return State.WORKING;
      }
    };

    Transition<FiniteStateMachine<State>, State, StopRequest> halt = new Transition<FiniteStateMachine<State>, State, StopRequest>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, StopRequest event) throws Exception {

        eventQueue.add("halting the machine...");

        return State.HALT;
      }
    };

    Transition<FiniteStateMachine<State>, State, Exception> handleError = new Transition<FiniteStateMachine<State>, State, Exception>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, Exception event) throws Exception {

        eventQueue.add("something whent wrong with the machine: " + event.getMessage());

        return State.ERROR;
      }
    };

    Transition<FiniteStateMachine<State>, State, NewReading> handleInput = new Transition<FiniteStateMachine<State>, State, NewReading>() {

      @Override
      public State transit(FiniteStateMachine<State> machine, NewReading event) throws Exception {

        if (event.getReading().equals("should cause to fail")) {
          throw new IllegalStateException(event.getReading());
        }

        eventQueue.add(event.getReading());

        return State.WORKING;
      }
    };

    FiniteStateMachine.TransitionTable tt = new FiniteStateMachine.TransitionTable();

    tt.transition(State.HALT, StartRequest.class, start)
      .transition(State.HALT, Exception.class, handleError)
      .transition(State.WORKING, StopRequest.class, halt)
      .transition(State.WORKING, BlockForNewReadings.class, block)
      .transition(State.WORKING, Exception.class, handleError)
      .transition(State.WORKING, NewReading.class, handleInput)
      .transition(State.BLOCKED, GoBackToListening.class, unblock)
      .transition(State.BLOCKED, StopRequest.class, halt)
      .transition(State.BLOCKED, Exception.class, handleError);

    fsm = new FiniteStateMachine<State>(INIT_STATE, tt);
  }

  @Test
  public void test() {

    assertEquals(INIT_STATE, fsm.getCurrentState());

    NewReading ignoredReading = new NewReading("ignored reading");


    fsm.reactTo(ignoredReading);
    assertTrue(eventQueue.isEmpty());
    assertEquals(INIT_STATE, fsm.getCurrentState());


    fsm.reactTo(new StartRequest());
    assertThat(eventQueue.remove(), containsString("starting"));
    assertEquals(State.WORKING, fsm.getCurrentState());


    NewReading readings = new NewReading("readins 2");
    fsm.reactTo(readings);
    assertEquals(readings.getReading(), eventQueue.remove());
    assertEquals(State.WORKING, fsm.getCurrentState());


    fsm.reactTo(new BlockForNewReadings());
    assertThat(eventQueue.remove(), containsString("blocking"));
    assertEquals(State.BLOCKED, fsm.getCurrentState());


    fsm.reactTo(ignoredReading);
    assertTrue(eventQueue.isEmpty());
    assertEquals(State.BLOCKED, fsm.getCurrentState());


    fsm.reactTo(new StopRequest());
    assertThat(eventQueue.remove(), containsString("halting"));
    assertEquals(State.HALT, fsm.getCurrentState());
  }

  @Test
  public void testWithErrorOnTransition() {

    assertEquals(INIT_STATE, fsm.getCurrentState());


    NewReading ignoredReading = new NewReading("ignored reading");


    fsm.reactTo(ignoredReading);
    assertTrue(eventQueue.isEmpty());
    assertEquals(INIT_STATE, fsm.getCurrentState());


    fsm.reactTo(new StartRequest());
    assertThat(eventQueue.remove(), containsString("starting"));
    assertEquals(State.WORKING, fsm.getCurrentState());


    NewReading failedReading = new NewReading("should cause to fail");
    fsm.reactTo(failedReading);
    String errorReport = eventQueue.remove();
    assertThat(errorReport, containsString("something whent wrong"));
    assertThat(errorReport, containsString(failedReading.getReading()));
    assertEquals(State.ERROR, fsm.getCurrentState());


    fsm.reactTo(new BlockForNewReadings());
    assertTrue(eventQueue.isEmpty());
    assertEquals(State.ERROR, fsm.getCurrentState());
  }
}
