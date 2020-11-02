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
 * ReadWriteLock的实现，支持与ReentrantLock类似的语义。
 * <p>该类具有以下属性:
 *
 * <ul>
 * <li><b>获得顺序：</b>
 *
 * <p>该类不强制对锁访问进行读写器优先级排序。但是，它支持一个可选的公平策略。
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>当构造为非公平(默认)时，读写锁的条目顺序是未指定的，这取决于可重入性约束。
 * 持续争用的非公平锁可能无限期地延迟一个或多个读写线程，但通常具有比公平锁更高的吞吐量。
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>当构造为公平时，线程使用近似到达顺序策略竞争进入。
 * 当当前持有的锁被释放时，要么为等待时间最长的写线程分配写锁，
 * 要么为一组等待时间比所有等待的写线程都长的读线程分配读锁。
 *
 * <p>如果持有写锁，或者有一个正在等待的写线程，那么试图获取一个公平的读锁(非reentrantly)的线程将阻塞。
 * 在当前最老的等待写线程获得并释放写锁之前，线程不会获取读锁。
 * 当然，如果一个等待的写线程放弃了它的等待，留下一个或多个读线程作为队列中最长的等待线程，
 * 而写锁是空闲的，那么这些读线程将被分配读锁。
 *
 * <p>除非读锁和写锁都是空闲的(这意味着没有等待的线程)，否则试图获取一个公平的写锁(非reentrantly)的线程将阻塞。
 * (注意，非阻塞的ReentrantReadWriteLock.ReadLock.tryLock()和
 * ReentrantReadWriteLock.WriteLock.tryLock()方法不遵守这个公平设置，
 * 并且在可能的情况下，不管正在等待的线程如何，都会立即获取锁。)
 * <p>
 * </dl>
 *
 * <li><b>Reentrancy</b>
 *
 * <p>这个锁允许读取器和写入器以ReentrantLock的形式重新获取读或写锁。
 * 在写线程持有的所有写锁都被释放之前，不可重入读取器是不允许的。
 * 此外，写入器可以获得读锁，但反之则不行。在其他应用程序中，
 * 可重入性在对在读锁下执行读的方法的调用或回调期间保持写锁时非常有用。
 * 如果一个读取器试图获取写锁，它将永远不会成功。
 *
 *
 * <li><b>Lock downgrading</b>
 * <p>可重入性还允许从写锁降级为读锁，方法是获取写锁，然后是读锁，然后释放写锁。但是，从读锁升级到写锁是不可能的。
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>读锁和写锁都支持在锁获取期间中断。
 *
 * <li><b>{@link Condition} support</b>
 * <p>写锁提供了一个条件实现，它在写锁方面的行为与ReentrantLock.newcondition()
 * 为ReentrantLock提供的条件实现相同。当然，此条件只能与写锁一起使用。
 *
 * <p>读锁不支持条件，readLock(). newcondition()抛出UnsupportedOperationException。
 *
 * <li><b>Instrumentation</b>
 * <p>该类支持用于确定锁是否被持有或竞争的方法。这些方法是为监视系统状态而设计的，不是为同步控制而设计的。
 * 这个类的序列化与内置锁的行为方式相同:反序列化的锁处于解锁状态，而与它在序列化时的状态无关。
 * 示例用法。下面的代码演示了如何在更新缓存后执行锁降级(异常处理在以非嵌套方式处理多个锁时特别棘手):
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
 * ReentrantReadWriteLocks可用于某些集合的某些用途中改进并发性。
 * 通常，只有在预期集合很大、读线程比写线程更多地访问集合、并且操作的开销超过同步开销时，才值得这样做。
 * 例如，这里有一个使用TreeMap的类，这个TreeMap应该很大，并且可以并发访问。
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
 * <p>该锁支持最多65535个递归写锁和65535个读锁。试图超过这些限制会导致锁定方法抛出错误。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** 提供读锁的内部类 */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** 提供写锁的内部类 */
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** 执行所有的同步操作 */
    final Sync sync;

    /**
     * 创建一个新的 {@code ReentrantReadWriteLock} with
     * 默认 (非公平) 顺序属性.
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 创建一个新的 {@code ReentrantReadWriteLock} with
     * the给定的公平策略.
     *
     * @param fair {@code true} 如果锁使用公平的顺序策略
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync(); // 公平或非公平同步锁
        readerLock = new ReadLock(this); // 读锁 共享锁
        writerLock = new WriteLock(this); // 写锁 排它锁
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    /**
     * 可重入读写锁的同步实现
     * 子类公平或非公平的版本
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16; // 高16位为读锁，低16位为写锁
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT); // 读写锁单位 65536
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1; // 读写锁最大数量
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // 获取写锁数的掩码

        /** 用于计算持有读锁的线程数  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** 用于计算持有写锁线程数  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * 每个线程持有读取锁的计数
         * 保存在 ThreadLocal; 缓存在 cachedHoldCounter
         */
        static final class HoldCounter {
            int count = 0;
            // 使用id, 而非引用, 去避免垃圾回收问题
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal 子类. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            // 重写threadLocal的initiaValue方法
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 当前线程持有的可重入读锁的数量。仅在构造函数和readObject中初始化。
         * 当线程的读保持计数下降到0时删除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 成功获取readLock的最后一个线程的持有计数。这在通常情况下节省了ThreadLocal查找，
         * 因为下一个要发布的线程是最后一个要获取的。这是非易失性的，因为它只是作为一种启发使用，
         * 而且对于线程缓存来说非常好。
         *
         * <p>可以比缓存读保持计数的线程活得更久，但是通过不保留对线程的引用来避免垃圾保留。
         *
         * <p>通过良性的数据竞争访问;依赖于内存模型的最终字段和非空保证。
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader是获得读锁的第一个线程。firstReaderHoldCount是firstReader的持有计数。
         *
         * <p>更准确地说，firstReader是最后一次将共享计数从0更改为1的惟一线程，并且从那时起就没有释放读锁;
         * 如果没有这样的线程，则为空。
         *
         * <p>除非线程在不释放读锁的情况下终止，否则不会导致垃圾保留，因为tryReleaseShared将其设置为null。
         *
         * <p>通过良性的数据竞争访问;依赖于内存模型的out- thin-air保证引用。
         *
         * <p>这使得对非竞争读=锁的读持有的跟踪非常便宜。
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // 确保readhold的可见性
        }

        /*
         * 获取和发布对公平锁和非公平锁使用相同的代码，但在队列非空时是否/如何允许阻塞方面有所不同。
         */

        /**
         * 如果当前线程在尝试获取读锁时(或者有资格这样做)应该阻塞，则返回true，因为策略是为了取代其他正在等待的线程。
         */
        abstract boolean readerShouldBlock();

        /**
         * 如果当前线程在尝试获取写锁时(或者有资格这样做)应该阻塞，则返回true，因为策略是为了取代其他正在等待的线程。
         */
        abstract boolean writerShouldBlock();

        /*
         * 请注意，tryRelease和tryAcquire可以根据条件调用。
         * 因此，它们的参数可能同时包含读和写持有，所有这些都在条件等待期间释放，并在tryAcquire中重新建立。
         */

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively()) // 不持有写锁则报错
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases; // 释放后状态值
            boolean free = exclusiveCount(nextc) == 0; // 写入锁数量为0则释放
            if (free)
                setExclusiveOwnerThread(null); // 设置当前持有锁对象为null
            setState(nextc); // 设置状态
            return free; // 返回锁是否被释放
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. 如果读计数非零或写计数非零和所有者是不同的线程，失败。
             * 2. 如果计数饱和，则失败。(这只会在count已经非零的情况下发生。)
             * 3. 否则，如果是可重入获取或队列策略允许，则此线程有资格获得锁。如果是，则更新状态并设置所有者。
             */
            Thread current = Thread.currentThread(); // 当前线程
            int c = getState(); // 锁状态
            int w = exclusiveCount(c); // 写入锁数量
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false; // 如果写入锁为0 或者 写入锁不为0但是当前线程不是写入锁线程，则获取失败
                if (w + exclusiveCount(acquires) > MAX_COUNT) // 如果写入锁数量超过最大值则抛出异常
                    throw new Error("Maximum lock count exceeded");
                // 否则可重入获取
                setState(c + acquires);
                return true; // 获得锁返回
            }
            if (writerShouldBlock() || // 写入未阻塞住则设置写入锁状态
                !compareAndSetState(c, c + acquires))
                return false; // 获得锁失败
            setExclusiveOwnerThread(current); // 设置当前线程持有锁
            return true; // 获得锁成功
        }

        protected final boolean tryReleaseShared(int unused) { // 试图释放共享锁
            Thread current = Thread.currentThread(); // 拿到当前线程
            if (firstReader == current) { // 如果当前线程是第一次获取读锁的线程
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)  // 如果第一次获取读锁的数量为1
                    firstReader = null; // 释放第一个读锁的数量
                else
                    firstReaderHoldCount--; // 否则数量减一
            } else { // 非第一个获取读锁的线程
                // 最近一次获取读取锁的线程和技术
                HoldCounter rh = cachedHoldCounter;
                // 如果HoldCounter为null，或保存的非当前线程的信息
                if (rh == null || rh.tid != getThreadId(current))
                    // 从上下文中获取当前线程的信息
                    rh = readHolds.get();
                int count = rh.count; // 拿到可重入数量
                if (count <= 1) { // 如果次数小于1，则从上下文中移除
                    readHolds.remove();
                    if (count <= 0) // 如果次数小于0，则抛异常
                        throw unmatchedUnlockException();
                }
                --rh.count; // 否则锁数量-1
            }
            for (;;) { // 自旋
                int c = getState(); // 锁状态
                int nextc = c - SHARED_UNIT; // 共享锁-1
                if (compareAndSetState(c, nextc)) // 设置state
                    // 释放读锁不会影响其他读锁
                    // 但是，如果读写锁现在都是空闲的，那么它可能允许等待的写入器继续工作。
                    return nextc == 0; // 返回是否还存在读锁
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        } // 试图去解除读锁，但是并未被当前线程锁住

        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. 如果另一个线程持有写锁，则失败。
             * 2. 否则，此线程将有资格获得锁wrt状态，因此请询问它是否应该由于队列策略而阻塞。
             * 如果不是，尝试通过外壳状态和更新计数来授予。
             * 注意，step没有检查可重入的获取，它被推迟到完整版本，以避免在更典型的不可重入情况下检查持有计数。
             * 3. 如果第2步失败，要么是因为线程显然不符合条件，要么是CAS失败或计数饱和，
             * 则链接到具有完整重试循环的版本。
             */
            Thread current = Thread.currentThread(); // 当前线程
            int c = getState(); // 当前状态
            if (exclusiveCount(c) != 0 && // 写锁数不为0
                getExclusiveOwnerThread() != current) // 持有锁线程不为当前线程
                return -1; // 获取失败
            int r = sharedCount(c);  // 读锁数量
            if (!readerShouldBlock() && // 读锁未阻塞
                r < MAX_COUNT && // 数量小于最大值
                compareAndSetState(c, c + SHARED_UNIT)) { // 锁数量+1
                if (r == 0) { // 读锁数量为0
                    firstReader = current; // 设置当前线程未第一个获取读锁的
                    firstReaderHoldCount = 1; // 数量为1
                } else if (firstReader == current) { // 重入
                    firstReaderHoldCount++; // +1
                } else {
                    HoldCounter rh = cachedHoldCounter; // 拿到ThreadocalMap
                    if (rh == null || rh.tid != getThreadId(current)) // 为null，或非当前线程
                        cachedHoldCounter = rh = readHolds.get(); // 上下文中获取
                    else if (rh.count == 0) // 若数量为0
                        readHolds.set(rh); // 设置
                    rh.count++; // +1
                }
                return 1; // 获取成功
            }
            // 通过自旋方式解决获取锁失败的情况
            return fullTryAcquireShared(current);
        }

        /**
         * 读取获取的完整版本，处理CAS丢失和可重入读取，tryacquirered中没有处理。
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * 他的代码在一定程度上与tryacquirered的代码是冗余的，但总的来说比较简单，
             * 因为它没有使tryacquirered在重试和延迟读取hold count之间的交互变得复杂。
             */
            HoldCounter rh = null;
            for (;;) { // 自旋
                int c = getState(); // 锁态
                if (exclusiveCount(c) != 0) { // 写入锁数量不为0
                    if (getExclusiveOwnerThread() != current) // 写入锁不为当前线程
                        return -1; // 获取失败
                    // 持有写入锁，阻塞在这
                    // would cause deadlock.
                } else if (readerShouldBlock()) { // 如果读锁应该被阻塞
                    // 确认没有获得可重入读锁
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0) // 若未持有锁则清除
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1; // 获取失败
                    }
                }
                if (sharedCount(c) == MAX_COUNT) // 若共享锁数量超过最大值则抛出异常
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) { // 设置读锁数量
                    if (sharedCount(c) == 0) { // 读锁数量为0的话
                        firstReader = current; // 第一个获取读锁
                        firstReaderHoldCount = 1; // 读锁数量
                    } else if (firstReader == current) { // 重入
                        firstReaderHoldCount++; // +1
                    } else { // 非第一个获取锁，调用ThreadLocalMap
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // 缓存释放
                    }
                    return 1; // 获取成功
                }
            }
        }

        /**
         * 执行写入锁， enabling barging in both modes.
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
         * 执行读锁， enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false; // 失败
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

        protected final boolean isHeldExclusively() { // 是否持有写入锁
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 依赖外部类的方法

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() { // 锁拥有的线程
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() { // 读锁数量
            return sharedCount(getState());
        }

        final boolean isWriteLocked() { // 是否写锁
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() { // 写锁数量
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() { // 读锁持有数量
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
        final boolean writerShouldBlock() { // 非公平锁写入应该被阻塞
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() { // 读锁是否应该被阻塞
            /* 作为一种避免无限写器饥饿的启发式方法，如果暂时出现的线程是队列的头(如果存在)，
             * 则阻塞它。这只是一种概率效应，因为如果在其他启用的读取器后面有一个等待的写入器，
             * 而这个读取器还没有从队列中删除，则新读取器不会阻塞。
             */
            return apparentlyFirstQueuedIsExclusive(); // 是否是队列的第一个线程
        }
    }

    /**
     * Fair version of Sync
     */
    static final class FairSync extends Sync { // 公平锁
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() { // 是否有前任
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() { // 是否有前任
            return hasQueuedPredecessors();
        }
    }

    /**
     * 读锁 {@link ReentrantReadWriteLock#readLock}.
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * 子类构造方法
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取锁
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
         * 中断模式获取锁
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
         * 获取读
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
         * 释放锁
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
         * 子类构造器
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取写锁
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
         * 中断模式获取写锁
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
         * 释放锁
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
     * 是否公平锁
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 得到当前持有锁线程
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
     * 读锁数量
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 是否写锁
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
     * 是否持有锁
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 写锁重入量
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
     * 读锁数量
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
     * 写锁队列
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
     * 读锁队列
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
     * 是否有队列
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
     * 是否在队列
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
     * 队列长度
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
     * 队列
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
     * 是否等待
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
     * 等待长度
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
     * 等待线程
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
     * 线程id
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
