// Copyright 2022 Google. This software is provided as-is, without warranty or representation
// for any use or purpose. Your use of it is subject to your agreement with Google.

package com.example.dfdl.util;

import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes Google Cloud Bigtable for testing.
 *
 * <p>This initializer is a very simple application, that create a new table, write to the table,
 * read the data back.
 *
 * <ul>
 *   <li>create table
 *   <li>create single row
 *   <li>read table
 * </ul>
 */
public class BigtableInitializer {

  private static final String COLUMN_FAMILY = "dfdl";
  private static final String COLUMN_QUALIFIER_DEFINITION = "definition";
  private static final String COLUMN_QUALIFIER_NAME = "name";
  private static final String ROW_KEY_PREFIX = "rowKey";
  private final String tableId;
  private final BigtableDataClient dataClient;
  private final BigtableTableAdminClient adminClient;

  public BigtableInitializer(String projectId, String instanceId, String tableId)
      throws IOException {
    this.tableId = tableId;

    // Creates the settings to configure a bigtable data client.
    BigtableDataSettings settings =
        BigtableDataSettings.newBuilder().setProjectId(projectId).setInstanceId(instanceId).build();

    // Creates a bigtable data client.
    dataClient = BigtableDataClient.create(settings);

    // Creates the settings to configure a bigtable table admin client.
    BigtableTableAdminSettings adminSettings =
        BigtableTableAdminSettings.newBuilder()
            .setProjectId(projectId)
            .setInstanceId(instanceId)
            .build();

    // Creates a bigtable table admin client.
    adminClient = BigtableTableAdminClient.create(adminSettings);
  }

  private static String requiredProperty(String prop) {
    String value = System.getProperty(prop);
    if (value == null) {
      throw new IllegalArgumentException("Missing required system property: " + prop);
    }
    return value;
  }

  public static void main(String[] args) throws Exception {
    // These parameters are passed to the initializer using maven:
    // exec:java
    //  -Dexec.mainClass=com.example.mfol.util.BigtableInitializer
    //  -DprojectID=lch-pso-project07
    //  -Dbigtable.instanceID=sabre-dfdl
    //  -Dbigtable.tableID=dfdl-schemas
    String projectId = requiredProperty("projectID");
    String instanceId = requiredProperty("bigtable.instanceID");
    String tableId = requiredProperty("bigtable.tableID");
    BigtableInitializer bigtableInit = new BigtableInitializer(projectId, instanceId, tableId);
    bigtableInit.run();
  }

  public void run() throws Exception {
    createTable();
    writeToTable();
    readSingleRow();
    readSpecificCells();
    readTable();
    close();
  }

  public void close() {
    dataClient.close();
    adminClient.close();
  }

  /**
   * Creates a table.
   */
  public void createTable() {
    // Checks if table exists, creates table if does not exist.
    if (!adminClient.exists(tableId)) {
      System.out.println("Creating table: " + tableId);
      CreateTableRequest createTableRequest =
          CreateTableRequest.of(tableId).addFamily(COLUMN_FAMILY);
      adminClient.createTable(createTableRequest);
      System.out.printf("Table %s created successfully%n", tableId);
    }
  }

