package edu.washington.escience.myriad.parallel;

import org.jboss.netty.channel.Channel;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.operator.Operator;
import edu.washington.escience.myriad.parallel.Exchange.ExchangePairID;
import edu.washington.escience.myriad.proto.TransportProto.TransportMessage;
import edu.washington.escience.myriad.util.IPCUtils;

/**
 * The producer part of the Collect Exchange operator.
 * 
 * The producer actively pushes the tuples generated by the child operator to the paired LocalMultiwayConsumer.
 * 
 */
public final class LocalMultiwayProducer extends Producer {

  /**
   * The working thread, which executes the child operator and send the tuples to the paired LocalMultiwayConsumer
   * operator.
   */
  class WorkingThread extends Thread {
    @Override
    public void run() {

      final Channel channel = getConnectionPool().reserveLongTermConnection(remoteWorkerID);

      try {

        final TupleBatchBuffer buffer = new TupleBatchBuffer(getSchema());

        TupleBatch tup = null;
        TransportMessage[] dms = null;
        while ((tup = child.next()) != null) {
          tup.compactInto(buffer);

          while ((dms = buffer.popFilledAsTM(operatorIDs)) != null) {
            for (final TransportMessage dm : dms) {
              channel.write(dm);
            }
          }
        }

        while ((dms = buffer.popAnyAsTM(operatorIDs)) != null) {
          for (final TransportMessage dm : dms) {
            channel.write(dm);
          }
        }

        for (final ExchangePairID operatorID : operatorIDs) {
          channel.write(IPCUtils.eosTM(operatorID));
        }

      } catch (final DbException e) {
        e.printStackTrace();
      } finally {
        getConnectionPool().releaseLongTermConnection(channel);
      }
    }
  }

  /** Required for Java serialization. */
  private static final long serialVersionUID = 1L;

  private transient WorkingThread runningThread;

  /**
   * The paired collect consumer address.
   */

  private final int remoteWorkerID;

  private Operator child;

  public LocalMultiwayProducer(final Operator child, final ExchangePairID[] operatorIDs, final int remoteWorkerID) {
    super(operatorIDs);
    this.child = child;
    this.remoteWorkerID = remoteWorkerID;
  }

  @Override
  public void cleanup() {
  }

  @Override
  protected TupleBatch fetchNext() throws DbException {
    try {
      // wait until the working thread terminate and return an empty tuple set
      runningThread.join();
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public TupleBatch fetchNextReady() throws DbException {
    return fetchNext();
  }

  @Override
  public Operator[] getChildren() {
    return new Operator[] { child };
  }

  @Override
  public Schema getSchema() {
    return child.getSchema();
  }

  @Override
  public void init() throws DbException {
    runningThread = new WorkingThread();
    runningThread.start();
  }

  @Override
  public void setChildren(final Operator[] children) {
    child = children[0];
  }

}