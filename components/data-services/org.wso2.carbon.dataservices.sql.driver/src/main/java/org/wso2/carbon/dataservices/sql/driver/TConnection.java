/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dataservices.sql.driver;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

public abstract class TConnection implements Connection {

    private String conType;

    private String path;

    public TConnection(Properties props) {
        this.conType = props.getProperty(TDriver.DATA_SOURCE_TYPE);
        this.path = props.getProperty(TDriver.FILE_PATH);
    }

    public String getType() {
        return conType;
    }

    public String getPath() {
        return path;
    }

    public String nativeSQL(String sql) throws SQLException {
        return null;  
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        
    }

    public boolean getAutoCommit() throws SQLException {
        return false;  
    }

    public void commit() throws SQLException {
        
    }

    public void rollback() throws SQLException {
        
    }

    public void close() throws SQLException {
        
    }

    public boolean isClosed() throws SQLException {
        return false;  
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return null;  
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        
    }

    public boolean isReadOnly() throws SQLException {
        return false;  
    }

    public void setCatalog(String catalog) throws SQLException {
        
    }

    public String getCatalog() throws SQLException {
        return null;  
    }

    public void setTransactionIsolation(int level) throws SQLException {
        
    }

    public int getTransactionIsolation() throws SQLException {
        return 0;  
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;  
    }

    public void clearWarnings() throws SQLException {
        
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency) throws SQLException {
        return null;  
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return null;  
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        
    }

    public void setHoldability(int holdability) throws SQLException {
        
    }

    public int getHoldability() throws SQLException {
        return 0;  
    }

    public Savepoint setSavepoint() throws SQLException {
        return null;  
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return null;  
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException {
        return null;  
    }

    public Clob createClob() throws SQLException {
        return null;  
    }

    public Blob createBlob() throws SQLException {
        return null;  
    }

    public NClob createNClob() throws SQLException {
        return null;  
    }

    public SQLXML createSQLXML() throws SQLException {
        return null;  
    }

    public boolean isValid(int timeout) throws SQLException {
        return false;  
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        
    }

    public String getClientInfo(String name) throws SQLException {
        return null;  
    }

    public Properties getClientInfo() throws SQLException {
        return null;  
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;  
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;  
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;  
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;  
    }

}
