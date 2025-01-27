// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.doris.flink.tools.cdc;

import org.apache.doris.flink.catalog.doris.DataModel;
import org.apache.doris.flink.catalog.doris.FieldSchema;
import org.apache.doris.flink.catalog.doris.TableSchema;
import org.apache.doris.flink.tools.cdc.mysql.MysqlType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SourceSchema {
    private final String databaseName;
    private final String tableName;
    private final String tableComment;
    private final LinkedHashMap<String, FieldSchema> fields;
    public final List<String> primaryKeys;

    public SourceSchema(
            DatabaseMetaData metaData, String databaseName, String tableName, String tableComment)
            throws Exception {
        this.databaseName = databaseName;
        this.tableName = tableName;
        this.tableComment = tableComment;

        fields = new LinkedHashMap<>();
        try (ResultSet rs = metaData.getColumns(databaseName, null, tableName, null)) {
            while (rs.next()) {
                String fieldName = rs.getString("COLUMN_NAME");
                String comment = rs.getString("REMARKS");
                String fieldType = rs.getString("TYPE_NAME");
                Integer precision = rs.getInt("COLUMN_SIZE");

                if (rs.wasNull()) {
                    precision = null;
                }
                Integer scale = rs.getInt("DECIMAL_DIGITS");
                if (rs.wasNull()) {
                    scale = null;
                }
                String dorisTypeStr = MysqlType.toDorisType(fieldType, precision, scale);
                fields.put(fieldName, new FieldSchema(fieldName, dorisTypeStr, comment));
            }
        }

        primaryKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(databaseName, null, tableName)) {
            while (rs.next()) {
                String fieldName = rs.getString("COLUMN_NAME");
                primaryKeys.add(fieldName);
            }
        }
    }

    public TableSchema convertTableSchema(Map<String, String> tableProps) {
        TableSchema tableSchema = new TableSchema();
        tableSchema.setModel(DataModel.UNIQUE);
        tableSchema.setFields(this.fields);
        tableSchema.setKeys(this.primaryKeys);
        tableSchema.setTableComment(this.tableComment);
        tableSchema.setDistributeKeys(this.primaryKeys);
        tableSchema.setProperties(tableProps);
        return tableSchema;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public LinkedHashMap<String, FieldSchema> getFields() {
        return fields;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public String getTableComment() {
        return tableComment;
    }
}
