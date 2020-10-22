/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * ReadWriteLock��ʵ�֣�֧����ReentrantLock���Ƶ����塣
 * <p>���������������:
 *
 * <ul>
 * <li><b>���˳��</b>
 *
 * <p>���಻ǿ�ƶ������ʽ��ж�д�����ȼ����򡣵��ǣ���֧��һ����ѡ�Ĺ�ƽ���ԡ�
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>������Ϊ�ǹ�ƽ(Ĭ��)ʱ����д������Ŀ˳����δָ���ģ���ȡ���ڿ�������Լ����
 * �������õķǹ�ƽ�����������ڵ��ӳ�һ��������д�̣߳���ͨ�����бȹ�ƽ�����ߵ���������
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>������Ϊ��ƽʱ���߳�ʹ�ý��Ƶ���˳����Ծ������롣
 * ����ǰ���е������ͷ�ʱ��ҪôΪ�ȴ�ʱ�����д�̷߳���д����
 * ҪôΪһ��ȴ�ʱ������еȴ���д�̶߳����Ķ��̷߳��������
 *
 * <p>�������д����������һ�����ڵȴ���д�̣߳���ô��ͼ��ȡһ����ƽ�Ķ���(��reentrantly)���߳̽�������
 * �ڵ�ǰ���ϵĵȴ�д�̻߳�ò��ͷ�д��֮ǰ���̲߳����ȡ������
 * ��Ȼ�����һ���ȴ���д�̷߳��������ĵȴ�������һ���������߳���Ϊ��������ĵȴ��̣߳�
 * ��д���ǿ��еģ���ô��Щ���߳̽������������
 *
 * <p>���Ƕ�����д�����ǿ��е�(����ζ��û�еȴ����߳�)��������ͼ��ȡһ����ƽ��д��(��reentrantly)���߳̽�������
 * (ע�⣬��������ReentrantReadWriteLock.ReadLock.tryLock()��
 * ReentrantReadWriteLock.WriteLock.tryLock()���������������ƽ���ã�
 * �����ڿ��ܵ�����£��������ڵȴ����߳���Σ�����������ȡ����)
 * <p>
 * </dl>
 *
 * <li><b>Reentrancy</b>
 *
 * <p>����������ȡ����д������ReentrantLock����ʽ���»�ȡ����д����
 * ��д�̳߳��е�����д�������ͷ�֮ǰ�����������ȡ���ǲ�����ġ�
 * ���⣬д�������Ի�ö���������֮���С�������Ӧ�ó����У�
 * ���������ڶ��ڶ�����ִ�ж��ķ����ĵ��û�ص��ڼ䱣��д��ʱ�ǳ����á�
 * ���һ����ȡ����ͼ��ȡд����������Զ����ɹ���
 *
 *
 * <li><b>Lock downgrading</b>
 * <p>�������Ի������д������Ϊ�����������ǻ�ȡд����Ȼ���Ƕ�����Ȼ���ͷ�д�������ǣ��Ӷ���������д���ǲ����ܵġ�
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>������д����֧��������ȡ�ڼ��жϡ�
 *
 * <li><b>{@link Condition} support</b>
 * <p>д���ṩ��һ������ʵ�֣�����д���������Ϊ��ReentrantLock.newcondition()
 * ΪReentrantLock�ṩ������ʵ����ͬ����Ȼ��������ֻ����д��һ��ʹ�á�
 *
 * <p>������֧��������readLock(). newcondition()�׳�UnsupportedOperationException��
 *
 * <li><b>Instrumentation</b>
 * <p>����֧������ȷ�����Ƿ񱻳��л����ķ�������Щ������Ϊ����ϵͳ״̬����Ƶģ�����Ϊͬ�����ƶ���Ƶġ�
 * ���������л�������������Ϊ��ʽ��ͬ:�����л��������ڽ���״̬�������������л�ʱ��״̬�޹ء�
 * ʾ���÷�������Ĵ�����ʾ������ڸ��»����ִ��������(�쳣�������Է�Ƕ�׷�ʽ��������ʱ�ر���):
 * </ul>
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * ReentrantReadWriteLocks������ĳЩ���ϵ�ĳЩ��;�иĽ������ԡ�
 * ͨ����ֻ����Ԥ�ڼ��Ϻܴ󡢶��̱߳�д�̸߳���ط��ʼ��ϡ����Ҳ����Ŀ�������ͬ������ʱ����ֵ����������
 * ���磬������һ��ʹ��TreeMap���࣬���TreeMapӦ�úܴ󣬲��ҿ��Բ������ʡ�
 *
 *  <pre> {@code
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<String, Data>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}</pre>
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>����֧�����65535���ݹ�д����65535����������ͼ������Щ���ƻᵼ�����������׳�����
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** �ṩ�������ڲ��� */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** �ṩд�����ڲ��� */
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** ִ�����е�ͬ������ */
    final Sync sync;

    /**
     * ����һ���µ� {@code ReentrantReadWriteLock} with
     * Ĭ�� (�ǹ�ƽ) ˳������.
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * ����һ���µ� {@code ReentrantReadWriteLock} with
     * the�����Ĺ�ƽ����.
     *
     * @param fair {@code true} �����ʹ�ù�ƽ��˳�����
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync(); // ��ƽ��ǹ�ƽͬ����
        readerLock = new ReadLock(this); // ���� ������
        writerLock = new WriteLock(this); // д�� ������
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    /**
     * �������д����ͬ��ʵ��
     * ���๫ƽ��ǹ�ƽ�İ汾
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16; // ��16λΪ��������16λΪд��
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT); // ��д����λ 65536
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1; // ��д���������
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // ��ȡд����������

        /** ���ڼ�����ж������߳���  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** ���ڼ������д���߳���  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * ÿ���̳߳��ж�ȡ���ļ���
         * ������ ThreadLocal; ������ cachedHoldCounter
         */
        static final class HoldCounter {
            int count = 0;
            // ʹ��id, ��������, ȥ����������������
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal ����. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            // ��дthreadLocal��initiaValue����
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * ��ǰ�̳߳��еĿ�������������������ڹ��캯����readObject�г�ʼ����
         * ���̵߳Ķ����ּ����½���0ʱɾ����
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * �ɹ���ȡreadLock�����һ���̵߳ĳ��м���������ͨ������½�ʡ��ThreadLocal���ң�
         * ��Ϊ��һ��Ҫ�������߳������һ��Ҫ��ȡ�ġ����Ƿ���ʧ�Եģ���Ϊ��ֻ����Ϊһ������ʹ�ã�
         * ���Ҷ����̻߳�����˵�ǳ��á�
         *
         * <p>���ԱȻ�������ּ������̻߳�ø��ã�����ͨ�����������̵߳���������������������
         *
         * <p>ͨ�����Ե����ݾ�������;�������ڴ�ģ�͵������ֶκͷǿձ�֤��
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader�ǻ�ö����ĵ�һ���̡߳�firstReaderHoldCount��firstReader�ĳ��м�����
         *
         * <p>��׼ȷ��˵��firstReader�����һ�ν����������0����Ϊ1��Ωһ�̣߳����Ҵ���ʱ���û���ͷŶ���;
         * ���û���������̣߳���Ϊ�ա�
         *
         * <p>�����߳��ڲ��ͷŶ������������ֹ�����򲻻ᵼ��������������ΪtryReleaseShared��������Ϊnull��
         *
         * <p>ͨ�����Ե����ݾ�������;�������ڴ�ģ�͵�out- thin-air��֤���á�
         *
         * <p>��ʹ�öԷǾ�����=���Ķ����еĸ��ٷǳ����ˡ�
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ȷ��readhold�Ŀɼ���
        }

        /*
         * ��ȡ�ͷ����Թ�ƽ���ͷǹ�ƽ��ʹ����ͬ�Ĵ��룬���ڶ��зǿ�ʱ�Ƿ�/���������������������ͬ��
         */

        /**
         * �����ǰ�߳��ڳ��Ի�ȡ����ʱ(�������ʸ�������)Ӧ���������򷵻�true����Ϊ������Ϊ��ȡ���������ڵȴ����̡߳�
         */
        abstract boolean readerShouldBlock();

        /**
         * �����ǰ�߳��ڳ��Ի�ȡд��ʱ(�������ʸ�������)Ӧ���������򷵻�true����Ϊ������Ϊ��ȡ���������ڵȴ����̡߳�
         */
        abstract boolean writerShouldBlock();

        /*
         * ��ע�⣬tryRelease��tryAcquire���Ը����������á�
         * ��ˣ����ǵĲ�������ͬʱ��������д���У�������Щ���������ȴ��ڼ��ͷţ�����tryAcquire�����½�����
         */

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively()) // ������д���򱨴�
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases; // �ͷź�״ֵ̬
            boolean free = exclusiveCount(nextc) == 0; // д��������Ϊ0���ͷ�
            if (free)
                setExclusiveOwnerThread(null); // ���õ�ǰ����������Ϊnull
            setState(nextc); // ����״̬
            return free; // �������Ƿ��ͷ�
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. ��������������д����������������ǲ�ͬ���̣߳�ʧ�ܡ�
             * 2. ����������ͣ���ʧ�ܡ�(��ֻ����count�Ѿ����������·�����)
             * 3. ��������ǿ������ȡ����в�����������߳����ʸ�����������ǣ������״̬�����������ߡ�
             */
            Thread current = Thread.currentThread(); // ��ǰ�߳�
            int c = getState(); // ��״̬
            int w = exclusiveCount(c); // д��������
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false; // ���д����Ϊ0 ���� д������Ϊ0���ǵ�ǰ�̲߳���д�����̣߳����ȡʧ��
                if (w + exclusiveCount(acquires) > MAX_COUNT) // ���д���������������ֵ���׳��쳣
                    throw new Error("Maximum lock count exceeded");
                // ����������ȡ
                setState(c + acquires);
                return true; // ���������
            }
            if (writerShouldBlock() || // д��δ����ס������д����״̬
                !compareAndSetState(c, c + acquires))
                return false; // �����ʧ��
            setExclusiveOwnerThread(current); // ���õ�ǰ�̳߳�����
            return true; // ������ɹ�
        }

        protected final boolean tryReleaseShared(int unused) { // ��ͼ�ͷŹ�����
            Thread current = Thread.currentThread(); // �õ���ǰ�߳�
            if (firstReader == current) { // �����ǰ�߳��ǵ�һ�λ�ȡ�������߳�
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)  // �����һ�λ�ȡ����������Ϊ1
                    firstReader = null; // �ͷŵ�һ������������
                else
                    firstReaderHoldCount--; // ����������һ
            } else { // �ǵ�һ����ȡ�������߳�
                // ���һ�λ�ȡ��ȡ�����̺߳ͼ���
                HoldCounter rh = cachedHoldCounter;
                // ���HoldCounterΪnull���򱣴�ķǵ�ǰ�̵߳���Ϣ
                if (rh == null || rh.tid != getThreadId(current))
                    // ���������л�ȡ��ǰ�̵߳���Ϣ
                    rh = readHolds.get();
                int count = rh.count; // �õ�����������
                if (count <= 1) { // �������С��1��������������Ƴ�
                    readHolds.remove();
                    if (count <= 0) // �������С��0�������쳣
                        throw unmatchedUnlockException();
                }
                --rh.count; // ����������-1
            }
            for (;;) { // ����
                int c = getState(); // ��״̬
                int nextc = c - SHARED_UNIT; // ������-1
                if (compareAndSetState(c, nextc)) // ����state
                    // �ͷŶ�������Ӱ����������
                    // ���ǣ������д�����ڶ��ǿ��еģ���ô����������ȴ���д��������������
                    return nextc == 0; // �����Ƿ񻹴��ڶ���
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        } // ��ͼȥ������������ǲ�δ����ǰ�߳���ס

        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. �����һ���̳߳���д������ʧ�ܡ�
             * 2. ���򣬴��߳̽����ʸ�����wrt״̬�������ѯ�����Ƿ�Ӧ�����ڶ��в��Զ�������
             * ������ǣ�����ͨ�����״̬�͸��¼��������衣
             * ע�⣬stepû�м�������Ļ�ȡ�������Ƴٵ������汾���Ա����ڸ����͵Ĳ�����������¼����м�����
             * 3. �����2��ʧ�ܣ�Ҫô����Ϊ�߳���Ȼ������������Ҫô��CASʧ�ܻ�������ͣ�
             * �����ӵ�������������ѭ���İ汾��
             */
            Thread current = Thread.currentThread(); // ��ǰ�߳�
            int c = getState(); // ��ǰ״̬
            if (exclusiveCount(c) != 0 && // д������Ϊ0
                getExclusiveOwnerThread() != current) // �������̲߳�Ϊ��ǰ�߳�
                return -1; // ��ȡʧ��
            int r = sharedCount(c);  // ��������
            if (!readerShouldBlock() && // ����δ����
                r < MAX_COUNT && // ����С�����ֵ
                compareAndSetState(c, c + SHARED_UNIT)) { // ������+1
                if (r == 0) { // ��������Ϊ0
                    firstReader = current; // ���õ�ǰ�߳�δ��һ����ȡ������
                    firstReaderHoldCount = 1; // ����Ϊ1
                } else if (firstReader == current) { // ����
                    firstReaderHoldCount++; // +1
                } else {
                    HoldCounter rh = cachedHoldCounter; // �õ�ThreadocalMap
                    if (rh == null || rh.tid != getThreadId(current)) // Ϊnull����ǵ�ǰ�߳�
                        cachedHoldCounter = rh = readHolds.get(); // �������л�ȡ
                    else if (rh.count == 0) // ������Ϊ0
                        readHolds.set(rh); // ����
                    rh.count++; // +1
                }
                return 1; // ��ȡ�ɹ�
            }
            // ͨ��������ʽ�����ȡ��ʧ�ܵ����
            return fullTryAcquireShared(current);
        }

        /**
         * ��ȡ��ȡ�������汾������CAS��ʧ�Ϳ������ȡ��tryacquirered��û�д���
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * ���Ĵ�����һ���̶�����tryacquirered�Ĵ���������ģ����ܵ���˵�Ƚϼ򵥣�
             * ��Ϊ��û��ʹtryacquirered�����Ժ��ӳٶ�ȡhold count֮��Ľ�����ø��ӡ�
             */
            HoldCounter rh = null;
            for (;;) { // ����
                int c = getState(); // ��̬
                if (exclusiveCount(c) != 0) { // д����������Ϊ0
                    if (getExclusiveOwnerThread() != current) // д������Ϊ��ǰ�߳�
                        return -1; // ��ȡʧ��
                    // ����д��������������
                    // would cause deadlock.
                } else if (readerShouldBlock()) { // �������Ӧ�ñ�����
                    // ȷ��û�л�ÿ��������
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0) // ��δ�����������
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1; // ��ȡʧ��
                    }
                }
                if (sharedCount(c) == MAX_COUNT) // �������������������ֵ���׳��쳣
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) { // ���ö�������
                    if (sharedCount(c) == 0) { // ��������Ϊ0�Ļ�
                        firstReader = current; // ��һ����ȡ����
                        firstReaderHoldCount = 1; // ��������
                    } else if (firstReader == current) { // ����
                        firstReaderHoldCount++; // +1
                    } else { // �ǵ�һ����ȡ��������ThreadLocalMap
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // �����ͷ�
                    }
                    return 1; // ��ȡ�ɹ�
                }
            }
        }

        /**
         * ִ��д������ enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * ִ�ж����� enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false; // ʧ��
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() { // �Ƿ����д����
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // �����ⲿ��ķ���

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() { // ��ӵ�е��߳�
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() { // ��������
            return sharedCount(getState());
        }

        final boolean isWriteLocked() { // �Ƿ�д��
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() { // д������
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() { // ������������
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        final int getCount() { return getState(); }
    }

    /**
     * Nonfair version of Sync
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() { // �ǹ�ƽ��д��Ӧ�ñ�����
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() { // �����Ƿ�Ӧ�ñ�����
            /* ��Ϊһ�ֱ�������д������������ʽ�����������ʱ���ֵ��߳��Ƕ��е�ͷ(�������)��
             * ������������ֻ��һ�ָ���ЧӦ����Ϊ������������õĶ�ȡ��������һ���ȴ���д������
             * �������ȡ����û�дӶ�����ɾ�������¶�ȡ������������
             */
            return apparentlyFirstQueuedIsExclusive(); // �Ƿ��Ƕ��еĵ�һ���߳�
        }
    }

    /**
     * Fair version of Sync
     */
    static final class FairSync extends Sync { // ��ƽ��
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() { // �Ƿ���ǰ��
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() { // �Ƿ���ǰ��
            return hasQueuedPredecessors();
        }
    }

    /**
     * ���� {@link ReentrantReadWriteLock#readLock}.
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * ���๹�췽��
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * ��ȡ��
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then
         * the current thread becomes disabled for thread scheduling
         * purposes and lies dormant until the read lock has been acquired.
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * �ж�ģʽ��ȡ��
         *
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * ��ȡ��
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. Even when this lock has been set to use a
         * fair ordering policy, a call to {@code tryLock()}
         * <em>will</em> immediately acquire the read lock if it is
         * available, whether or not other threads are currently
         * waiting for the read lock.  This &quot;barging&quot; behavior
         * can be useful in certain circumstances, even though it
         * breaks fairness. If you want to honor the fairness setting
         * for this lock, then use {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS) } which is almost equivalent
         * (it also detects interruption).
         *
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value
         * {@code false}.
         *
         * @return {@code true} if the read lock was acquired
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * Acquires the read lock if the write lock is not held by
         * another thread within the given waiting time and the
         * current thread has not been {@linkplain Thread#interrupt
         * interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. If this lock has been set to use a fair
         * ordering policy then an available lock <em>will not</em> be
         * acquired if any other threads are waiting for the
         * lock. This is in contrast to the {@link #tryLock()}
         * method. If you want a timed {@code tryLock} that does
         * permit barging on a fair lock then combine the timed and
         * un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the read lock
         * @param unit the time unit of the timeout argument
         * @return {@code true} if the read lock was acquired
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * �ͷ���
         *
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts.
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * Throws {@code UnsupportedOperationException} because
         * {@code ReadLocks} do not support conditions.
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
    }

    /**
     * The lock returned by method {@link ReentrantReadWriteLock#writeLock}.
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * ���๹����
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * ��ȡд��
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * <p>If the current thread already holds the write lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until the write lock has been acquired, at which
         * time the write lock hold count is set to one.
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * �ж�ģʽ��ȡд��
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the write lock is acquired by the current thread then the
         * lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * Acquires the write lock only if it is not held by another thread
         * at the time of invocation.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. Even when this lock has
         * been set to use a fair ordering policy, a call to
         * {@code tryLock()} <em>will</em> immediately acquire the
         * lock if it is available, whether or not other threads are
         * currently waiting for the write lock.  This &quot;barging&quot;
         * behavior can be useful in certain circumstances, even
         * though it breaks fairness. If you want to honor the
         * fairness setting for this lock, then use {@link
         * #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
         * which is almost equivalent (it also detects interruption).
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * Acquires the write lock if it is not held by another thread
         * within the given waiting time and the current thread has
         * not been {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock
         * <em>will not</em> be acquired if any other threads are
         * waiting for the write lock. This is in contrast to the {@link
         * #tryLock()} method. If you want a timed {@code tryLock}
         * that does permit barging on a fair lock then combine the
         * timed and un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the write lock
         * @param unit the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * �ͷ���
         *
         * <p>If the current thread is the holder of this lock then
         * the hold count is decremented. If the hold count is now
         * zero then the lock is released.  If the current thread is
         * not the holder of this lock then {@link
         * IllegalMonitorStateException} is thrown.
         *
         * @throws IllegalMonitorStateException if the current thread does not
         * hold this lock
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * Returns a {@link Condition} instance for use with this
         * {@link Lock} instance.
         * <p>The returned {@link Condition} instance supports the same
         * usages as do the {@link Object} monitor methods ({@link
         * Object#wait() wait}, {@link Object#notify notify}, and {@link
         * Object#notifyAll notifyAll}) when used with the built-in
         * monitor lock.
         *
         * <ul>
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * <li> Waiting threads are signalled in FIFO order.
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * Returns a string identifying this lock, as well as its lock
         * state.  The state, in brackets includes either the String
         * {@code "Unlocked"} or the String {@code "Locked by"}
         * followed by the {@linkplain Thread#getName name} of the owning thread.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }

        /**
         * Queries if this write lock is held by the current thread.
         * Identical in effect to {@link
         * ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         *
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * Queries the number of holds on this write lock by the current
         * thread.  A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action.  Identical in effect
         * to {@link ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /**
     * �Ƿ�ƽ��
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * �õ���ǰ�������߳�
     * Returns the thread that currently owns the write lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * ��������
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * �Ƿ�д��
     * Queries if the write lock is held by any thread. This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * �Ƿ������
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * д��������
     * Queries the number of reentrant write holds on this lock by the
     * current thread.  A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * ��������
     * Queries the number of reentrant read holds on this lock by the
     * current thread.  A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * д������
     * Returns a collection containing threads that may be waiting to
     * acquire the write lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * ��������
     * Returns a collection containing threads that may be waiting to
     * acquire the read lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * �Ƿ��ж���
     * Queries whether any threads are waiting to acquire the read or
     * write lock. Note that because cancellations may occur at any
     * time, a {@code true} return does not guarantee that any other
     * thread will ever acquire a lock.  This method is designed
     * primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * �Ƿ��ڶ���
     * Queries whether the given thread is waiting to acquire either
     * the read or write lock. Note that because cancellations may
     * occur at any time, a {@code true} return does not guarantee
     * that this thread will ever acquire a lock.  This method is
     * designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * ���г���
     * Returns an estimate of the number of threads waiting to acquire
     * either the read or write lock.  The value is only an estimate
     * because the number of threads may change dynamically while this
     * method traverses internal data structures.  This method is
     * designed for use in monitoring of the system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * ����
     * Returns a collection containing threads that may be waiting to
     * acquire either the read or write lock.  Because the actual set
     * of threads may change dynamically while constructing this
     * result, the returned collection is only a best-effort estimate.
     * The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * �Ƿ�ȴ�
     * Queries whether any threads are waiting on the given condition
     * associated with the write lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * �ȴ�����
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with the write lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * �ȴ��߳�
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with the write lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * �߳�id
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() is not final, and has been known to be overridden in
     * ways that do not preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
