/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


import com.google.common.base.Charsets;
import org.apache.cassandra.config.*;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.compaction.LeveledCompactionStrategy;
import org.apache.cassandra.db.filter.QueryPath;
import org.apache.cassandra.db.marshal.*;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.io.compress.CompressionParameters;
import org.apache.cassandra.io.compress.SnappyCompressor;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.thrift.IndexType;
import org.apache.cassandra.utils.ByteBufferUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaLoader
{
    private static Logger logger = LoggerFactory.getLogger(SchemaLoader.class);
    
    @BeforeClass
    public static void loadSchema() throws IOException
    {
        // Cleanup first
        cleanupAndLeaveDirs();

        CommitLog.instance.allocator.enableReserveSegmentCreation();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            public void uncaughtException(Thread t, Throwable e)
            {
                logger.error("Fatal exception in thread " + t, e);
            }
        });


        // Migrations aren't happy if gossiper is not started
        startGossiper();
        try
        {
            for (KSMetaData ksm : schemaDefinition())
                MigrationManager.announceNewKeyspace(ksm);
        }
        catch (ConfigurationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void startGossiper()
    {
        Gossiper.instance.start((int) (System.currentTimeMillis() / 1000));
    }

    @AfterClass
    public static void stopGossiper()
    {
        Gossiper.instance.stop();
    }

    public static Collection<KSMetaData> schemaDefinition() throws ConfigurationException
    {
        List<KSMetaData> schema = new ArrayList<KSMetaData>();

        // A whole bucket of shorthand
        String ks1 = "Keyspace1";
        String ks2 = "Keyspace2";
        String ks3 = "Keyspace3";
        String ks4 = "Keyspace4";                                   
        String ks5 = "Keyspace5";
        String ks6 = "Keyspace6";
        String ks_kcs = "KeyCacheSpace";
        String ks_rcs = "RowCacheSpace";
        String ks_nocommit = "NoCommitlogSpace";

        Class<? extends AbstractReplicationStrategy> simple = SimpleStrategy.class;

        Map<String, String> opts_rf1 = KSMetaData.optsWithRF(1);
        Map<String, String> opts_rf2 = KSMetaData.optsWithRF(2);
        Map<String, String> opts_rf3 = KSMetaData.optsWithRF(3);
        Map<String, String> opts_rf5 = KSMetaData.optsWithRF(5);

        ColumnFamilyType st = ColumnFamilyType.Standard;
        ColumnFamilyType su = ColumnFamilyType.Super;
        AbstractType bytes = BytesType.instance;

        AbstractType<?> composite = CompositeType.getInstance(Arrays.asList(new AbstractType<?>[]{BytesType.instance, TimeUUIDType.instance, IntegerType.instance}));
        Map<Byte, AbstractType<?>> aliases = new HashMap<Byte, AbstractType<?>>();
        aliases.put((byte)'b', BytesType.instance);
        aliases.put((byte)'t', TimeUUIDType.instance);
        AbstractType<?> dynamicComposite = DynamicCompositeType.getInstance(aliases);

        // these column definitions will will be applied to the jdbc utf and integer column familes respectively.
        Map<ByteBuffer, ColumnDefinition> integerColumn = new HashMap<ByteBuffer, ColumnDefinition>();
        integerColumn.put(IntegerType.instance.fromString("42"), new ColumnDefinition(
            IntegerType.instance.fromString("42"),
            UTF8Type.instance,
            null,
            null,
            null,
            null));
        Map<ByteBuffer, ColumnDefinition> utf8Column = new HashMap<ByteBuffer, ColumnDefinition>();
        utf8Column.put(UTF8Type.instance.fromString("fortytwo"), new ColumnDefinition(
            UTF8Type.instance.fromString("fortytwo"),
            IntegerType.instance,
            null,
            null,
            null,
            null));

        // Make it easy to test leveled compaction
        Map<String, String> leveledOptions = new HashMap<String, String>();
        leveledOptions.put("sstable_size_in_mb", "1");

        // Keyspace 1
        schema.add(KSMetaData.testMetadata(ks1,
                                           simple,
                                           opts_rf1,

                                           // Column Families
                                           standardCFMD(ks1, "Standard1"),
                                           standardCFMD(ks1, "Standard2"),
                                           standardCFMD(ks1, "Standard3"),
                                           standardCFMD(ks1, "Standard4"),
                                           standardCFMD(ks1, "StandardLong1"),
                                           standardCFMD(ks1, "StandardLong2"),
                                           new CFMetaData(ks1,
                                                          "ValuesWithQuotes",
                                                          st,
                                                          BytesType.instance,
                                                          null)
                                                   .defaultValidator(UTF8Type.instance),
                                           superCFMD(ks1, "Super1", LongType.instance),
                                           superCFMD(ks1, "Super2", LongType.instance),
                                           superCFMD(ks1, "Super3", LongType.instance),
                                           superCFMD(ks1, "Super4", UTF8Type.instance),
                                           superCFMD(ks1, "Super5", bytes),
                                           superCFMD(ks1, "Super6", LexicalUUIDType.instance, UTF8Type.instance),
                                           indexCFMD(ks1, "Indexed1", true),
                                           indexCFMD(ks1, "Indexed2", false),
                                           new CFMetaData(ks1,
                                                          "StandardInteger1",
                                                          st,
                                                          IntegerType.instance,
                                                          null),
                                           new CFMetaData(ks1,
                                                          "Counter1",
                                                          st,
                                                          bytes,
                                                          null)
                                                   .defaultValidator(CounterColumnType.instance),
                                           new CFMetaData(ks1,
                                                          "SuperCounter1",
                                                          su,
                                                          bytes,
                                                          bytes)
                                                   .defaultValidator(CounterColumnType.instance),
                                           superCFMD(ks1, "SuperDirectGC", BytesType.instance).gcGraceSeconds(0),
                                           jdbcCFMD(ks1, "JdbcInteger", IntegerType.instance).columnMetadata(integerColumn),
                                           jdbcCFMD(ks1, "JdbcUtf8", UTF8Type.instance).columnMetadata(utf8Column),
                                           jdbcCFMD(ks1, "JdbcLong", LongType.instance),
                                           jdbcCFMD(ks1, "JdbcBytes", bytes),
                                           jdbcCFMD(ks1, "JdbcAscii", AsciiType.instance),
                                           new CFMetaData(ks1,
                                                          "StandardComposite",
                                                          st,
                                                          composite,
                                                          null),
                                           new CFMetaData(ks1,
                                                          "StandardDynamicComposite",
                                                          st,
                                                          dynamicComposite,
                                                          null),
                                           standardCFMD(ks1, "StandardLeveled").compactionStrategyClass(LeveledCompactionStrategy.class).compactionStrategyOptions(leveledOptions)));

        // Keyspace 2
        schema.add(KSMetaData.testMetadata(ks2,
                                           simple,
                                           opts_rf1,

                                           // Column Families
                                           standardCFMD(ks2, "Standard1"),
                                           standardCFMD(ks2, "Standard3"),
                                           superCFMD(ks2, "Super3", bytes),
                                           superCFMD(ks2, "Super4", TimeUUIDType.instance),
                                           indexCFMD(ks2, "Indexed1", true)));

        // Keyspace 3
        schema.add(KSMetaData.testMetadata(ks3,
                                           simple,
                                           opts_rf5,

                                           // Column Families
                                            standardCFMD(ks3, "Standard1"),
                                           indexCFMD(ks3, "Indexed1", true)));

        // Keyspace 4
        schema.add(KSMetaData.testMetadata(ks4,
                                           simple,
                                           opts_rf3,

                                           // Column Families
                                           standardCFMD(ks4, "Standard1"),
                                           standardCFMD(ks4, "Standard3"),
                                           superCFMD(ks4, "Super3", bytes),
                                           superCFMD(ks4, "Super4", TimeUUIDType.instance),
                                           new CFMetaData(ks4,
                                                          "Super5",
                                                          su,
                                                          TimeUUIDType.instance,
                                                          bytes)));

        // Keyspace 5
        schema.add(KSMetaData.testMetadata(ks5,
                                           simple,
                                           opts_rf2,
                                           standardCFMD(ks5, "Standard1"),
                                           standardCFMD(ks5, "Counter1")
                                                   .defaultValidator(CounterColumnType.instance)));

        // Keyspace 6
        schema.add(KSMetaData.testMetadata(ks6,
                                           simple,
                                           opts_rf1,
                                           indexCFMD(ks6, "Indexed1", true)));

        // KeyCacheSpace
        schema.add(KSMetaData.testMetadata(ks_kcs,
                                           simple,
                                           opts_rf1,
                                            standardCFMD(ks_kcs, "Standard1"),
                                           standardCFMD(ks_kcs, "Standard2"),
                                           standardCFMD(ks_kcs, "Standard3")));

        // RowCacheSpace
        schema.add(KSMetaData.testMetadata(ks_rcs,
                                           simple,
                                           opts_rf1,
                                           standardCFMD(ks_rcs, "CFWithoutCache").caching(CFMetaData.Caching.NONE),
                                           standardCFMD(ks_rcs, "CachedCF").caching(CFMetaData.Caching.ALL)));

        schema.add(KSMetaData.testMetadataNotDurable(ks_nocommit,
                                                     simple,
                                                     opts_rf1,
                                                      standardCFMD(ks_nocommit, "Standard1")));


        if (Boolean.parseBoolean(System.getProperty("cassandra.test.compression", "false")))
            useCompression(schema);

        return schema;
    }

    private static void useCompression(List<KSMetaData> schema)
    {
        for (KSMetaData ksm : schema)
        {
            for (CFMetaData cfm : ksm.cfMetaData().values())
            {
                cfm.compressionParameters(new CompressionParameters(SnappyCompressor.instance));
            }
        }
    }

    private static CFMetaData standardCFMD(String ksName, String cfName)
    {
        return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard, BytesType.instance, null);
    }
    
    private static CFMetaData superCFMD(String ksName, String cfName, AbstractType subcc)
    {
        return superCFMD(ksName, cfName, BytesType.instance, subcc);
    }
    private static CFMetaData superCFMD(String ksName, String cfName, AbstractType cc, AbstractType subcc)
    {
        return new CFMetaData(ksName, cfName, ColumnFamilyType.Super, cc, subcc);
    }
    private static CFMetaData indexCFMD(String ksName, String cfName, final Boolean withIdxType) throws ConfigurationException
    {
        return standardCFMD(ksName, cfName)
               .keyValidator(AsciiType.instance)
               .columnMetadata(new HashMap<ByteBuffer, ColumnDefinition>()
                   {{
                        ByteBuffer cName = ByteBuffer.wrap("birthdate".getBytes(Charsets.UTF_8));
                        IndexType keys = withIdxType ? IndexType.KEYS : null;
                        put(cName, new ColumnDefinition(cName, LongType.instance, keys, null, withIdxType ? ByteBufferUtil.bytesToHex(cName) : null, null));
                    }});
    }
    private static CFMetaData jdbcCFMD(String ksName, String cfName, AbstractType comp)
    {
        return new CFMetaData(ksName, cfName, ColumnFamilyType.Standard, comp, null).defaultValidator(comp);
    }

    public static void cleanupAndLeaveDirs() throws IOException
    {
        mkdirs();
        cleanup();
        mkdirs();
        CommitLog.instance.resetUnsafe(); // cleanup screws w/ CommitLog, this brings it back to safe state
    }

    public static void cleanup() throws IOException
    {
        // clean up commitlog
        String[] directoryNames = { DatabaseDescriptor.getCommitLogLocation(), };
        for (String dirName : directoryNames)
        {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }

        cleanupSavedCaches();

        // clean up data directory which are stored as data directory/table/data files
        for (String dirName : DatabaseDescriptor.getAllDataFileLocations())
        {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }
    }

    public static void mkdirs()
    {
        try
        {
            DatabaseDescriptor.createAllDirectories();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void insertData(String keyspace, String columnFamily, int offset, int numberOfRows) throws IOException
    {
        for (int i = offset; i < offset + numberOfRows; i++)
        {
            ByteBuffer key = ByteBufferUtil.bytes("key" + i);
            RowMutation rowMutation = new RowMutation(keyspace, key);
            QueryPath path = new QueryPath(columnFamily, null, ByteBufferUtil.bytes("col" + i));

            rowMutation.add(path, ByteBufferUtil.bytes("val" + i), System.currentTimeMillis());
            rowMutation.applyUnsafe();
        }
    }

    /* usually used to populate the cache */
    protected void readData(String keyspace, String columnFamily, int offset, int numberOfRows) throws IOException
    {
        ColumnFamilyStore store = Table.open(keyspace).getColumnFamilyStore(columnFamily);
        for (int i = offset; i < offset + numberOfRows; i++)
        {
            DecoratedKey key = Util.dk("key" + i);
            QueryPath path = new QueryPath(columnFamily, null, ByteBufferUtil.bytes("col" + i));

            store.getColumnFamily(key, path, ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBufferUtil.EMPTY_BYTE_BUFFER, false, 1);
        }
    }

    protected static void cleanupSavedCaches()
    {
        File cachesDir = new File(DatabaseDescriptor.getSavedCachesLocation());

        if (!cachesDir.exists() || !cachesDir.isDirectory())
            return;

        FileUtils.delete(cachesDir.listFiles());
    }
}
