package edu.washington.escience.myriad.accessmethod;

import java.util.ArrayList;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.Predicate;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.operator.Filter;
import edu.washington.escience.myriad.operator.JdbcQueryScan;
import edu.washington.escience.myriad.operator.Operator;
import edu.washington.escience.myriad.operator.Project;

public class JDBCTest {
  /** The logger for this class. Defaults to myriad level, but could be set to a finer granularity if needed. */
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("edu.washington.escience.myriad");

  @Test
  public void JdbcTest() throws DbException {
    final String host = "54.245.108.198";
    final int port = 3306;
    final String user = "myriad";
    final String password = "nays26[shark";
    final String dbms = "mysql";
    final String databaseName = "myriad_test";
    final String jdbcDriverName = "com.mysql.jdbc.Driver";
    final String query = "select * from testtable";
    final String insert = "INSERT INTO testtable2 VALUES(?)";
    final Schema schema = new Schema(new Type[] { Type.INT_TYPE, Type.STRING_TYPE }, new String[] { "id", "name" });
    final String connectionString = "jdbc:" + dbms + "://" + host + ":" + port + "/" + databaseName;
    final JdbcQueryScan scan = new JdbcQueryScan(jdbcDriverName, connectionString, query, schema, user, password);
    final Filter filter1 = new Filter(Predicate.Op.GREATER_THAN_OR_EQ, 0, new Integer(50), scan);

    final Filter filter2 = new Filter(Predicate.Op.LESS_THAN_OR_EQ, 0, new Integer(60), filter1);

    final ArrayList<Integer> fieldIdx = new ArrayList<Integer>();
    fieldIdx.add(1);
    final ArrayList<Type> fieldType = new ArrayList<Type>();
    fieldType.add(Type.STRING_TYPE);

    final Project project = new Project(fieldIdx, fieldType, filter2);

    final Operator root = project;

    root.open();

    TupleBatch tb = null;
    while ((tb = root.next()) != null) {
      LOGGER.debug(tb.toString());
      JdbcAccessMethod.tupleBatchInsert(jdbcDriverName, connectionString, insert, tb, user, password);
    }

    root.close();
  }
}