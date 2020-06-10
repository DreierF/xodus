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
package jetbrains.exodus.entitystore;

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.TestFor;
import jetbrains.exodus.TestUtil;
import jetbrains.exodus.bindings.ComparableSet;
import jetbrains.exodus.core.execution.Job;
import jetbrains.exodus.util.ByteArraySizedInputStream;
import jetbrains.exodus.util.DeferredIO;
import jetbrains.exodus.util.LightByteArrayOutputStream;
import jetbrains.exodus.util.UTFUtil;
import org.junit.Assert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@SuppressWarnings({"RawUseOfParameterizedType", "ConstantConditions"})
public class EntityTests extends EntityStoreTestBase {

    @Override
    protected String[] casesThatDontNeedExplicitTxn() {
        return new String[]{"testConcurrentCreationTypeIdsAreOk",
            "testConcurrentSerializableChanges",
            "testEntityStoreClear",
            "testSetPhantomLink",
            "testAddPhantomLink"
        };
    }

    public void testCreateSingleEntity() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        final EntityIterable all = txn.getAll("Issue");
        Assert.assertEquals(1, all.size());
        Assert.assertTrue(all.iterator().hasNext());
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.getId().getTypeId() >= 0);
        Assert.assertTrue(entity.getId().getLocalId() >= 0);
    }

    public void testCreateSingleEntity2() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        txn.flush();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.getId().getTypeId() >= 0);
        Assert.assertTrue(entity.getId().getLocalId() >= 0);
        Assert.assertEquals(entity.getId(), new PersistentEntityId(0, 0));
        try {
            txn.getEntity(new PersistentEntityId(0, 1));
            Assert.fail();
        } catch (EntityRemovedInDatabaseException ignore) {
        }
    }

    public void testEntityIdToString() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        txn.flush();
        final String representation = entity.getId().toString();
        Assert.assertEquals(entity, txn.getEntity(txn.toEntityId(representation)));
    }

    public void testCreateTwoEntitiesInTransaction() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity1 = txn.newEntity("Issue");
        final Entity entity2 = txn.newEntity("Issue");
        txn.flush();
        Assert.assertNotNull(entity1);
        Assert.assertTrue(entity1.getId().getTypeId() >= 0);
        Assert.assertTrue(entity1.getId().getLocalId() >= 0);
        Assert.assertNotNull(entity2);
        Assert.assertTrue(entity2.getId().getLocalId() > 0);
        Assert.assertTrue(entity2.getId().getLocalId() > entity1.getId().getLocalId());
    }

    public void testCreateTwoEntitiesInTwoTransactions() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity1 = txn.newEntity("Issue");
        txn.flush();
        final Entity entity2 = txn.newEntity("Issue");
        txn.flush();
        Assert.assertNotNull(entity1);
        Assert.assertTrue(entity1.getId().getTypeId() >= 0);
        Assert.assertTrue(entity1.getId().getLocalId() >= 0);
        Assert.assertNotNull(entity2);
        Assert.assertTrue(entity2.getId().getLocalId() > 0);
        Assert.assertTrue(entity2.getId().getLocalId() > entity1.getId().getLocalId());
    }

    public void testCreateAndGetSingleEntity() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
    }

    public void testRawProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("description", "it doesn't work");
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        ByteIterable rawValue = entity.getRawProperty("description");
        Assert.assertNotNull(rawValue);
        Assert.assertEquals("it doesn't work", getEntityStore().getPropertyTypes().entryToPropertyValue(rawValue).getData());
        entity.setProperty("description", "it works");
        txn.flush();
        sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        rawValue = entity.getRawProperty("description");
        Assert.assertNotNull(rawValue);
        Assert.assertEquals("it works", getEntityStore().getPropertyTypes().entryToPropertyValue(rawValue).getData());
    }

    public void testIntProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("size", 100);
        entity.setProperty("minus_size", -100);
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertEquals(100, entity.getProperty("size"));
        Assert.assertEquals(-100, entity.getProperty("minus_size"));
    }

    public void testLongProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("length", 0x10000ffffL);
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertEquals(0x10000ffffL, entity.getProperty("length"));
    }

    public void testStringProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("description", "This is a test issue");
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertEquals("This is a test issue", entity.getProperty("description"));
    }

    public void testDoubleAndFloatProperties() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("hitRate", 0.123456789);
        entity.setProperty("hitRate (float)", 0.123456789f);
        entity.setProperty("crude oil (WTI) price", -40.32);
        entity.setProperty("crude oil (WTI) price (float)", -40.32f);
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertEquals(0.123456789, entity.getProperty("hitRate"));
        Assert.assertEquals(0.123456789f, entity.getProperty("hitRate (float)"));
        Assert.assertEquals(-40.32, entity.getProperty("crude oil (WTI) price"));
        Assert.assertEquals(-40.32f, entity.getProperty("crude oil (WTI) price (float)"));
    }

    public void testDateProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        final Date date = new Date();
        entity.setProperty("date", date.getTime());
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        final Comparable dateProp = entity.getProperty("date");
        Assert.assertNotNull(dateProp);
        Assert.assertEquals(date.getTime(), dateProp);
        Assert.assertTrue(new Date().getTime() >= (Long) dateProp);
    }

    public void testBooleanProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("ready", true);
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertTrue((Boolean) entity.getProperty("ready"));
        entity.setProperty("ready", false);
        txn.flush();
        sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertNotNull(entity.getProperty("ready"));
        Assert.assertEquals(false, entity.getProperty("ready"));
    }

    public void testHeterogeneousProperties() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("description", "This is a test issue");
        entity.setProperty("size", 100);
        entity.setProperty("rank", 0.5);
        txn.flush();
        Assert.assertEquals("Issue", entity.getType());
        final Entity sameEntity = txn.getEntity(entity.getId());
        Assert.assertNotNull(sameEntity);
        Assert.assertEquals(entity.getType(), sameEntity.getType());
        Assert.assertEquals(entity.getId(), sameEntity.getId());
        Assert.assertEquals("This is a test issue", entity.getProperty("description"));
        Assert.assertEquals(100, entity.getProperty("size"));
        Assert.assertEquals(0.5, entity.getProperty("rank"));
    }

    @TestFor(issues = "XD-509")
    public void testComparableSetNewEmpty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        Assert.assertFalse(entity.setProperty("subsystems", newComparableSet()));
        Assert.assertTrue(entity.getPropertyNames().isEmpty());
    }

    @SuppressWarnings("unchecked")
    public void testComparableSetNew() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        final ComparableSet<String> subsystems = newComparableSet("Search Parser", "Agile Board", "Full Text Index", "REST API", "Workflow", "Agile Board");

        entity.setProperty("subsystems", subsystems);
        txn.flush();

        Comparable propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        ComparableSet<String> readSet = (ComparableSet) propValue;
        Assert.assertFalse(readSet.isEmpty());
        Assert.assertFalse(readSet.isDirty());
        Assert.assertEquals(subsystems, propValue);
    }

    @SuppressWarnings("unchecked")
    public void testComparableSetAdd() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");

        final ComparableSet<String> subsystems = newComparableSet("Search Parser", "Agile Board");
        entity.setProperty("subsystems", subsystems);
        txn.flush();

        Comparable propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        ComparableSet<String> updateSet = (ComparableSet) propValue;
        updateSet.addItem("Obsolete Subsystem");
        Assert.assertTrue(updateSet.isDirty());
        entity.setProperty("subsystems", updateSet);
        txn.flush();

        propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        updateSet = (ComparableSet) propValue;
        Assert.assertFalse(updateSet.isEmpty());
        Assert.assertFalse(updateSet.isDirty());
        Assert.assertEquals(newComparableSet("Search Parser", "Agile Board", "Obsolete Subsystem"), propValue);
    }


    public void testComparableSetAddAll() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");

        entity.setProperty("subsystems", newComparableSet("Search Parser", "Agile Board"));
        txn.flush();

        entity.setProperty("subsystems", newComparableSet("Search Parser", "Agile Board", "Obsolete Subsystem"));
        txn.flush();

        Comparable propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        Assert.assertEquals(newComparableSet("Search Parser", "Agile Board", "Obsolete Subsystem"), propValue);
    }

    @SuppressWarnings("unchecked")
    public void testComparableSetRemove() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");

        final ComparableSet<String> subsystems = newComparableSet("Search Parser", "Agile Board");
        entity.setProperty("subsystems", subsystems);
        txn.flush();

        Comparable propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        ComparableSet<String> updateSet = (ComparableSet) propValue;
        updateSet.removeItem("Agile Board");
        Assert.assertTrue(updateSet.isDirty());
        entity.setProperty("subsystems", updateSet);
        txn.flush();

        propValue = entity.getProperty("subsystems");
        Assert.assertTrue(propValue instanceof ComparableSet);
        updateSet = (ComparableSet) propValue;
        Assert.assertFalse(updateSet.isEmpty());
        Assert.assertFalse(updateSet.isDirty());
        Assert.assertEquals(newComparableSet("Search Parser"), propValue);
    }

    @TestFor(issues = "XD-509")
    public void testComparableSetClear() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");

        final ComparableSet<String> subsystems = newComparableSet("Search Parser", "Agile Board");
        entity.setProperty("subsystems", subsystems);
        txn.flush();

        entity.setProperty("subsystems", newComparableSet());
        txn.flush();

        Assert.assertNull(entity.getProperty("subsystems"));
    }

    private ComparableSet<String> newComparableSet(String... values) {
        ComparableSet<String> set = new ComparableSet<>();
        for (String value : values) {
            set.addItem(value);
        }
        return set;
    }

    public void testOverwriteProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity entity = txn.newEntity("Issue");
        entity.setProperty("description", "This is a test issue");
        txn.flush();
        Assert.assertEquals("This is a test issue", entity.getProperty("description"));
        entity.setProperty("description", "This is overriden test issue");
        txn.flush();
        Assert.assertEquals("This is overriden test issue", entity.getProperty("description"));
        entity.deleteProperty("description"); // for XD-262 I optimized this to prohibit such stuff
        entity.setProperty("description", 100);
        txn.flush();
        Assert.assertEquals(100, entity.getProperty("description"));
    }

    public void testDeleteProperty() {
        final StoreTransaction txn = getStoreTransaction();
        final Entity issue = txn.newEntity("Issue");
        issue.setProperty("description", "This is a test issue");
        txn.flush();
        Assert.assertEquals("This is a test issue", issue.getProperty("description"));
        issue.deleteProperty("description");
        txn.flush();
        Assert.assertNull(issue.getProperty("description"));
        final EntityIterable issues = txn.find("Issue", "description", "This is a test issue");
        Assert.assertFalse(issues.iterator().hasNext());
    }

    public void testReadingWithoutTransaction() throws Exception {
        StoreTransaction txn = getStoreTransaction();
        txn.getAll("Issue");
        try {
            final Entity issue = txn.newEntity("Issue");
            issue.setProperty("name", "my name");
            final Entity user = txn.newEntity("User");
            user.setProperty("name", "charisma user");
            issue.addLink("creator", user);
        } finally {
            txn.flush();
        }
        reinit();
        txn = getStoreTransaction();
        for (final Entity issue : txn.getAll("Issue")) {
            Assert.assertEquals("my name", issue.getProperty("name"));
            final Iterable<Entity> users = issue.getLinks("creator");
            for (final Entity user : users) {
                Assert.assertEquals("charisma user", user.getProperty("name"));
            }
        }
    }

    public void testClearingProperties() {
        final PersistentStoreTransaction txn = getStoreTransaction();
        final PersistentEntity issue = txn.newEntity("Issue");
        issue.setProperty("description", "This is a test issue");
        issue.setProperty("size", 0);
        issue.setProperty("rank", 0.5);
        txn.flush();
        Assert.assertNotNull(issue.getProperty("description"));
        Assert.assertNotNull(issue.getProperty("size"));
        Assert.assertNotNull(issue.getProperty("rank"));
        getEntityStore().clearProperties(txn, issue);
        txn.flush();
        Assert.assertNull(issue.getProperty("description"));
        Assert.assertNull(issue.getProperty("size"));
        Assert.assertNull(issue.getProperty("rank"));
    }

    public void testDeleteEntities() {
        final StoreTransaction txn = getStoreTransaction();
        txn.newEntity("Issue");
        txn.newEntity("Issue");
        txn.newEntity("Issue");
        txn.newEntity("Issue");
        txn.flush();
        int i = 0;
        for (final Entity issue : txn.getAll("Issue")) {
            if ((i++ & 1) == 0) {
                issue.delete();
            }
        }
        txn.flush();
        Assert.assertEquals(2, (int) txn.getAll("Issue").size());
    }

    public void testRenameEntityType() {
        final StoreTransaction txn = getStoreTransaction();
        for (int i = 0; i < 10; ++i) {
            txn.newEntity("Issue");
        }
        txn.flush();
        Assert.assertEquals(10, txn.getAll("Issue").size());
        getEntityStore().renameEntityType("Issue", "Comment");
        txn.flush();
        //noinspection SizeReplaceableByIsEmpty
        Assert.assertEquals(0, txn.getAll("Issue").size());
        Assert.assertEquals(10, txn.getAll("Comment").size());
    }

    public void testRenameNonExistingEntityType() {
        final StoreTransaction txn = getStoreTransaction();
        for (int i = 0; i < 10; ++i) {
            txn.newEntity("Issue");
        }
        txn.flush();
        Assert.assertEquals(10, txn.getAll("Issue").size());
        TestUtil.runWithExpectedException(() -> getEntityStore().renameEntityType("Comment", "Issue"), IllegalArgumentException.class);
    }

    public void testConcurrentSerializableChanges() throws InterruptedException {
        final Entity e1 = getEntityStore().computeInTransaction(txn -> txn.newEntity("E"));
        final int count = 100;
        final Runnable target = () -> {
            final StoreTransaction txn = getEntityStore().beginTransaction();
            try {
                for (int i = 0; i <= count; ++i) {
                    do {
                        e1.setProperty("i", i);
                        e1.setProperty("s", Integer.toString(i));
                    } while (!txn.flush());
                }
            } finally {
                txn.abort();
            }
        };
        final Thread t1 = new Thread(target);
        final Thread t2 = new Thread(target);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        getEntityStore().executeInReadonlyTransaction(txn -> {
            Assert.assertEquals(count, e1.getProperty("i"));
            Assert.assertEquals(Integer.toString(count), e1.getProperty("s"));
        });
    }

    public void testConcurrentCreationTypeIdsAreOk() throws InterruptedException {
        final int count = 100;
        final boolean[] itsOk = {true};
        final Runnable target = () -> {
            for (final int[] i = {0}; i[0] <= count; ++i[0]) {
                if (!getEntityStore().computeInTransaction(txn -> {
                    final Entity e = txn.newEntity("Entity" + i[0]);
                    if (e.getId().getTypeId() != i[0]) {
                        itsOk[0] = false;
                        return false;
                    }
                    return true;
                })) {
                    break;
                }
            }
        };
        final Thread t1 = new Thread(target);
        final Thread t2 = new Thread(target);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assert.assertTrue(itsOk[0]);
    }

    public void testAsciiUTFDecodingBenchmark() {
        final String s = "This is sample ASCII string of not that great size, but large enough to use in the benchmark";
        TestUtil.time("Constructing string from data input", () -> {
            try {
                final LightByteArrayOutputStream out = new LightByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(out);
                output.writeUTF(s);
                final InputStream stream = new ByteArraySizedInputStream(out.toByteArray(), 0, out.size());
                stream.mark(Integer.MAX_VALUE);
                for (int i = 0; i < 10000000; i++) {
                    stream.reset();
                    assertEquals(s, new DataInputStream(stream).readUTF());
                }
            } catch (IOException e) {
                throw ExodusException.toEntityStoreException(e);
            }
        });
        TestUtil.time("Constructing strings from bytes", () -> {
            final byte bytes[] = s.getBytes();
            for (int i = 0; i < 10000000; i++) {
                assertEquals(s, UTFUtil.fromAsciiByteArray(bytes, 0, bytes.length));
            }
        });
    }

    public void testTxnCachesIsolation() {
        final Entity issue = getEntityStore().computeInTransaction(txn -> {
            final Entity issue1 = txn.newEntity("Issue");
            issue1.setProperty("description", "1");
            return issue1;
        });
        final PersistentStoreTransaction txn = getStoreTransaction();
        txn.revert();
        Assert.assertEquals("1", issue.getProperty("description"));
        getEntityStore().executeInTransaction(txn1 -> issue.setProperty("description", "2"));
        txn.revert();
        Assert.assertEquals("2", issue.getProperty("description"));
    }

    public void testTxnCachesIsolation2() {
        final Entity issue = getEntityStore().computeInTransaction(txn -> {
            final Entity issue1 = txn.newEntity("Issue");
            issue1.setProperty("description", "1");
            return issue1;
        });
        final PersistentStoreTransaction txn = getStoreTransaction();
        txn.revert();
        Assert.assertEquals("1", issue.getProperty("description"));
        issue.setProperty("description", "2");
        getEntityStore().executeInTransaction(txn1 -> issue.setProperty("description", "3"));
        Assert.assertFalse(txn.flush());
        Assert.assertEquals("3", issue.getProperty("description"));
    }

    @TestFor(issues = "XD-530")
    public void testEntityStoreClear() {
        final PersistentEntityStoreImpl store = getEntityStore();
        final Entity user = store.computeInTransaction(txn -> {
            final Entity result = txn.newEntity("User");
            result.setProperty("login", "penemue");
            return result;
        });
        store.executeInReadonlyTransaction(txn -> Assert.assertEquals("penemue", user.getProperty("login")));
        store.clear();
        store.executeInReadonlyTransaction(txn -> Assert.assertNull(user.getProperty("login")));
        store.executeInTransaction(txn -> txn.newEntity("UserProfile"));
        store.executeInTransaction(txn -> txn.getSequence("qwerty").increment());
    }


    public void testSetPhantomLink() {
        setOrAddPhantomLink(false);
    }

    public void testAddPhantomLink() {
        setOrAddPhantomLink(true);
    }

    private void setOrAddPhantomLink(final boolean setLink) {
        final PersistentEntityStoreImpl store = getEntityStore();

        store.getEnvironment().getEnvironmentConfig().setGcEnabled(false);
        store.getConfig().setDebugTestLinkedEntities(true);

        final Entity issue = store.computeInTransaction(txn -> txn.newEntity("Issue"));
        final Entity comment = store.computeInTransaction(txn -> txn.newEntity("Comment"));
        final CountDownLatch startBoth = new CountDownLatch(2);
        final Semaphore deleted = new Semaphore(0);
        DeferredIO.getJobProcessor().queue(new Job() {
            @Override
            protected void execute() {
                store.executeInTransaction(txn -> {
                    startBoth.countDown();
                    try {
                        startBoth.await();
                    } catch (InterruptedException ignore) {
                    }
                    comment.delete();
                    txn.flush();
                    deleted.release();
                });
            }
        });
        final int[] i = {0};
        TestUtil.runWithExpectedException(() -> store.executeInTransaction(txn -> {
            final boolean first = i[0] == 0;
            if (first) {
                startBoth.countDown();
                try {
                    startBoth.await();
                } catch (InterruptedException ignore) {
                }
            }
            ++i[0];
            if (setLink) {
                issue.setLink("comment", comment);
            } else {
                issue.addLink("comment", comment);
            }
            if (first) {
                deleted.acquireUninterruptibly();
            }
        }), PhantomLinkException.class);
        Assert.assertEquals(2, i[0]);
        store.executeInReadonlyTransaction(txn -> Assert.assertNull(issue.getLink("comment")));
    }
}
