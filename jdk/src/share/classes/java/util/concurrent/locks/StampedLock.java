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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.LockSupport;

/**
 * 一种基于能力的锁，有三种模式用于控制读/写访问。StampedLock的状态由版本和模式组成。
 * 锁获取方法返回一个表示并控制对锁状态的访问的戳记;这些方法的“尝试”版本可能会返回特殊的值0来表示访问失败。
 * 锁释放和转换方法需要戳记作为参数，如果它们与锁的状态不匹配，则会失败。三种模式是:
 *
 * <ul>
 *
 *  <li>Write:方法writeLock()可能阻塞等待独占访问，返回一个可以在方法unlockWrite(long)中使用的戳记来释放锁。
 *  还提供了不定时和定时的tryWriteLock版本。当锁处于写模式时，可能不会获得读锁，所有乐观读验证都将失败。</li>
 *
 *  <li>Reading:方法readLock()可能阻塞等待非独占访问，返回一个可以在方法unlockRead(long)中使用的戳记来释放锁。
 *  还提供了不定时和定时的tryReadLock版本。</li>
 *
 *  <li>Optimistic Reading:方法tryOptimisticRead()仅在当前锁未处于写模式时才返回非零戳记。
 *  方法validate(long)返回true，如果在获取给定的戳记之后，锁还没有在写模式下获得。
 *  这种模式可以被认为是一个非常弱的读锁版本，可以被写入器在任何时候打破。
 *  对于短的只读代码段使用乐观模式通常可以减少争用并提高吞吐量。然而，它的使用本身是脆弱的。
 *  乐观读取部分应该只读取字段并将它们保存在局部变量中，以便在验证之后使用。
 *  在乐观模式下读取的字段可能非常不一致，
 *  因此只有在您足够熟悉数据表示以检查一致性和/或反复调用方法validate()时才会使用。
 *  例如，在首先读取对象或数组引用，然后访问其中一个字段、元素或方法时，通常需要这些步骤。</li>
 *
 * </ul>
 *
 * <p>该类还支持有条件地跨三种模式提供转换的方法。
 * 例如，方法tryConvertToWriteLock(long)尝试“升级”一个模式，
 * 如果(1)已经处于写入模式(2)处于读取模式，并且没有其他读取器，
 * 或者(3)处于乐观模式，且锁可用，则返回一个有效的写戳。
 * 这些方法的形式旨在帮助减少在基于回退的设计中出现的一些代码膨胀。
 *
 * <p>在开发线程安全组件时，StampedLocks被设计为内部实用程序。
 * 它们的使用依赖于它们所保护的数据、对象和方法的内部属性的知识。
 * 它们是不可重入的，所以锁定的实体不应该调用其他未知的方法，
 * 这些方法可能试图重新获取锁(尽管您可以将戳记传递给其他可以使用或转换它的方法)。
 * 读锁定模式的使用依赖于相关的代码部分是无副作用的。
 * 未经验证的乐观读取部分不能调用不知道可以容忍潜在不一致性的方法。
 * 戳记使用有限的表示，并且在密码上是不安全的(例如。，有效的印章可能是可猜测的)。
 * 邮票价值可在(不早于)连续运作一年后循环使用。未经使用或验证而持有超过此期限的戳记可能无法正确验证。
 * StampedLocks是可序列化的，但总是反序列化为初始解锁状态，因此它们对于远程锁定没有用处。
 *
 * <p>StampedLock的调度策略并不总是优先选择读写器，反之亦然。
 * 所有的“尝试”方法都是尽了最大努力的，不一定符合任何调度或公平策略。
 * 从获取或转换锁的任何“尝试”方法返回的零不会携带有关锁状态的任何信息;后续调用可能成功。
 *
 * <p>因为它支持跨多个锁模式的协调使用，所以这个类不直接实现锁或读写锁接口。
 * 但是，在只需要相关功能集的应用程序中，可以将StampedLock视为asReadLock()、
 * asWriteLock()或asReadWriteLock()。
 *
 * <p>下面说明了维护简单二维点的类中的一些用法。示例代码演示了一些try/catch约定，
 * 尽管这里并不严格需要它们，因为它们的主体中不可能出现异常。<br>
 *
 *  <pre>{@code
 * class Point {
 *   private double x, y;
 *   private final StampedLock sl = new StampedLock();
 *
 *   void move(double deltaX, double deltaY) { // 一个排它锁方法
 *     long stamp = sl.writeLock();
 *     try {
 *       x += deltaX;
 *       y += deltaY;
 *     } finally {
 *       sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   double distanceFromOrigin() { // 一个读锁方法
 *     long stamp = sl.tryOptimisticRead();
 *     double currentX = x, currentY = y;
 *     if (!sl.validate(stamp)) {
 *        stamp = sl.readLock();
 *        try {
 *          currentX = x;
 *          currentY = y;
 *        } finally {
 *           sl.unlockRead(stamp);
 *        }
 *     }
 *     return Math.sqrt(currentX * currentX + currentY * currentY);
 *   }
 *
 *   void moveIfAtOrigin(double newX, double newY) { // 升级
 *     // Could instead start with optimistic, not read mode
 *     long stamp = sl.readLock();
 *     try {
 *       while (x == 0.0 && y == 0.0) {
 *         long ws = sl.tryConvertToWriteLock(stamp);
 *         if (ws != 0L) {
 *           stamp = ws;
 *           x = newX;
 *           y = newY;
 *           break;
 *         }
 *         else {
 *           sl.unlockRead(stamp);
 *           stamp = sl.writeLock();
 *         }
 *       }
 *     } finally {
 *       sl.unlock(stamp);
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public class StampedLock implements java.io.Serializable {
    /*
     * Algorithmic notes:
     *
     * The design employs elements of Sequence locks
     * (as used in linux kernels; see Lameter's
     * http://www.lameter.com/gelato2005.pdf
     * and elsewhere; see
     * Boehm's http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html)
     * and Ordered RW locks (see Shirako et al
     * http://dl.acm.org/citation.cfm?id=2312015)
     *
     * Conceptually, the primary state of the lock includes a sequence
     * number that is odd when write-locked and even otherwise.
     * However, this is offset by a reader count that is non-zero when
     * read-locked.  The read count is ignored when validating
     * "optimistic" seqlock-reader-style stamps.  Because we must use
     * a small finite number of bits (currently 7) for readers, a
     * supplementary reader overflow word is used when the number of
     * readers exceeds the count field. We do this by treating the max
     * reader count value (RBITS) as a spinlock protecting overflow
     * updates.
     *
     * Waiters use a modified form of CLH lock used in
     * AbstractQueuedSynchronizer (see its internal documentation for
     * a fuller account), where each node is tagged (field mode) as
     * either a reader or writer. Sets of waiting readers are grouped
     * (linked) under a common node (field cowait) so act as a single
     * node with respect to most CLH mechanics.  By virtue of the
     * queue structure, wait nodes need not actually carry sequence
     * numbers; we know each is greater than its predecessor.  This
     * simplifies the scheduling policy to a mainly-FIFO scheme that
     * incorporates elements of Phase-Fair locks (see Brandenburg &
     * Anderson, especially http://www.cs.unc.edu/~bbb/diss/).  In
     * particular, we use the phase-fair anti-barging rule: If an
     * incoming reader arrives while read lock is held but there is a
     * queued writer, this incoming reader is queued.  (This rule is
     * responsible for some of the complexity of method acquireRead,
     * but without it, the lock becomes highly unfair.) Method release
     * does not (and sometimes cannot) itself wake up cowaiters. This
     * is done by the primary thread, but helped by any other threads
     * with nothing better to do in methods acquireRead and
     * acquireWrite.
     *
     * These rules apply to threads actually queued. All tryLock forms
     * opportunistically try to acquire locks regardless of preference
     * rules, and so may "barge" their way in.  Randomized spinning is
     * used in the acquire methods to reduce (increasingly expensive)
     * context switching while also avoiding sustained memory
     * thrashing among many threads.  We limit spins to the head of
     * queue. A thread spin-waits up to SPINS times (where each
     * iteration decreases spin count with 50% probability) before
     * blocking. If, upon wakening it fails to obtain lock, and is
     * still (or becomes) the first waiting thread (which indicates
     * that some other thread barged and obtained lock), it escalates
     * spins (up to MAX_HEAD_SPINS) to reduce the likelihood of
     * continually losing to barging threads.
     *
     * Nearly all of these mechanics are carried out in methods
     * acquireWrite and acquireRead, that, as typical of such code,
     * sprawl out because actions and retries rely on consistent sets
     * of locally cached reads.
     *
     * As noted in Boehm's paper (above), sequence validation (mainly
     * method validate()) requires stricter ordering rules than apply
     * to normal volatile reads (of "state").  To force orderings of
     * reads before a validation and the validation itself in those
     * cases where this is not already forced, we use
     * Unsafe.loadFence.
     *
     * The memory layout keeps lock state and queue pointers together
     * (normally on the same cache line). This usually works well for
     * read-mostly loads. In most other cases, the natural tendency of
     * adaptive-spin CLH locks to reduce memory contention lessens
     * motivation to further spread out contended locations, but might
     * be subject to future improvements.
     */

    private static final long serialVersionUID = -6001602636862214147L;

    /** CPU核数 */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** 线程进入队列前最大自旋次数 */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;

    /** 队列头结点阻塞前的最大自旋次数 */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 0;

    /** 重新阻塞前的最大自旋次数 */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 0;

    /** 等待溢出自旋锁时的一段时间 */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1

    /** 在溢出之前用于读取器计数的位的数目 */
    private static final int LG_READERS = 7;

    // Values for lock state and stamp operations
    private static final long RUNIT = 1L;
    private static final long WBIT  = 1L << LG_READERS; // 1000,0000
    private static final long RBITS = WBIT - 1L; // 111,1111
    private static final long RFULL = RBITS - 1L; // 111,1110
    private static final long ABITS = RBITS | WBIT; // 1111,1111
    private static final long SBITS = ~RBITS; // note overlap with ABITS

    // 锁状态初始化值; 避免失败值0
    private static final long ORIGIN = WBIT << 1;

    // 取消获取方法特殊值 so caller can throw IE
    private static final long INTERRUPTED = 1L;

    // 节点状态值; order matters
    private static final int WAITING   = -1; // 等待
    private static final int CANCELLED =  1; // 取消

    // 节点模式 (int代替boolean计算)
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /** 等待节点  */
    static final class WNode { // 写节点
        volatile WNode prev; // 前节点
        volatile WNode next; // 下一个节点
        volatile WNode cowait;    // 读表
        volatile Thread thread;   // 可能阻塞非null线程
        volatile int status;      // 0, WAITING, or CANCELLED
        final int mode;           // RMODE or WMODE
        WNode(int m, WNode p) { mode = m; prev = p; }
    }

    /** 头 of CLH queue */
    private transient volatile WNode whead;
    /** 尾 (last) of CLH queue */
    private transient volatile WNode wtail;

    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    /** 锁状态 */
    private transient volatile long state;
    /** 额外的读计数 当状态读数量饱和时 */
    private transient int readerOverflow;

    /**
     * 创建一个新的锁, 初始化为非锁状态
     */
    public StampedLock() {
        state = ORIGIN; // 1,0000,0000
    }

    /**
     * 独占获取锁，必要时阻塞，直到可用。
     *
     * @return 可用于解锁或转换模式的戳记
     */
    public long writeLock() {
        long s, next;  //  仅在完全解锁的情况下绕过acquireWrite ；s 当前状态值；next 下一个状态值
        return ((((s = state) & ABITS) == 0L && // 1,0000,0000 & 1111,1111 = 0 没有读锁和写锁
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ? // 1,1000,0000 设置写锁
                next : acquireWrite(false, 0L)); // 成功返回next，失败尝试获取写锁
    }

    /**
     * 如果锁是立即可用的，则独占地获取锁。
     *
     * @return 可用于解锁或转换模式的戳记，如果锁不可用则为零
     */
    public long tryWriteLock() {
        long s, next;
        return ((((s = state) & ABITS) == 0L &&
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : 0L); // 获取成功返回state，失败返回0；
    }

    /**
     * 如果锁在给定时间内可用并且当前线程没有被中断，则独占性地获取锁。
     * 超时和中断下的行为与方法Lock.tryLock(long,TimeUnit)指定的行为匹配。
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryWriteLock(long time, TimeUnit unit)
        throws InterruptedException {
        // 将时间转换为纳秒
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) { // 判断线程是否中断
            long next, deadline;
            // 先调用视图获取写锁，成功则返回
            if ((next = tryWriteLock()) != 0L)
                return next;
            if (nanos <= 0L) // 超时则失败
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireWrite(true, deadline)) != INTERRUPTED) // 不超时的话加锁成功
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * 独占获取锁，必要时阻塞，直到可用或当前线程中断。
     * 中断下的行为与为Lock.lockInterruptibly()方法指定的行为相匹配。
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        // 中断则抛出异常
        if (!Thread.interrupted() &&
            (next = acquireWrite(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * 非独占性获取锁，必要时阻塞，直到可用为止。
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long readLock() { // 获取读锁
        long s = state, next;  // bypass acquireRead on common uncontended case
        // 头尾节点相等 & 读锁没有溢出 & 设置读锁成功 = 获取成功 ，否则尝试获取
        return ((whead == wtail && (s & ABITS) < RFULL &&
                 U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
                next : acquireRead(false, 0L));
    }

    /**
     * 如果锁是立即可用的，则非独占性获取锁。
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryReadLock() { // 非阻塞获取
        for (;;) {
            long s, m, next;
            if ((m = (s = state) & ABITS) == WBIT) // 写锁状态，获取失败
                return 0L;
            else if (m < RFULL) { // 没有溢出
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next; // 获取成功
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L) // 读锁溢出
                return next;
        }
    }

    /**
     * 如果锁在给定的时间内可用并且当前线程没有被中断，则非独占性获取锁。
     * 超时和中断下的行为与方法Lock.tryLock(long,TimeUnit)指定的行为匹配。
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryReadLock(long time, TimeUnit unit) // 超时机制，支持中断
        throws InterruptedException {
        long s, m, next, deadline;
        // 转换纳秒
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) { // 非中断状态
            // 不为写锁状态
            if ((m = (s = state) & ABITS) != WBIT) {
                if (m < RFULL) { // 读锁未溢出
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next; // 成功返回
                } // 溢出情况去尝试
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            if (nanos <= 0L) // 小于0，失败
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            // 尝试获取 非中断
            if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException(); // 中断异常
    }

    /**
     * 非独占性获取锁，必要时阻塞，直到可用或当前线程中断。
     * 中断下的行为与为Lock.lockInterruptibly()方法指定的行为相匹配。
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long readLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
            (next = acquireRead(true, 0L)) != INTERRUPTED) // 非中断尝试获取
            return next;
        throw new InterruptedException(); // 中断
    }

    /**
     * 返回一个稍后可以验证的戳记，如果独占锁定则返回零。
     *
     * @return a stamp, or zero if exclusively locked
     */
    public long tryOptimisticRead() {
        long s;
        // 不处于写模式返回戳记，否则失败
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * 如果自发出给定的戳记以来该锁没有被独占，则返回true。
     * 如果戳记为零，则始终返回false。如果戳记表示当前持有的锁，则始终返回true。
     * 使用未从tryOptimisticRead()获取的值或此锁的锁定方法调用此方法没有定义效果或结果。
     *
     * @param stamp a stamp
     * @return {@code true} if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    public boolean validate(long stamp) {
        // 加入内存屏障
        U.loadFence();
        // cas
        return (stamp & SBITS) == (state & SBITS);
    }

    /**
     * 如果锁状态与给定的戳记匹配，则释放互斥锁。
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockWrite(long stamp) {
        WNode h;
        // 如果当前state和传入的戳记不匹配，抛异常
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        // 释放锁需要在高位增加记录
        state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
        // 头节点不为空，并且头结点状态不为0
        if ((h = whead) != null && h.status != 0)
            // 释放下个节点
            release(h);
    }

    /**
     * 如果锁状态与给定的戳记匹配，则释放非排他锁。
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockRead(long stamp) {
        long s, m; WNode h;
        for (;;) { // 自旋
            // 写状态改变 或 无锁 或 写锁 -》 抛异常
            if (((s = state) & SBITS) != (stamp & SBITS) ||
                (stamp & ABITS) == 0L || (m = s & ABITS) == 0L || m == WBIT)
                throw new IllegalMonitorStateException();
            if (m < RFULL) { // 读锁未溢出
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h); // 释放读锁
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L) // 处理读锁溢出
                break;
        }
    }

    /**
     * 如果锁状态与给定的戳记匹配，则释放锁的相应模式。
     *
     * @param stamp a stamp returned by a lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlock(long stamp) { // 读写锁都可以释放
        long a = stamp & ABITS, m, s; WNode h;
        // 判断高位写锁记录
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // 如果当前处于无锁状态或乐观读状态，直接退出
            if ((m = s & ABITS) == 0L)
                break;
            else if (m == WBIT) { // 写模式
                // 传入的stam不是写模式，退出
                if (a != m)
                    break;
                // 释放写锁，添加记录
                state = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return;
            }
            // 无锁或乐观读，退出
            else if (a == 0L || a >= WBIT)
                break;
                // 读锁没有溢出
            else if (m < RFULL) {
                // 释放读锁,成功退出，失败重试
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        // 记录不对，抛异常
        throw new IllegalMonitorStateException();
    }

    /**
     * 如果锁状态与给定的戳记匹配，则执行以下操作之一。
     * 如果戳记表示持有写锁，则返回它。或者，如果有读锁，如果写锁可用，则释放读锁并返回写戳。
     * 或者，如果是乐观读，则仅在立即可用时才返回写戳记。此方法在所有其他情况下都返回零。
     *
     * @param stamp a stamp
     * @return a valid write stamp, or zero on failure
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        // 若锁记录相同
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) { // 无锁或乐观读
                if (a != 0L) // 戳记有写败
                    break;
                if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    return next; // 升级成功
            }
            else if (m == WBIT) { // 若写锁状态
                if (a != m) // 非写锁 失败
                    break;
                return stamp; // 返回写锁状态stamp
            }
            else if (m == RUNIT && a != 0L) { // 若只有一个读锁，则直接升级为写锁
                if (U.compareAndSwapLong(this, STATE, s,
                                         next = s - RUNIT + WBIT))
                    return next;
            }
            else // 否则退出
                break;
        }
        return 0L;
    }

    /**
     * 如果锁状态与给定的戳记匹配，则执行以下操作之一。
     * 如果戳记表示持有写锁，则释放它并获得读锁。
     * 或者，如果是读锁，则返回它。或者，如果一个乐观读操作获得了一个读锁，
     * 并且只有在立即可用的情况下才返回一个读戳。此方法在所有其他情况下都返回零。
     *
     * @param stamp a stamp
     * @return a valid read stamp, or zero on failure
     */
    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        // 锁记录
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // 无锁
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                else if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next; // 直接升级
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L) // 处理溢出
                    return next;
            }
            else if (m == WBIT) { // 有锁
                if (a != m)
                    break;
                state = next = s + (WBIT + RUNIT); // 记录写锁，添加读锁
                if ((h = whead) != null && h.status != 0)
                    release(h); // 释放后续节点
                return next;
            }
            else if (a != 0L && a < WBIT) // 若是读锁，直接返回
                return stamp;
            else
                break;
        }
        return 0L;
    }

    /**
     * 如果锁状态与给定的戳记匹配，则如果戳记表示持有锁，则释放它并返回一个观察戳记。
     * 或者，如果是乐观读取，则在验证后返回。该方法在所有其他情况下都返回0，
     * 因此可以作为“tryUnlock”的一种形式使用。
     *
     * @param stamp a stamp
     * @return a valid optimistic read stamp, or zero on failure
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        U.loadFence();
        for (;;) {
            // 锁记录相等
            if (((s = state) & SBITS) != (stamp & SBITS))
                break;
            if ((m = s & ABITS) == 0L) { // 无锁
                if (a != 0L)
                    break;
                return s; // 返回
            }
            else if (m == WBIT) { // 写锁
                if (a != m)
                    break;
                state = next = (s += WBIT) == 0L ? ORIGIN : s; // 记录写记录，无锁的话返回ORIGIN，有锁返回之前值
                // 头节点不为空，并且头结点的状态不为0
                if ((h = whead) != null && h.status != 0)
                    release(h); // 释放
                return next;
            }
            else if (a == 0L || a >= WBIT) // 无锁退出
                break;
            else if (m < RFULL) { // 读锁未溢出
                if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT)) { // 读锁-1
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h); // 若读锁全部解锁，则释放下个节点。
                    return next & SBITS;
                }
            }
            else if ((next = tryDecReaderOverflow(s)) != 0L) // 处理溢出
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * 如果持有写锁，则释放它，而不需要戳记值。此方法对于错误后的恢复可能很有用。
     *
     * @return {@code true} if the lock was held, else false
     */
    public boolean tryUnlockWrite() { // 无需传入stamp释放写锁
        long s; WNode h;
        // 如果当前不是写状态，则释放失败
        if (((s = state) & WBIT) != 0L) {
            // 释放，高位添加记录
            state = (s += WBIT) == 0L ? ORIGIN : s;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return true;
        }
        return false;
    }

    /**
     * 如果持有读锁，则释放该读锁的一次持有，而不需要戳记值。此方法对于错误后的恢复可能很有用。
     *
     * @return {@code true} if the read lock was held, else false
     */
    public boolean tryUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    // status monitoring methods

    /**
     * 返回给定状态的组合状态保持数和溢出读计数。
     */
    private int getReadLockCount(long s) {
        long readers;
        if ((readers = s & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }

    /**
     * 如果锁当前被独占，则返回true。
     *
     * @return {@code true} if the lock is currently held exclusively
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * 如果锁当前非独占地持有，则返回true。
     *
     * @return {@code true} if the lock is currently held non-exclusively
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    /**
     * 查询为该锁持有的读锁的数量。该方法用于监控系统状态，不用于同步控制。
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return getReadLockCount(state);
    }

    /**
     * 返回标识此锁的字符串及其锁状态。
     * 括号中的状态包括字符串“解锁”或字符串“写锁”或字符串“读锁”:后跟当前持有的读锁数量。
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        long s = state;
        return super.toString() +
            ((s & ABITS) == 0L ? "[Unlocked]" :
             (s & WBIT) != 0L ? "[Write-locked]" :
             "[Read-locks:" + getReadLockCount(s) + "]");
    }

    // views

    /**
     * 返回此StampedLock的普通{@link Lock}视图，其中{@link Lock# Lock}方法映射到
     * {@link #readLock}，其他方法也类似。
     * 返回的锁不支持{@link条件};方法{@link Lock#newCondition()}抛出
     * {@code UnsupportedOperationException}。
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    /**
     * 返回此StampedLock的普通{@link Lock}视图，其中{@link Lock# Lock}方法映射到
     * {@link #writeLock}，其他方法也类似。
     * 返回的锁不支持{@link条件};方法{@link Lock#newCondition()}抛出
     * {@code UnsupportedOperationException}。
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new WriteLockView()));
    }

    /**
     * 返回此StampedLock的{@link ReadWriteLock}视图，其中
     * {@link ReadWriteLock#readLock()}方法映射到{@link #asReadLock()}，
     * {@link ReadWriteLock#writeLock()}映射到{@link #asWriteLock()}。
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new ReadWriteLockView()));
    }

    // view classes

    // 读锁视图
    final class ReadLockView implements Lock {
        public void lock() { readLock(); }
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        public boolean tryLock() { return tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockRead(); }
        public Condition newCondition() { // 不支持
            throw new UnsupportedOperationException();
        }
    }
    // 写锁视图
    final class WriteLockView implements Lock {
        public void lock() { writeLock(); }
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        public boolean tryLock() { return tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockWrite(); }
        public Condition newCondition() { // 不支持
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock { // 实现读写锁
        public Lock readLock() { return asReadLock(); }
        public Lock writeLock() { return asWriteLock(); }
    }

    // Unlock methods without stamp argument checks for view classes.
    // Needed because view-class lock methods throw away stamps.

    final void unstampedUnlockWrite() {
        WNode h; long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (s += WBIT) == 0L ? ORIGIN : s;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    final void unstampedUnlockRead() {
        for (;;) {
            long s, m; WNode h;
            if ((m = (s = state) & ABITS) == 0L || m >= WBIT)
                throw new IllegalMonitorStateException();
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        state = ORIGIN; // reset to unlocked state
    }

    // internals

    /**
     * 尝试通过首先将状态访问位值设置为RBITS来增加readerOverflow，表示持有自旋锁，然后更新，然后释放。
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        // 如果读锁溢出
        if ((s & ABITS) == RFULL) {
            // 读锁设满127
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                // 溢出+1
                ++readerOverflow;
                // state为原值
                state = s;
                return s; // 返回原值
            }
        } // 若并发情况下不满了，则随机让步
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * 尝试递减readerOverflow。
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        // 如果当前读模式已满，
        if ((s & ABITS) == RFULL) {
            // 先将state设满127
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                int r; long next;
                // 如果当前当前记录溢出数大于0
                if ((r = readerOverflow) > 0) {
                    // 溢出数减1
                    readerOverflow = r - 1;
                    // 原来state
                    next = s;
                }
                else
                    // 否则读锁记录-1
                    next = s - RUNIT;
                 state = next;
                 return next;
            }
        } // 如果并发释放读锁，随机让步
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * 唤醒h的继承者(通常是whead)。
     * 这通常是h。但是，如果下一个指针滞后，则可能需要从wtail遍历。
     * 当一个或多个线程被取消时，这可能无法唤醒获取线程，但是取消方法本身提供了额外的保护措施来确保活性。
     */
    private void release(WNode h) {
        if (h != null) { // 头节点不为空
            WNode q; Thread w;
            // 如果头结点的状态为等待状态，将其状态设置为0
            U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
            // 从尾结点开始，到头节点为止，寻找状态为等待状态或者为0的有效节点
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            // 如果寻找的有效节点不为空，并且其对应的线程也不为空，唤醒其线程
            if (q != null && (w = q.thread) != null)
                U.unpark(w);
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible t如果检查中断 and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return 下一个状态, or 中断
     */
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) { // 入队时自旋
            long m, s, ns;
            if ((m = (s = state) & ABITS) == 0L) { // 若当前恰好无锁，则设置写锁后返回
                if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                    return ns;
            }
            else if (spins < 0) // 如果当前锁被占用了
                // 队列里没东西的话，获取自旋次数
                spins = (m == WBIT && wtail == whead) ? SPINS : 0; // 写锁，当前无队列，自旋次数255，否则0
            else if (spins > 0) { // 重试
                if (LockSupport.nextSecondarySeed() >= 0)// 不知道有啥子用，永远返回正数
                    --spins; // 重试次数减一
            }
            // 重试结束，开始插入队列
            else if ((p = wtail) == null) { // 初始化队列
                WNode hd = new WNode(WMODE, null); // 无前任的节点
                if (U.compareAndSwapObject(this, WHEAD, null, hd)) // 替换队列头结点
                    wtail = hd; // 尾节点=头结点
            }
            else if (node == null)
                node = new WNode(WMODE, p); // 头节点后的新节点
            else if (node.prev != p) // 尾节点改变，重新设置
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) { // 设置尾节点
                p.next = node;
                break; // 进入队列成功，跳出循环
            }
        }
// 循环阻塞当前线程
        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) { // 若头结点等于尾节点
                if (spins < 0)
                    spins = HEAD_SPINS; // 设置自旋的值
                else if (spins < MAX_HEAD_SPINS) // 如果自旋小于自旋最大值，则扩大两倍
                    spins <<= 1;
                for (int k = spins;;) { // 从头部自旋
                    long s, ns;
                    if (((s = state) & ABITS) == 0L) { // 若无锁。则获取锁
                        if (U.compareAndSwapLong(this, STATE, s,
                                                 ns = s + WBIT)) {
                            whead = node; // 头节点设置为当前节点
                            node.prev = null; // 帮助gc
                            return ns; // 返回当前state
                        }
                    }
                    else if (LockSupport.nextSecondarySeed() >= 0 &&
                             --k <= 0)
                        break; // 自旋超时，跳出循环
                }
            }
            else if (h != null) { // 若头节点不为空，释放等待者
                WNode c; Thread w;
                // 如果头结点的cowait队列不为空，唤醒cowait队列
                while ((c = h.cowait) != null) { // 循环唤醒等待线程
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w); // 释放读线程
                }
            }
            if (whead == h) { // 如果头结点不变
                // 如果当前节点的前节点不是尾结点
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                // 如果当前节点的前节点状态为0，将其前驱节点设置为等待状态
                else if ((ps = p.status) == 0)
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                    // 如果当前节点前节点状态为取消
                else if (ps == CANCELLED) {
                    // 重新设置当前节点的前驱节点
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time; // 如果传入时间为0，阻塞直到唤醒
                    if (deadline == 0L)
                        time = 0L;
                        // 如果时间已超时，取消当前节点
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    // 获取当前线程
                    Thread wt = Thread.currentThread();
                    // 设置线程Thread的parkblocker属性，表示当前线程被谁阻塞，用于监控线程使用
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 && (p != h || (state & ABITS) != 0L) &&
                        whead == h && node.prev == p)
                        // 阻塞当前线程
                        U.park(false, time);  // 类似 LockSupport.park
                    // 当前节点线程为空
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    // 如果传入的参数interruptible为true，并且当前线程中断，取消当前节点
                    if (interruptible && Thread.interrupted())
                        // 取消节点
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true 如果检查中断为true
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) {
            WNode h;
            // 如果头结点等于尾结点，则自旋
            if ((h = whead) == (p = wtail)) {
                for (long m, s, ns;;) {
                    // 如果读锁未溢出则设置读+1成功 ，则返回获取成功
                    if ((m = (s = state) & ABITS) < RFULL ?
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))
                        return ns;
                    else if (m >= WBIT) { // 自旋-1
                        if (spins > 0) {
                            if (LockSupport.nextSecondarySeed() >= 0)
                                --spins;
                        }
                        else {
                            if (spins == 0) { // 自旋减到0
                                WNode nh = whead, np = wtail;
                                // 如果头尾节点未改变，或头尾节点不相等，退出自旋
                                if ((nh == h && np == p) || (h = nh) != (p = np))
                                    break;
                            }
                            spins = SPINS; // 自旋初始值
                        }
                    }
                }
            }
            if (p == null) { // 如果队列为null，初始化队列
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd)) // 设置头节点
                    wtail = hd;
            }
            else if (node == null) // 若当前节点为空，构造当前节点
                node = new WNode(RMODE, p);
                // 若头尾节点相等，当前锁状态不为读锁状态
            else if (h == p || p.mode != RMODE) {
                if (node.prev != p) // 设置当前节点
                    node.prev = p;
                else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    break; // 入队成功，跳出
                }
            } // 将当前节点加入尾结点的cowait队列中，如果失败，则将当前节点的cowait设为null
            else if (!U.compareAndSwapObject(p, WCOWAIT,
                                             node.cowait = p.cowait, node))
                node.cowait = null;
            else { // 如果当前队列不为空，当前节点不为空，并且当前为读锁状态，并且加入尾队列失败
                for (;;) {
                    WNode pp, c; Thread w;
                    // 如果头节点的cowait队列不为空，并且其线程也不为null，将其cowait唤醒
                    if ((h = whead) != null && (c = h.cowait) != null &&
                        U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null) // help release
                        U.unpark(w); // 唤醒cow头ait队列中的节点线程
                    // 如果当前头节点为尾结点的前驱节点，或者头尾节点相等，或者尾结点的前节点为空
                    if (h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        do { // 判断当前是否属于读状态，有没有溢出，读锁加1·
                            if ((m = (s = state) & ABITS) < RFULL ?
                                U.compareAndSwapLong(this, STATE, s,
                                                     ns = s + RUNIT) :
                                (m < WBIT &&
                                 (ns = tryIncReaderOverflow(s)) != 0L)) // 否则进行溢出操作
                                return ns;
                        } while (m < WBIT); // 当前state不是写模式，才能进行重试
                    }
                    if (whead == h && p.prev == pp) { // 如果头节点没有改变，并且尾结点的前驱节点不变
                        long time;
                        // 如果尾结点的前驱节点为空，或头尾节点相等，或尾结点的状态为取消
                        if (pp == null || h == p || p.status > 0) {
                            // 将当前节点设置为空，退出循环
                            node = null; // throw away
                            break;
                        }
                        // 如果传入的超时时间已经过期，将当前节点取消
                        if (deadline == 0L)
                            time = 0L;
                        else if ((time = deadline - System.nanoTime()) <= 0L)
                            return cancelWaiter(node, p, false);
                        // 获取当前线程，设置阻塞对象
                        Thread wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        if ((h != pp || (state & ABITS) == WBIT) &&
                            whead == h && p.prev == pp)
                            // 阻塞当前节点
                            U.park(false, time);
                        node.thread = null;
                        // 阻塞结束，阻塞类设为null
                        U.putObject(wt, PARKBLOCKER, null);
                        // 中断的话，取消等待
                        if (interruptible && Thread.interrupted())
                            return cancelWaiter(node, p, true);
                    }
                }
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            // 如果头尾节点相等
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS; // 自旋初始值
                else if (spins < MAX_HEAD_SPINS) // spin小于最大自旋，扩大二倍
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    long m, s, ns;
                    if ((m = (s = state) & ABITS) < RFULL ? // 若读锁小于溢出值
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) : // 获取成功
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) { // 处理溢出值
                        WNode c; Thread w;
                        whead = node;
                        node.prev = null;
                        while ((c = node.cowait) != null) {
                            if (U.compareAndSwapObject(node, WCOWAIT,
                                                       c, c.cowait) &&
                                (w = c.thread) != null)
                                // 释放当前节点
                                U.unpark(w);
                        }
                        return ns; // 返回成功
                    }
                    else if (m >= WBIT && // 若当前为写状态，重试
                             LockSupport.nextSecondarySeed() >= 0 && --k <= 0)
                        break;
                }
            }
            else if (h != null) { // 如果头结点不为空
                WNode c; Thread w;
                while ((c = h.cowait) != null) { // 头结点的队列不为空，循环唤醒cowait队列中，线程不为空的线程
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w); // 唤醒
                }
            }
            if (whead == h) { // 若头结点没改变
                if ((np = node.prev) != p) { // 如果当前节点的前节点不等于尾结点
                    // 前节点不为空
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0) // 如果当前节点的前驱节点状态为0，
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING); // 状态修改为等待状态
                else if (ps == CANCELLED) { // 若是取消状态
                    if ((pp = p.prev) != null) { // 跳过
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time;
                    if (deadline == 0L) // 超时取消
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 &&
                        (p != h || (state & ABITS) == WBIT) &&
                        whead == h && node.prev == p)
                        U.park(false, time); // 阻塞
                    node.thread = null; // 唤醒后设置为空
                    U.putObject(wt, PARKBLOCKER, null); // 唤醒后设置为空
                    if (interruptible && Thread.interrupted()) // 中断的话
                        return cancelWaiter(node, node, true); // 取消等待节点
                }
            }
        }
    }

    /**
     * 如果节点非空，则强制取消状态并在可能的情况下将其从队列中解拼接出来，
     * 并唤醒任何cowaiter(节点或组的cowaiter，视情况而定)，在任何情况下，
     * 如果锁是空闲的，则帮助释放当前的first waiter。(使用null参数调用是一种条件释放形式，
     * 目前还不需要，但是在将来可能的取消策略下可能需要)。
     * 这是AbstractQueuedSynchronizer中取消方法的一种变体(参见AQS内部文档中的详细说明)。
     *
     * @param node if nonnull, the waiter
     * @param group either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     * @return INTERRUPTED if interrupted or Thread.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        // node和group为同一节点，要取消的节点，都不为null时。
        if (node != null && group != null) {
            Thread w;
            node.status = CANCELLED; // 将当前节点的状态设置为取消状态
            // unsplice cancelled nodes from group
            // 如果当前要取消的节点的cowait队列不为空，将其cowait队列中取消的节点去除
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group; // restart
                }
                else
                    p = q;
            }
            // group和node为同一节点
            if (group == node) {
                // 唤醒状态没有取消的cowait队列中的节点
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    if ((w = r.thread) != null)
                        U.unpark(w);       // wake up uncancelled co-waiters
                }
                // 将其当前取消节点的前驱节点的下一个节点设置为当前取消节点的next节点
                for (WNode pred = node.prev; pred != null; ) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while ((succ = node.next) == null ||
                           succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // don't link if succ cancelled
                        if (succ == q ||   // ensure accurate successor
                            U.compareAndSwapObject(node, WNEXT,
                                                   succ, succ = q)) {
                            if (succ == null && node == wtail)
                                U.compareAndSwapObject(this, WTAIL, node, pred);
                            break;
                        }
                    }
                    if (pred.next == node) // unsplice pred link
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    if (succ != null && (w = succ.thread) != null) {
                        succ.thread = null;
                        U.unpark(w);       // wake up succ to observe new pred
                    }
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while ((h = whead) != null) {
            long s; WNode q; // similar to release() but check eligibility
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                if (q != null && h.status == 0 &&
                    ((s = state) & ABITS) != WBIT && // waiter is eligible
                    (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATE;
    private static final long WHEAD;
    private static final long WTAIL;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WCOWAIT;
    private static final long PARKBLOCKER;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = StampedLock.class;
            Class<?> wk = WNode.class;
            STATE = U.objectFieldOffset
                (k.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset
                (k.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset
                (k.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset
                (wk.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset
                (wk.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset
                (wk.getDeclaredField("cowait"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
