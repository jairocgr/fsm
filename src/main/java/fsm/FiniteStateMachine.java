/*
 * Copyright (c) 2016 Jairo Ricardes Rodrigues Filho All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package fsm;


import java.util.HashMap;
import java.util.Map;

/**
 * @author jairorodrigues
 */
public class FiniteStateMachine<S> {

  public interface Transition<M extends FiniteStateMachine<S>, S, E> {
    S transit(M fsm, E event) throws Exception;
  }

  public static class TransitionTable<M extends FiniteStateMachine<S>, S> {

    private final Map<S, Map<Class, Transition<M, S, Object>>> table;

    private final Transition<M, S, Object> fallbackTransition;

    public TransitionTable(Transition<M, S, Object> fallbackTransition) {

      if (fallbackTransition == null) {
        throw new IllegalArgumentException("fallbackTransition can't be null");
      }

      this.table = new HashMap();
      this.fallbackTransition = fallbackTransition;
    }

    public TransitionTable() {
      table = new HashMap();
      fallbackTransition = new Transition<M, S, Object>() {
        @Override
        public S transit(M fsm, Object event) throws Exception {
          return fsm.getCurrentState(); // do nothing and remaing in the current state
        }
      };
    }

    public <E> TransitionTable transition(S state, Class<E> eventClass, Transition<M, S, E> transition) {

      Map<Class, Transition<M, S, Object>> table = getTableFor(state);

      table.put(eventClass, (Transition<M, S, Object>) transition);

      return this;
    }

    private Map<Class, Transition<M, S, Object>> getTableFor(S state) {
      if (!table.containsKey(state)) {
        table.put(state, new HashMap<Class, Transition<M, S, Object>>());
      }

      return table.get(state);
    }

    private Transition<M, S, Object> getTransitionFor(S state, Object event) {
      if (table.containsKey(state)) {

        Map<Class, Transition<M, S, Object>> transitions = table.get(state);

        return lookup(transitions, event.getClass());

      } else {
        return fallbackTransition;
      }
    }

    private Transition lookup(Map<Class, Transition<M, S, Object>> transitions, Class eventClass) {
      if (eventClass == null) { // stop condition
        return fallbackTransition;
      } else if (transitions.containsKey(eventClass)) {
        return transitions.get(eventClass);
      } else {
        Class parentClass = eventClass.getSuperclass();
        return lookup(transitions, parentClass);
      }
    }
  }

  protected final TransitionTable<FiniteStateMachine<S>, S> tt;

  private S currentState;

  public FiniteStateMachine(S initState) {

    if (initState == null) throw new IllegalArgumentException("initState can't be null");

    this.currentState = initState;
    this.tt = new TransitionTable<FiniteStateMachine<S>, S>();
  }

  public <M extends FiniteStateMachine<S>> FiniteStateMachine(S initState, TransitionTable<M, S> transitionTable) {

    if (initState == null) throw new IllegalArgumentException("initState can't be null");
    if (transitionTable == null) throw new IllegalArgumentException("transitionTable can't be null");

    this.currentState = initState;
    this.tt = (TransitionTable<FiniteStateMachine<S>, S>) transitionTable;
  }

  public <E, M extends FiniteStateMachine<S>> FiniteStateMachine<S> transition(S state, Class<E> eventClass, Transition<M, S, E> transition) {
    tt.transition(state, eventClass, (Transition<FiniteStateMachine<S>, S, E>) transition);
    return this;
  }

  public void reactTo(Object event) {
    synchronized (this) {
      try {

        Transition<FiniteStateMachine<S>, S, Object> transition = tt.getTransitionFor(currentState, event);

        S nextState = transition.transit(this, event);

        currentState = nextState;

      } catch (Exception ex) {
        reactTo(ex);
      }
    }
  }

  public S getCurrentState() {
    synchronized (this) {
      return currentState;
    }
  }
}
