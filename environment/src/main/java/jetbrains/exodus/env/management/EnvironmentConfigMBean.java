/**
 * Copyright 2010 - 2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.env.management;

public interface EnvironmentConfigMBean {

    String OBJECT_NAME_PREFIX = "jetbrains.exodus.env: type=EnvironmentConfig";

    long getMemoryUsage();

    int getMemoryUsagePercent();

    boolean getLogDurableWrite();

    void setLogDurableWrite(boolean durableWrite);

    long getLogFileSize();

    long getLogLockTimeout();

    int getLogCachePageSize();

    int getLogCacheOpenFilesCount();

    boolean getLogCacheUseNio();

    long getLogCacheFreePhysicalMemoryThreshold();

    boolean isLogCacheShared();

    boolean isLogCacheNonBlocking();

    int getLogCacheGenerationCount();

    int getLogCacheReadAheadMultiple();

    void setLogCacheReadAheadMultiple(int readAheadMultiple);

    boolean isLogCleanDirectoryExpected();

    boolean isLogClearInvalid();

    long getLogSyncPeriod();

    void setLogSyncPeriod(long millis);

    boolean isLogFullFileReadonly();

    boolean getEnvIsReadonly();

    void setEnvIsReadonly(boolean isReadonly);

    boolean getEnvFailFastInReadonly();

    void setEnvFailFastInReadonly(boolean failFast);

    boolean getEnvReadonlyEmptyStores();

    void setEnvReadonlyEmptyStores(boolean readonlyEmptyStores);

    int getEnvStoreGetCacheSize();

    void setEnvStoreGetCacheSize(int storeGetCacheSize);

    int getEnvStoreGetCacheMinTreeSize();

    void setEnvStoreGetCacheMinTreeSize(int minTreeSize);

    int getEnvStoreGetCacheMaxValueSize();

    void setEnvStoreGetCacheMaxValueSize(int maxValueSize);

    boolean getEnvCloseForcedly();

    void setEnvCloseForcedly(boolean closeForcedly);

    long getEnvTxnReplayTimeout();

    void setEnvTxnReplayTimeout(final long txnReplayTimeout);

    int getEnvTxnReplayMaxCount();

    void setEnvTxnReplayMaxCount(final int txnReplayMaxCount);

    boolean getEnvTxnDowngradeAfterFlush();

    void setEnvTxnDowngradeAfterFlush(final boolean downgrade);

    boolean getEnvTxnSingleThreadWrites();

    void setEnvTxnSingleThreadWrites(final boolean singleThreadWrites);

    int getEnvMaxParallelTxns();

    int getEnvMaxParallelReadonlyTxns();

    int getEnvMonitorTxnsTimeout();

    int getEnvMonitorTxnsCheckFreq();

    boolean getEnvGatherStatistics();

    int getTreeMaxPageSize();

    void setTreeMaxPageSize(int treeMaxPageSize);

    boolean isGcEnabled();

    void setGcEnabled(boolean enabled);

    boolean isGcSuspended();

    int getGcStartIn();

    int getGcMinUtilization();

    void setGcMinUtilization(int percent);

    boolean getGcRenameFiles();

    void setGcRenameFiles(boolean rename);

    int getGcFileMinAge();

    void setGcFileMinAge(int minAge);

    int getGcFilesInterval();

    void setGcFilesInterval(int files);

    int getGcRunPeriod();

    void setGcRunPeriod(int runPeriod);

    boolean getGcUtilizationFromScratch();

    void setGcUtilizationFromScratch(boolean fromScratch);

    String getGcUtilizationFromFile();

    void setGcUtilizationFromFile(String file);

    boolean getGcUseExclusiveTransaction();

    void setGcUseExclusiveTransaction(boolean useExclusiveTransaction);

    int getGcTransactionAcquireTimeout();

    void setGcTransactionAcquireTimeout(int timeout);

    int getGcTransactionTimeout();

    void setGcTransactionTimeout(int timeout);

    int getGcFilesDeletionDelay();

    void setGcFilesDeletionDelay(int delay);

    int getGcRunEvery();

    void setGcRunEvery(int seconds);

    void close();

    void gc();
}