  /**
   * Writes a row to a table.
   */
  public void writeToTable() {
    try {
      System.out.println("\nWriting a defintion to the table");

      String name2 = "binary-example";
      String definition2 =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://example.com/dfdl/helloworld/\" xmlns:dfdl=\"http://www.ogf.org/dfdl/dfdl-1.0/\">\n"
              + "  <xs:include schemaLocation=\"org/apache/daffodil/xsd/DFDLGeneralFormat.dfdl.xsd\"/>\n"
              + "  <xs:annotation>\n"
              + "    <xs:appinfo source=\"http://www.ogf.org/dfdl/\">\n"
              + "      <dfdl:format ref=\"GeneralFormat\" representation=\"binary\"/>\n"
              + "    </xs:appinfo>\n"
              + "  </xs:annotation>\n"
              + "  <xs:element name=\"binary_example\">\n"
              + "    <xs:complexType>\n"
              + "      <xs:sequence>\n"
              + "        <xs:element name=\"w\" type=\"xs:int\" dfdl:binaryNumberRep=\"binary\" dfdl:byteOrder=\"bigEndian\" dfdl:lengthKind=\"implicit\"/>\n"
              + "        <xs:element name=\"x\" type=\"xs:int\" dfdl:binaryNumberRep=\"binary\" dfdl:byteOrder=\"bigEndian\" dfdl:lengthKind=\"implicit\"/>\n"
              + "        <xs:element name=\"y\" type=\"xs:double\" dfdl:binaryFloatRep=\"ieee\" dfdl:byteOrder=\"bigEndian\" dfdl:lengthKind=\"implicit\"/>\n"
              + "        <xs:element name=\"z\" type=\"xs:float\" dfdl:binaryFloatRep=\"ieee\" dfdl:byteOrder=\"bigEndian\" dfdl:lengthKind=\"implicit\"/>\n"
              + "      </xs:sequence>\n"
              + "    </xs:complexType>\n"
              + "  </xs:element>\n"
              + "</xs:schema>";

      RowMutation rowMutation2 =
          RowMutation.create(tableId, ROW_KEY_PREFIX + 2)
              .setCell(COLUMN_FAMILY, COLUMN_QUALIFIER_NAME, name2)
              .setCell(COLUMN_FAMILY, COLUMN_QUALIFIER_DEFINITION, definition2);
      dataClient.mutateRow(rowMutation2);
      System.out.println(definition2);

    } catch (NotFoundException e) {
      System.err.println("Failed to write to non-existent table: " + e.getMessage());
    }
  }

  /**
   * Reads a single row from a table.
   */
  public Row readSingleRow() {
    try {
      System.out.println("\nReading a single row by row key");
      Row row = dataClient.readRow(tableId, ROW_KEY_PREFIX + 3);
      System.out.println("Row: " + row.getKey().toStringUtf8());
      for (RowCell cell : row.getCells()) {
        System.out.printf(
            "Family: %s    Qualifier: %s    Value: %s%n",
            cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
      }
      return row;
    } catch (NotFoundException e) {
      System.err.println("Failed to read from a non-existent table: " + e.getMessage());
      return null;
    }
  }

  /**
   * Accesses specific cells by family and qualifier.
   */
  public List<RowCell> readSpecificCells() {
    try {
      System.out.println("\nReading specific cells by family and qualifier");
      Row row = dataClient.readRow(tableId, ROW_KEY_PREFIX + 2);
      System.out.println("Row: " + row.getKey().toStringUtf8());
      List<RowCell> cells = row.getCells(COLUMN_FAMILY, COLUMN_QUALIFIER_NAME);
      for (RowCell cell : cells) {
        System.out.printf(
            "Family: %s    Qualifier: %s    Value: %s%n",
            cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
      }
      return cells;
    } catch (NotFoundException e) {
      System.err.println("Failed to read from a non-existent table: " + e.getMessage());
      return null;
    }
  }

  /**
   * Reads an entire table.
   */
  public List<Row> readTable() {
    try {
      System.out.println("\nReading the entire table");
      Query query = Query.create(tableId);
      ServerStream<Row> rowStream = dataClient.readRows(query);
      List<Row> tableRows = new ArrayList<>();
      for (Row r : rowStream) {
        System.out.println("Row Key: " + r.getKey().toStringUtf8());
        tableRows.add(r);
        for (RowCell cell : r.getCells()) {
          System.out.printf(
              "Family: %s    Qualifier: %s    Value: %s%n",
              cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
        }
      }
      return tableRows;
    } catch (NotFoundException e) {
      System.err.println("Failed to read a non-existent table: " + e.getMessage());
      return null;
    }
  }
}