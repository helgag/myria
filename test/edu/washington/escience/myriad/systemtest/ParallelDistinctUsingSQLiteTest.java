package edu.washington.escience.myriad.systemtest;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.coordinator.catalog.CatalogException;
import edu.washington.escience.myriad.operator.BlockingSQLiteDataReceiver;
import edu.washington.escience.myriad.operator.Operator;
import edu.washington.escience.myriad.operator.SQLiteQueryScan;
import edu.washington.escience.myriad.operator.SQLiteSQLProcessor;
import edu.washington.escience.myriad.parallel.CollectConsumer;
import edu.washington.escience.myriad.parallel.CollectProducer;
import edu.washington.escience.myriad.parallel.Exchange.ExchangePairID;
import edu.washington.escience.myriad.parallel.Server;
import edu.washington.escience.myriad.util.TestUtils;

public class ParallelDistinctUsingSQLiteTest extends SystemTestBase {

  @Test
  public void parallelTestSQLite() throws DbException, IOException, CatalogException {
    final Type[] types = new Type[] { Type.LONG_TYPE, Type.STRING_TYPE };
    final String[] columnNames = new String[] { "id", "name" };
    final Schema schema = new Schema(types, columnNames);

    createTable(WORKER_ID[0], "testtable", "testtable", "id int, name varchar(20)");
    createTable(WORKER_ID[1], "testtable", "testtable", "id int, name varchar(20)");
    createTable(WORKER_ID[0], "temptable", "temptable", "id int, name varchar(20)");
    createTable(WORKER_ID[1], "temptable", "temptable", "id int, name varchar(20)");

    String[] names = TestUtils.randomFixedLengthNumericString(1000, 1005, 20, 20);
    long[] ids = TestUtils.randomLong(1000, 1005, names.length);

    TupleBatchBuffer tbb = new TupleBatchBuffer(schema);
    for (int i = 0; i < names.length; i++) {
      tbb.put(0, ids[i]);
      tbb.put(1, names[i]);
    }
    HashMap<Tuple, Integer> expectedResult = TestUtils.distinct(tbb);

    TupleBatch tb = null;
    while ((tb = tbb.popAny()) != null) {
      insert(WORKER_ID[0], "testtable", schema, tb);
      insert(WORKER_ID[1], "testtable", schema, tb);
    }

    final ExchangePairID serverReceiveID = ExchangePairID.newID();
    final ExchangePairID worker2ReceiveID = ExchangePairID.newID();

    final SQLiteQueryScan scan = new SQLiteQueryScan("testtable.db", "select distinct * from testtable", schema);
    final CollectProducer cp = new CollectProducer(scan, worker2ReceiveID, WORKER_ID[1]);

    // CollectProducer child, ParallelOperatorID operatorID, SocketInfo[] workers
    final CollectConsumer cc = new CollectConsumer(cp, worker2ReceiveID, new int[] { WORKER_ID[0], WORKER_ID[1] });
    final BlockingSQLiteDataReceiver block2 = new BlockingSQLiteDataReceiver("temptable.db", "temptable", cc);
    final SQLiteSQLProcessor scan22 =
        new SQLiteSQLProcessor("temptable.db", "select distinct * from temptable", schema, new Operator[] { block2 });
    final CollectProducer cp22 = new CollectProducer(scan22, serverReceiveID, MASTER_ID);
    final HashMap<Integer, Operator> workerPlans = new HashMap<Integer, Operator>();
    workerPlans.put(WORKER_ID[0], cp);
    workerPlans.put(WORKER_ID[1], cp22);

    while (Server.runningInstance == null) {
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
      }
    }
    Server.runningInstance.dispatchWorkerQueryPlans(workerPlans);
    LOGGER.debug("Query dispatched to the workers");
    TupleBatchBuffer result = null;
    CollectConsumer serverPlan = new CollectConsumer(schema, serverReceiveID, new int[] { WORKER_ID[1] });
    while ((result = Server.runningInstance.startServerQuery(0, serverPlan)) == null) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
      }
    }

    HashMap<Tuple, Integer> resultSet = TestUtils.tupleBatchToTupleBag(result);

    TestUtils.assertTupleBagEqual(expectedResult, resultSet);

  }
}