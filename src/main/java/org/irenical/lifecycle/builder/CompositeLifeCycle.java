package org.irenical.lifecycle.builder;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.irenical.lifecycle.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite pattern for lifecycles A common use case is to use a
 * CompositeLifeCycle as your Application and a child lifecycle for each of your
 * modules
 * 
 * @author tgsimao
 *
 */
public class CompositeLifeCycle implements LifeCycle {

  private static final Logger LOG = LoggerFactory.getLogger(CompositeLifeCycle.class);

  private final List<LifeCycle> children = new CopyOnWriteArrayList<>();

  private final Thread terminator = new Thread(this::stop, "Composite LifeCycle shutdown hook");

  private boolean started = false;

  /**
   * Append a child lifecycle
   * 
   * @param child
   *          - a lifecycle instance
   */
  public synchronized CompositeLifeCycle append(LifeCycle child) {
    if (child == null) {
      throw new IllegalArgumentException("You cannot append a null LifeCycle");
    }
    if (started) {
      throw new IllegalStateException("Composite Lifecycle already started");
    }
    children.add(child);
    return this;
  }

  /**
   * After calling this method, a JVM shutdown will trigger this lifecycle to
   * stop()
   */
  public synchronized void withShutdownHook() {
    Runtime.getRuntime().addShutdownHook(terminator);
  }

  /**
   * Stops all children, starting from the last one. Logs and ignores exceptions
   * thrown by each one
   */
  @Override
  public synchronized void stop() {
    LOG.info("Stopping Composite LifeCycle");
    for (int i = children.size(); i > 0; --i) {
      LifeCycle hatchling = null;
      try {
        hatchling = children.get(i - 1);
        LOG.info("Stopping LifeCycle '" + hatchling + "'");
        hatchling.stop();
        LOG.info("LifeCycle '" + hatchling + "' was successfully stopped");
      } catch (Exception e) {
        LOG.error("Error stoping LifeCycle: " + hatchling + "... ignoring", e);
      }
    }
    children.clear();
  }

  /**
   * Returns an AND operation over the children. It is not guaranteed that
   * isRunning will be called for all children
   */
  @Override
  public synchronized <ERROR extends Exception> boolean isRunning() throws ERROR {
    return !children.isEmpty() && children.stream().allMatch(LifeCycle::isRunning);
  }

  /**
   * Starts each child in the order they were appended. An error on one child
   * will halt the execution and the respective exception is thrown
   */
  @Override
  public synchronized <ERROR extends Exception> void start() throws ERROR {
    LOG.info("Starting Composite LifeCycle");
    if (started) {
      throw new IllegalStateException("Composite Lifecycle already started");
    }
    children.forEach(LifeCycle::start);

    started = true;
  }

}
