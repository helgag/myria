package edu.washington.escience.myriad.parallel;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import edu.washington.escience.myriad.Predicate;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.accessmethod.SQLiteAccessMethod;
import edu.washington.escience.myriad.column.Column;
import edu.washington.escience.myriad.table._TupleBatch;

// Not yet @ThreadSafe
public class SQLiteTupleBatch implements _TupleBatch {

  private static final long serialVersionUID = 1L;

  public static final int BATCH_SIZE = 100;

  private final Schema inputSchema;
  private transient String dataDir;
  private final String filename;
  private int numInputTuples;
  private final String tableName;

  public SQLiteTupleBatch(final Schema inputSchema, final String filename, final String tableName) {
    this.inputSchema = Objects.requireNonNull(inputSchema);
    this.filename = filename;
    this.tableName = tableName;
  }

  @Override
  public synchronized _TupleBatch append(final _TupleBatch another) {
    final Iterator<Schema.TDItem> it = this.inputSchema.iterator();

    final String[] fieldNames = new String[this.inputSchema.numFields()];
    final String[] placeHolders = new String[this.inputSchema.numFields()];
    int i = 0;
    while (it.hasNext()) {
      final Schema.TDItem item = it.next();
      placeHolders[i] = "?";
      fieldNames[i++] = item.getName();
    }

    SQLiteAccessMethod.tupleBatchInsert(this.dataDir + "/" + this.filename, "insert into " + this.tableName + " ( "
        + StringUtils.join(fieldNames, ',') + " ) values ( " + StringUtils.join(placeHolders, ',') + " )",
        new TupleBatch(another.outputSchema(), another.outputRawData(), another.numOutputTuples()));
    return this;
  }

  @Override
  public synchronized _TupleBatch distinct() {
    return null;
  }

  @Override
  public synchronized _TupleBatch except(final _TupleBatch another) {
    return null;
  }

  @Override
  public synchronized SQLiteTupleBatch filter(final int fieldIdx, final Predicate.Op op, final Object operand) {
    return this;
  }

  @Override
  public synchronized boolean getBoolean(final int column, final int row) {
    return false;
  }

  @Override
  public synchronized double getDouble(final int column, final int row) {
    return 0d;
  }

  @Override
  public synchronized float getFloat(final int column, final int row) {
    return 0f;
  }

  @Override
  public synchronized int getInt(final int column, final int row) {
    return 0;
  }

  @Override
  public synchronized long getLong(final int column, final int row) {
    return 0;
  }

  @Override
  public synchronized String getString(final int column, final int row) {
    return null;
  }

  @Override
  public synchronized _TupleBatch groupby() {
    return null;
  }

  @Override
  public int hashCode(final int rowIndx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Schema inputSchema() {
    return inputSchema;
  }

  @Override
  public synchronized _TupleBatch intersect(final _TupleBatch another) {
    return null;
  }

  @Override
  public synchronized _TupleBatch join(final _TupleBatch other, final Predicate p, final _TupleBatch output) {
    return null;
  }

  @Override
  public synchronized int numInputTuples() {
    return numInputTuples;
  }

  @Override
  public synchronized int numOutputTuples() {
    return this.numInputTuples;
  }

  @Override
  public synchronized _TupleBatch orderby() {
    return null;
  }

  protected synchronized int[] outputColumnIndices() {
    final int numInputColumns = this.inputSchema.numFields();
    final int[] validC = new int[numInputColumns];
    int j = 0;
    for (int i = 0; i < numInputColumns; i++) {
      // operate on index i here
      validC[j++] = i;
    }
    return validC;
  }

  @Override
  public List<Column> outputRawData() {
    return null;
  }

  @Override
  public synchronized Schema outputSchema() {

    final int[] columnIndices = this.outputColumnIndices();
    final String[] columnNames = new String[columnIndices.length];
    final Type[] columnTypes = new Type[columnIndices.length];
    int j = 0;
    for (final int columnIndx : columnIndices) {
      columnNames[j] = this.inputSchema.getFieldName(columnIndx);
      columnTypes[j] = this.inputSchema.getFieldType(columnIndx);
      j++;
    }

    return new Schema(columnTypes, columnNames);
  }

  public synchronized SQLiteTupleBatch[] partition(final PartitionFunction<?, ?> p) {
    return null;
  }

  @Override
  public TupleBatchBuffer[] partition(final PartitionFunction<?, ?> p, final TupleBatchBuffer[] buffers) {
    return null;
  }

  @Override
  public synchronized SQLiteTupleBatch project(final int[] remainingColumns) {
    return this;
  }

  @Override
  public synchronized _TupleBatch purgeFilters() {
    return this;
  }

  @Override
  public synchronized _TupleBatch purgeProjects() {
    return this;
  }

  @Override
  public _TupleBatch remove(final int innerIdx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized _TupleBatch renameColumn(final int inputColumnIdx, final String newName) {
    return this;
  }

  public void reset(final String dataDir) {
    this.dataDir = dataDir;
  }

  @Override
  public synchronized _TupleBatch union(final _TupleBatch another) {
    return null;
  }
}